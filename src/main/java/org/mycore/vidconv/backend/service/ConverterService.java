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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.MessageFormat;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
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
import org.mycore.vidconv.frontend.entity.SMILWrapper;
import org.mycore.vidconv.frontend.entity.SettingsWrapper;
import org.mycore.vidconv.frontend.entity.SettingsWrapper.Output;
import org.mycore.vidconv.frontend.widget.Widget;

/**
 * @author Ren\u00E9 Adler (eagle)
 *
 */
public class ConverterService extends Widget implements Listener {

    public static final String WIDGET_NAME = "converter";

    private static final Logger LOGGER = LogManager.getLogger(ConverterService.class);

    private static final EventManager EVENT_MANGER = EventManager.instance();

    private static final Settings CONFIG = Settings.instance();

    private final Map<String, ConverterJob> converters = new ConcurrentHashMap<>();

    private final ExecutorService converterThreadPool;

    private String outputDir;

    public ConverterService(final String outputDir, int converterThreads) {
        super(WIDGET_NAME);

        this.outputDir = outputDir;
        EVENT_MANGER.addListener(this);
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
            final ConverterJob converter = converters.get(converterId);

            if (converter != null) {
                return converter.outputPath;
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

            final List<ConverterJob> jobs = converters.values().stream().filter(cj -> {
                return cj.parentId().equals(event.getParameter("parentId"));
            }).collect(Collectors.toList());

            if (jobs.size() > 1 && jobs.stream().filter(cj -> !cj.isDone() || cj.isRunning()).count() == 0) {
                final ConverterJob job = jobs.get(0);
                final String fileName = job.inputPath.getFileName().toString();
                final Path file = job.outputPath.getParent()
                    .resolve(fileName.substring(0, fileName.lastIndexOf(".")) + ".smil");
                SMILWrapper.saveTo(file, jobs.stream().map(cj -> {
                    try {
                        return FFMpegImpl.probe(cj.outputPath);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }).collect(Collectors.toList()));
            }
        }
    }

    private void addConverter(final Path inputPath) throws InterruptedException, JAXBException, ExecutionException {
        final SettingsWrapper settings = CONFIG.getSettings();

        if (settings != null && !settings.getOutput().isEmpty()) {
            if (!Files.isDirectory(inputPath)) {
                if (FFMpegImpl.isEncodingSupported(inputPath)) {
                    final String parentId = Long.toHexString(new Random().nextLong());
                    final String fileName = inputPath.getFileName().toString();

                    List<Output> outputs = settings.getOutput().stream()
                        .sorted((o2, o1) -> o1.getFormat().equals(o2.getFormat())
                            ? o1.getVideo().getScale().compareTo(o2.getVideo().getScale())
                            : o1.getFormat().compareTo(o2.getFormat()))
                        .filter(output -> {
                            try {
                                return output.getVideo().getUpscale()
                                    || !FFMpegImpl.isUpscaling(inputPath, output.getVideo().getScale());
                            } catch (Exception e) {
                                throw new RuntimeException(e);
                            }
                        }).collect(Collectors.toList());

                    outputs.forEach(output -> {
                        try {
                            final String id = outputs.size() == 1 ? parentId
                                : Long.toHexString(new Random().nextLong());

                            final String appendix = Optional.ofNullable(output.getFilenameAppendix()).orElse(id);
                            final Path outputPath = Paths.get(outputDir, parentId,
                                FFMpegImpl.filename(output.getFormat(), fileName,
                                    appendix));

                            if (!Files.exists(outputPath.getParent()))
                                Files.createDirectories(outputPath.getParent());

                            final String command = FFMpegImpl.command(output);
                            final ConverterJob converter = new ConverterJob(parentId, id, command, inputPath,
                                outputPath);

                            converters.put(id, converter);
                            converterThreadPool.submit(converter);
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                    });
                } else {
                    LOGGER.warn("encoding of file \"" + inputPath.toFile().getAbsolutePath() + "\" isn't supported.");
                }
            }
        }
    }

    public static class ConverterJob implements Runnable {

        public static final String DONE = "done";

        private final String parentId;

        private final String id;

        private final Path inputPath;

        private final Path outputPath;

        private final String command;

        private boolean running;

        private boolean done;

        private Instant addTime;

        private Instant startTime;

        private Instant endTime;

        private Integer exitValue;

        private StreamConsumer outputConsumer;

        private StreamConsumer errorConsumer;

        public ConverterJob(final String parentId, final String id, final String command, final Path inputPath,
            final Path outputPath) {
            this.parentId = parentId;
            this.id = id;
            this.command = command;
            this.inputPath = inputPath;
            this.outputPath = outputPath;
            this.addTime = Instant.now();
            this.done = false;
            this.running = false;
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

                final Executable exec = new Executable(
                    Arrays.stream(command.split(" ")).map(s -> MessageFormat.format(s,
                        inputPath.toFile().getAbsolutePath(), outputPath.toFile().getAbsolutePath()))
                        .collect(Collectors.toList()));

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

                EventManager.instance().fireEvent(new Event(DONE, new HashMap<String, String>() {
                    {
                        put("parentId", parentId);
                        put("id", id);
                    }
                }, this.getClass()));
            } catch (InterruptedException | JAXBException | ExecutionException | IOException e) {
                LOGGER.error(e.getMessage(), e);
            }
        }

        public String parentId() {
            return parentId;
        }

        public String id() {
            return id;
        }

        public String command() {
            return MessageFormat.format(command,
                inputPath.toFile().getAbsolutePath(), outputPath.toFile().getAbsolutePath());
        }

        public String fileName() {
            return outputPath.getFileName().toString();
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
                outputPath.getParent().resolve(".convert").toFile());
        }
    }
}
