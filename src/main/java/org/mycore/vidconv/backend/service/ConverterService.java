/*
 * $Id$ 
 * $Revision$ $Date$
 *
 * This file is part of ***  M y C o R e  ***
 * See http://www.mycore.de/ for details.
 *
 * This program is free software; you can use it, redistribute it
 * and / or modify it under the terms of the GNU General Public License
 * (GPL) as published by the Free Software Foundation; either version 3
 * of the License or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program, in a file called gpl.txt or license.txt.
 * If not, write to the Free Software Foundation Inc.,
 * 59 Temple Place - Suite 330, Boston, MA  02111-1307 USA
 */
package org.mycore.vidconv.backend.service;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.xml.bind.JAXBException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.vidconv.backend.encoder.FFMpegImpl;
import org.mycore.vidconv.common.config.Settings;
import org.mycore.vidconv.common.event.Event;
import org.mycore.vidconv.common.event.EventManager;
import org.mycore.vidconv.common.event.Listener;
import org.mycore.vidconv.common.util.Executable;
import org.mycore.vidconv.common.util.JsonUtils;
import org.mycore.vidconv.common.util.StreamConsumer;
import org.mycore.vidconv.concurrent.PriorityExecutor;
import org.mycore.vidconv.frontend.entity.ConverterWrapper;
import org.mycore.vidconv.frontend.entity.ConvertersWrapper;
import org.mycore.vidconv.frontend.entity.HWAccelDeviceSpec;
import org.mycore.vidconv.frontend.entity.HWAccelWrapper;
import org.mycore.vidconv.frontend.entity.SettingsWrapper;
import org.mycore.vidconv.frontend.entity.SettingsWrapper.Output;
import org.mycore.vidconv.frontend.util.ZipStreamingOutput;
import org.mycore.vidconv.frontend.widget.Widget;

/**
 * @author Ren\u00E9 Adler (eagle)
 *
 */
public class ConverterService extends Widget implements Listener {

    public static final String WIDGET_NAME = "converter";

    public static final String EVENT_CONVERT_START = "converter_start";

    public static final String EVENT_CONVERT_PROGRESS = "converter_progress";

    public static final String EVENT_CONVERT_DONE = "converter_done";

    public static final int PRIORITY_HIGHT = 100;

    public static final int PRIORITY_NORMAL = 0;

    public static final int PRIORITY_LOW = -1;

    private static final Logger LOGGER = LogManager.getLogger(ConverterService.class);

    private static final EventManager EVENT_MANAGER = EventManager.instance();

    private static final Settings SETTINGS = Settings.instance();

    private static final List<HWAccelWrapper<? extends HWAccelDeviceSpec>> hwAccels = Collections
            .synchronizedList(new ArrayList<>());

    private final Map<String, ConverterJob> converters = new ConcurrentHashMap<>();

    private final ExecutorService converterThreadPool;

    private String outputDir;

    private String tempDir;

    static {
        Optional.ofNullable(FFMpegImpl.detectHWAccels())
                .ifPresent(dhw -> dhw.getHWAccels().stream().filter(
                        hw -> SETTINGS.getSettings() != null ? SETTINGS.getSettings().getHwaccels().contains(hw) : true)
                        .forEach(hw -> hwAccels.add(hw)));
    }

    public ConverterService(final String outputDir, final String tempDir, int converterThreads) throws IOException {
        super(WIDGET_NAME);

        this.outputDir = outputDir;
        this.setTempDir(tempDir);

        Path outputPath = Paths.get(outputDir);
        if (!Files.exists(outputPath)) {
            Files.createDirectories(outputPath);
        }

        EVENT_MANAGER.addListener(this);

        int hwAccelConverterThreads = hwAccels.stream()
                .mapToInt(hw -> hw.getDeviceSpec().numConcurrentProcesses())
                .sum();

        converterThreadPool = PriorityExecutor.newFixedThreadPool(
                hwAccelConverterThreads > 0 ? Integer.max(hwAccelConverterThreads, converterThreads)
                        : converterThreads);

        addIncompleteJobs();
    }

    /**
     * @return the outputDir
     */
    public String getOutputDir() {
        return outputDir;
    }

    /**
     * @param outputDir the outputDir to set
     */
    public void setOutputDir(String outputDir) {
        this.outputDir = outputDir;
    }

    /**
     * @return the tempDir
     */
    public String getTempDir() {
        return tempDir;
    }

    /**
     * @param tempDir the tempDir to set
     */
    public void setTempDir(String tempDir) {
        this.tempDir = tempDir;
    }

    /* (non-Javadoc)
     * @see org.mycore.vidconv.frontend.widget.Widget#status()
     */
    @Override
    public ConvertersWrapper status() {
        return new ConvertersWrapper(this.converters);
    }

    /* (non-Javadoc)
     * @see org.mycore.vidconv.frontend.widget.Widget#status(java.util.List)
     */
    @Override
    public Object status(List<String> params) {
        if (params.size() == 1) {
            final String converterId = params.get(0);
            final ConverterJob converter = converters.get(converterId);

            if (converter != null) {
                return new ConverterWrapper(converterId, converter);
            }
        } else {
            final Integer page = new Integer(params.get(0));
            final Integer limit = new Integer(params.get(1));

            int start = (page - 1) * limit;

            final List<ConverterWrapper> filteredList = converters.entrySet().stream()
                    .map(e -> new ConverterWrapper(e.getKey(), e.getValue())).filter(cw -> {
                        boolean ret = true;
                        final String filter = params.size() == 3 ? params.get(2).toLowerCase(Locale.ROOT) : null;
                        if (filter != null) {
                            ret = Pattern.compile(",").splitAsStream(filter).map(f -> {
                                boolean fb = true;
                                if (f.endsWith("isdone"))
                                    fb = cw.isDone();
                                if (f.endsWith("isrunning"))
                                    fb = cw.isRunning();

                                if (f.startsWith("!"))
                                    fb = !fb;
                                return fb;
                            }).filter(b -> !b).count() != 0;
                        }

                        return ret;
                    }).collect(Collectors.toCollection(ArrayList<ConverterWrapper>::new));

            final ConvertersWrapper wrapper = new ConvertersWrapper(
                    (ArrayList<ConverterWrapper>) filteredList.stream().sorted().skip(start).limit(limit)
                            .collect(Collectors.toCollection(ArrayList<ConverterWrapper>::new)));

            wrapper.setTotal((int) filteredList.size());
            wrapper.setStart(start);
            wrapper.setLimit(limit);

            return wrapper;
        }

        return null;
    }

    /* (non-Javadoc)
     * @see org.mycore.vidconv.frontend.widget.Widget#download(java.util.List)
     */
    @Override
    public Path download(List<String> params) throws Exception {
        if (!params.isEmpty()) {
            final String converterId = params.get(0);
            final ConverterJob converter = converters.get(converterId);

            if (converter != null && converter.isDone()) {
                final String fileName = URLDecoder.decode(params.get(1), StandardCharsets.UTF_8.toString());
                return converter.outputs.stream()
                        .filter(o -> o.getOutputPath().getFileName().toString().equals(fileName))
                        .findFirst()
                        .map(o -> o.getOutputPath())
                        .orElseThrow(() -> new FileNotFoundException("File \"" + fileName + "\" not found."));
            }
        }

        return null;
    }

    /* (non-Javadoc)
     * @see org.mycore.vidconv.frontend.widget.Widget#compress(java.util.List)
     */
    @Override
    public ZipStreamingOutput compress(List<String> params) throws Exception {
        if (!params.isEmpty()) {
            final String converterId = params.get(0);
            final ConverterJob converter = converters.get(converterId);

            if (converter != null && converter.isDone()) {
                if (params.size() == 1) {
                    return new ZipStreamingOutput(converter.outputPath, 0);
                } else {
                    final String fileName = URLDecoder.decode(params.get(1), StandardCharsets.UTF_8.toString());
                    return new ZipStreamingOutput(converter.outputs.stream()
                            .filter(o -> o.getOutputPath().getFileName().toString().equals(fileName))
                            .findFirst()
                            .map(o -> o.getOutputPath())
                            .orElseThrow(() -> new FileNotFoundException("File \"" + fileName + "\" not found.")));
                }
            }
        }

        return null;
    }

    /* (non-Javadoc)
     * @see org.mycore.vidconv.common.event.Listener#handleEvent(org.mycore.vidconv.common.event.Event)
     */
    @Override
    public void handleEvent(Event<?> event) throws Exception {
        if (DirectoryWatchService.EVENT_ENTRY_CREATE.equals(event.getType())
                && event.getSource().equals(DirectoryWatchService.class)) {
            Path inputPath = (Path) event.getObject();
            addJob(inputPath, null, PRIORITY_NORMAL, null);
        }

        if ((ConverterJob.START.equals(event.getType()) || ConverterJob.PROGRESS.equals(event.getType())
                || ConverterJob.DONE.equals(event.getType()))
                && event.getSource().equals(ConverterJob.class)) {
            String type = ConverterJob.START.equals(event.getType()) ? EVENT_CONVERT_START
                    : ConverterJob.PROGRESS.equals(event.getType()) ? EVENT_CONVERT_PROGRESS : EVENT_CONVERT_DONE;
            EVENT_MANAGER
                    .fireEvent(
                            new Event<ConverterJob>(type, converters.get((String) event.getObject()), this.getClass())
                                    .setInternal(false));
        }
    }

    public String addJob(final Path inputPath, final String jobId, final int priority, final String completeCallBack)
            throws InterruptedException, JAXBException, ExecutionException, IOException {
        final SettingsWrapper settings = SETTINGS.getSettings();

        if (settings != null && !settings.getOutput().isEmpty() && !Files.isDirectory(inputPath)) {
            if (FFMpegImpl.isEncodingSupported(inputPath)) {
                final String id = Optional.ofNullable(jobId).orElseGet(() -> Long.toHexString(new Random().nextLong()));
                final String fileName = inputPath.getFileName().toString();
                final Path outputPath = Paths.get(outputDir, id);
                final int prio = Math.min(PRIORITY_HIGHT, Math.max(PRIORITY_LOW, priority));

                final Function<Output, Output> outMapper = (Output o) -> {
                    final Output out = o.getCopy();
                    final String appendix = Optional.ofNullable(o.getFilenameAppendix()).orElse(id);
                    out.setInputPath(inputPath);
                    out.setOutputPath(outputPath.resolve(FFMpegImpl.filename(o.getFormat(), fileName,
                            appendix)));

                    return out;
                };

                final List<Output> outputs = settings.getOutput().stream()
                        .sorted(Settings.sortOutputs)
                        .filter(output -> {
                            try {
                                return output.getVideo().getUpscale()
                                        || output.getVideo().getScale() != null
                                                && !FFMpegImpl.isUpscaling(inputPath, output.getVideo().getScale());
                            } catch (Exception e) {
                                throw new RuntimeException(e);
                            }
                        }).map(outMapper).collect(Collectors.toList());

                if (outputs.isEmpty() && settings.getOutput().size() == 1) {
                    Optional.ofNullable(settings.getOutput().get(0)).map(outMapper).ifPresent(outputs::add);
                }

                final ConverterJob converter = new ConverterJob(id, prio, outputs, inputPath, outputPath,
                        completeCallBack);

                if (converters.containsKey(id)
                        && Optional.ofNullable(converters.get(id)).filter(cj -> cj.inputPath().equals(inputPath))
                                .map(ConverterJob::isRunning).orElse(false)) {
                    LOGGER.warn("File \"" + inputPath.toFile().getAbsolutePath() + "\" is already in queue.");
                    return null;
                }

                converters.put(id, converter);
                converterThreadPool.submit(converter, prio);

                return id;
            } else {
                LOGGER.warn("encoding of file \"" + inputPath.toFile().getAbsolutePath() + "\" isn't supported.");
            }
        }

        return null;
    }

    public boolean removeJob(String jobId) throws IOException {
        ConverterJob job = converters.get(jobId);

        if (job != null) {
            if (job.isDone()) {
                deleteRecursive(job.inputPath());
                deleteRecursive(job.outputPath());
                converters.remove(jobId);
                return true;
            } else {
                LOGGER.warn("Couldn't remove job with id {}, because is running!", jobId);
            }
        } else {
            LOGGER.error("Couldn't find a job with id {}!", jobId);
        }

        return false;
    }

    private void addIncompleteJobs() {
        try {
            LOGGER.info("search for incomplete/failed jobs...");
            try (Stream<Path> stream = Files.walk(Paths.get(outputDir))) {
                stream.filter(f -> f.getFileName().equals(Paths.get(".convert"))).forEach(file -> {
                    try {
                        ConverterWrapper cw = JsonUtils.loadJSON(file.toFile(), ConverterWrapper.class);
                        if (!cw.isDone() || cw.getExitValue() != 0) {
                            LOGGER.info("...restart \"{}\" with id \"{}\".", cw.getFileName(), cw.getId());
                            addJob(Paths.get(cw.getInputPath(), cw.getFileName()), cw.getId(), cw.getPriority(),
                                    cw.getCompleteCallback());
                        }
                    } catch (JAXBException | InterruptedException | ExecutionException | IOException e) {
                        LOGGER.error("Couldn't add incomplete jobs.", e);
                    }
                });
            }
            LOGGER.info("...done.");
        } catch (IOException e) {
            LOGGER.error("Couldn't read incomplete jobs.", e);
        }
    }

    private static void deleteRecursive(Path path) throws IOException {
        Files.walk(path, FileVisitOption.FOLLOW_LINKS)
                .sorted(Comparator.reverseOrder())
                .map(Path::toFile)
                .forEach(File::delete);
    }

    public static class ConverterJob implements Runnable {

        public static final String START = "start";

        public static final String PROGRESS = "progress";

        public static final String DONE = "done";

        private final String id;

        private int priority;

        private final Path inputPath;

        private final Path outputPath;

        private final String completeCallBack;

        private final List<Output> outputs;

        private String command;

        private Optional<HWAccelWrapper<? extends HWAccelDeviceSpec>> hwAccel;

        private boolean running;

        private boolean done;

        private Instant addTime;

        private Instant startTime;

        private Instant endTime;

        private Integer exitValue;

        private StreamConsumer outputConsumer;

        private StreamConsumer errorConsumer;

        private Timer timer;

        public ConverterJob(final String id, int priority, final List<Output> outputs, final Path inputPath,
                final Path outputPath,
                final String completeCallBack) throws InterruptedException, IOException, JAXBException {
            this.id = id;
            this.outputs = outputs;
            this.inputPath = inputPath;
            this.outputPath = outputPath;
            this.completeCallBack = completeCallBack;
            this.hwAccel = Optional.empty();
            this.addTime = Instant.now();
            this.done = false;
            this.running = false;
            this.priority = priority;

            if (!Files.exists(outputPath))
                Files.createDirectories(outputPath);

            save();
        }

        @Override
        public void run() {
            try {
                LOGGER.info("Start converting of " + inputPath.toString() + " to " + outputPath.toString() + "...");

                done = false;
                running = true;
                startTime = Instant.now();

                hwAccel = hwAccels.stream()
                        .filter(hw -> FFMpegImpl.canHWAccelerate(outputs, hw)).sorted()
                        .findFirst();

                command = FFMpegImpl.command(id, inputPath, outputs, hwAccel);
                final Executable exec = new Executable(command);

                EVENT_MANAGER.fireEvent(new Event<String>(START, id, this.getClass()));

                timer = new Timer();
                timer.scheduleAtFixedRate(new ConverterJobProgress(this), 0, 1000);

                final Process p = exec.run();

                outputConsumer = exec.outputConsumer();
                errorConsumer = exec.errorConsumer();

                p.waitFor();

                timer.cancel();

                exitValue = p.exitValue();
                running = false;
                done = true;
                endTime = Instant.now();

                hwAccel.ifPresent(hw -> hw.getDeviceSpec().unregisterProcessId(id));

                save();

                LOGGER.info("Converting of " + inputPath.toString() + " done.");

                EVENT_MANAGER.fireEvent(new Event<String>(DONE, id, this.getClass()));
            } catch (InterruptedException | JAXBException | ExecutionException | IOException e) {
                LOGGER.error(e.getMessage(), e);
            }
        }

        public String id() {
            return id;
        }

        public int priority() {
            return priority;
        }

        public String completeCallBack() {
            return completeCallBack;
        }

        public String command() {
            return command;
        }

        public List<Output> outputs() {
            return outputs;
        }

        public Path inputPath() {
            return inputPath;
        }

        public Path outputPath() {
            return outputPath;
        }

        public HWAccelWrapper<? extends HWAccelDeviceSpec> hwAccel() {
            return hwAccel.orElse(null);
        }

        public boolean isRunning() {
            return running;
        }

        public boolean isDone() {
            return done;
        }

        public Instant addTime() {
            return addTime;
        }

        public Instant startTime() {
            return startTime;
        }

        public Instant endTime() {
            return endTime;
        }

        public Integer exitValue() {
            return exitValue;
        }

        public String outputStream() {
            return outputConsumer != null ? outputConsumer.getStreamOutput() : null;
        }

        public String errorStream() {
            return errorConsumer != null ? errorConsumer.getStreamOutput() : null;
        }

        private void save() throws JAXBException, IOException {
            JsonUtils.saveJSON(outputPath.resolve(".convert").toFile(), new ConverterWrapper(id, this));
        }
    }

    private static class ConverterJobProgress extends TimerTask {

        private final ConverterJob job;

        public ConverterJobProgress(ConverterJob job) {
            this.job = job;
        }

        /* (non-Javadoc)
         * @see java.util.TimerTask#run()
         */
        @Override
        public void run() {
            EVENT_MANAGER
                    .fireEvent(new Event<String>(ConverterJob.PROGRESS, job.id(), job.getClass()));
        }

    }
}
