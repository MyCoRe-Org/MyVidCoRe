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

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.MessageFormat;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.persistence.jaxb.MarshallerProperties;
import org.mycore.vidconv.backend.encoder.FFMpegImpl;
import org.mycore.vidconv.common.config.Settings;
import org.mycore.vidconv.common.event.Event;
import org.mycore.vidconv.common.event.EventManager;
import org.mycore.vidconv.common.event.Listener;
import org.mycore.vidconv.common.util.Executable;
import org.mycore.vidconv.common.util.StreamConsumer;
import org.mycore.vidconv.frontend.entity.ConverterWrapper;
import org.mycore.vidconv.frontend.entity.ConvertersWrapper;
import org.mycore.vidconv.frontend.entity.SettingsWrapper;
import org.mycore.vidconv.frontend.entity.SettingsWrapper.Output;
import org.mycore.vidconv.frontend.widget.Widget;

/**
 * @author Ren\u00E9 Adler (eagle)
 *
 */
public class ConverterService extends Widget implements Listener {

    public static final String WIDGET_NAME = "converter";

    public static final String EVENT_CONVERT_DONE = "converter_done";

    private static final Logger LOGGER = LogManager.getLogger(ConverterService.class);

    private static final EventManager EVENT_MANAGER = EventManager.instance();

    private static final Settings SETTINGS = Settings.instance();

    private final Map<String, ConverterJob> converters = new ConcurrentHashMap<>();

    private final ExecutorService converterThreadPool;

    private String outputDir;

    private Comparator<Output> sortOutputs = (o1, o2) -> {
        if (o1.getFormat().equals(o2.getFormat())) {
            final Integer[] sc1 = Arrays.stream(o1.getVideo().getScale().split(":")).map(Integer::new)
                .toArray(Integer[]::new);
            final Integer[] sc2 = Arrays.stream(o2.getVideo().getScale().split(":")).map(Integer::new)
                .toArray(Integer[]::new);
            return sc1[0] < 0 && sc1[0] < 0 ? Integer.compare(sc2[1], sc1[1]) : Integer.compare(sc2[1], sc1[1]);
        }

        return o1.getFormat().compareTo(o2.getFormat());
    };

    public ConverterService(final String outputDir, int converterThreads) {
        super(WIDGET_NAME);

        this.outputDir = outputDir;
        EVENT_MANAGER.addListener(this);
        converterThreadPool = Executors.newFixedThreadPool(converterThreads);
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
            final String fileName = URLDecoder.decode(params.get(1), StandardCharsets.UTF_8.toString());
            final ConverterJob converter = converters.get(converterId);

            if (converter != null) {
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
     * @see org.mycore.vidconv.common.event.Listener#handleEvent(org.mycore.vidconv.common.event.Event)
     */
    @Override
    public void handleEvent(Event event) throws Exception {
        if (DirectoryWatchService.EVENT_ENTRY_CREATE.equals(event.getType())
            && event.getSource().equals(DirectoryWatchService.class)) {
            Path inputPath = event.getParameter("path");
            addConverter(inputPath);
        }

        if (ConverterJob.DONE.equals(event.getType())
            && event.getSource().equals(ConverterJob.class)) {

            Event ev = new Event(EVENT_CONVERT_DONE, this.getClass());
            ev.setParameter("job", converters.get(event.getParameter("id")));
            EVENT_MANAGER.fireEvent(ev);
        }
    }

    private void addConverter(final Path inputPath)
        throws InterruptedException, JAXBException, ExecutionException, IOException {
        final SettingsWrapper settings = SETTINGS.getSettings();

        if (settings != null && !settings.getOutput().isEmpty() && !Files.isDirectory(inputPath)) {
            if (FFMpegImpl.isEncodingSupported(inputPath)) {
                final String id = Long.toHexString(new Random().nextLong());
                final String fileName = inputPath.getFileName().toString();
                final Path outputPath = Paths.get(outputDir, id);

                final List<Output> outputs = settings.getOutput().stream()
                    .sorted(sortOutputs)
                    .filter(output -> {
                        try {
                            return output.getVideo().getUpscale()
                                || !FFMpegImpl.isUpscaling(inputPath, output.getVideo().getScale());
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                    }).map(o -> {
                        final Output out = o.getCopy();
                        final String appendix = Optional.ofNullable(o.getFilenameAppendix()).orElse(id);
                        try {
                            out.setInputPath(inputPath);
                            out.setOutputPath(outputPath.resolve(FFMpegImpl.filename(o.getFormat(), fileName,
                                appendix)));

                            return out;
                        } catch (ExecutionException e) {
                            throw new RuntimeException(e);
                        }
                    }).collect(Collectors.toList());

                final ConverterJob converter = new ConverterJob(id, outputs, inputPath, outputPath);

                converters.put(id, converter);
                converterThreadPool.submit(converter);
            } else {
                LOGGER.warn("encoding of file \"" + inputPath.toFile().getAbsolutePath() + "\" isn't supported.");
            }
        }
    }

    public static class ConverterJob implements Runnable {

        public static final String DONE = "done";

        private final String id;

        private final Path inputPath;

        private final Path outputPath;

        private final List<Output> outputs;

        private String command;

        private boolean running;

        private boolean done;

        private Instant addTime;

        private Instant startTime;

        private Instant endTime;

        private Integer exitValue;

        private StreamConsumer outputConsumer;

        private StreamConsumer errorConsumer;

        public ConverterJob(final String id, final List<Output> outputs, final Path inputPath, final Path outputPath)
            throws InterruptedException, IOException {
            this.id = id;
            this.outputs = outputs;
            this.inputPath = inputPath;
            this.outputPath = outputPath;
            this.addTime = Instant.now();
            this.done = false;
            this.running = false;

            command = FFMpegImpl.command(inputPath, outputs);

            if (!Files.exists(outputPath))
                Files.createDirectories(outputPath);
        }

        @SuppressWarnings("serial")
        @Override
        public void run() {
            try {
                LOGGER.info("Start converting of " + inputPath.toString() + " to " + outputPath.toString() + "...");

                save();

                done = false;
                running = true;
                startTime = Instant.now();

                final Executable exec = new Executable(command);

                final Process p = exec.run();

                outputConsumer = exec.outputConsumer();
                errorConsumer = exec.errorConsumer();

                p.waitFor();

                exitValue = p.exitValue();
                running = false;
                done = true;
                endTime = Instant.now();

                save();

                LOGGER.info("Converting of " + inputPath.toString() + " done.");

                EVENT_MANAGER.fireEvent(new Event(DONE, new HashMap<String, String>() {
                    {
                        put("id", id);
                    }
                }, this.getClass()));
            } catch (InterruptedException | JAXBException | ExecutionException | IOException e) {
                LOGGER.error(e.getMessage(), e);
            }
        }

        public String id() {
            return id;
        }

        public String command() {
            return MessageFormat.format(command,
                inputPath.toFile().getAbsolutePath(), outputPath.toFile().getAbsolutePath());
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

        private void save() throws JAXBException {
            final JAXBContext jc = JAXBContext.newInstance(ConverterWrapper.class);
            final Marshaller marshaller = jc.createMarshaller();
            marshaller.setProperty(MarshallerProperties.MEDIA_TYPE, "application/json");
            marshaller.setProperty(MarshallerProperties.JSON_INCLUDE_ROOT, false);
            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);

            marshaller.marshal(new ConverterWrapper(id, this),
                outputPath.resolve(".convert").toFile());
        }
    }
}
