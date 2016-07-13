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
package org.mycore.vidconv.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.MessageFormat;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.persistence.jaxb.MarshallerProperties;
import org.mycore.vidconv.config.Settings;
import org.mycore.vidconv.encoder.FFMpegImpl;
import org.mycore.vidconv.entity.ConverterWrapper;
import org.mycore.vidconv.entity.ConvertersWrapper;
import org.mycore.vidconv.entity.SettingsWrapper;
import org.mycore.vidconv.event.Event;
import org.mycore.vidconv.event.EventManager;
import org.mycore.vidconv.event.Listener;
import org.mycore.vidconv.util.Executable;
import org.mycore.vidconv.util.StreamConsumer;
import org.mycore.vidconv.widget.Widget;

/**
 * @author Ren\u00E9 Adler (eagle)
 *
 */
public class ConverterService extends Widget implements Listener {

    public static final String WIDGET_NAME = "converter";

    private static final Logger LOGGER = LogManager.getLogger(ConverterService.class);

    private static final EventManager EVENT_MANGER = EventManager.instance();

    private static final Settings CONFIG = Settings.instance();

    private final Map<String, ConverterJob> converts = new ConcurrentHashMap<>();

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
     * @see org.mycore.vidconv.widget.Widget#status()
     */
    @Override
    public ConvertersWrapper status() {
        return new ConvertersWrapper(this.converts);
    }

    /* (non-Javadoc)
     * @see org.mycore.vidconv.widget.Widget#status(java.util.List)
     */
    @Override
    public ConverterWrapper status(List<String> params) {
        final String converterId = params.get(0);
        final ConverterJob converter = converts.get(converterId);

        if (converter != null) {
            return new ConverterWrapper(converterId, converter);
        }

        return null;
    }

    /* (non-Javadoc)
     * @see org.mycore.vidconv.widget.Widget#download(java.util.List)
     */
    @Override
    public Path download(List<String> params) throws Exception {
        if (!params.isEmpty()) {
            final String converterId = params.get(0);
            final ConverterJob converter = converts.get(converterId);

            if (converter != null) {
                return converter.outputPath;
            }
        }

        return null;
    }

    /* (non-Javadoc)
     * @see org.mycore.vidconv.event.Listener#handleEvent(org.mycore.vidconv.event.Event)
     */
    @Override
    public void handleEvent(Event event) throws Exception {
        if (DirectoryWatchService.EVENT_ENTRY_CREATE.equals(event.getType())
                && event.getSource().equals(DirectoryWatchService.class)) {
            Path inputPath = event.getParameter("path", Path.class);
            addConverter(inputPath);
        }
    }

    private void addConverter(final Path inputPath)
            throws IOException, InterruptedException, ExecutionException, JAXBException {
        final SettingsWrapper settings = CONFIG.getSettings();

        if (settings != null) {
            if (!Files.isDirectory(inputPath)) {
                if (FFMpegImpl.isEncodingSupported(inputPath)) {
                    final String id = Long.toHexString(new Random().nextLong());
                    final String fileName = inputPath.getFileName().toString();
                    final Path outputPath = Paths.get(outputDir, id, FFMpegImpl.filename(settings, fileName));

                    if (!Files.exists(outputPath.getParent()))
                        Files.createDirectories(outputPath.getParent());

                    final String command = FFMpegImpl.command(settings);
                    final ConverterJob converter = new ConverterJob(id, command, inputPath, outputPath);
                    converts.put(id, converter);
                    converterThreadPool.submit(converter);
                } else {
                    LOGGER.warn("encoding of file \"" + inputPath.toFile().getAbsolutePath() + "\" isn't supported.");
                }
            }
        }
    }

    public static class ConverterJob implements Runnable {

        private final String id;

        private final Path inputPath;

        private final Path outputPath;

        private final String command;

        private boolean running;

        private boolean done;

        private Instant startTime;

        private Instant endTime;

        private Integer exitValue;

        private StreamConsumer outputConsumer;

        private StreamConsumer errorConsumer;

        public ConverterJob(final String id, final String command, final Path inputPath, final Path outputPath) {
            this.id = id;
            this.command = command;
            this.inputPath = inputPath;
            this.outputPath = outputPath;
            this.done = false;
            this.running = false;
        }

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
            } catch (IOException | InterruptedException | JAXBException e) {
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

        public String fileName() {
            return outputPath.getFileName().toString();
        }

        public boolean isRunning() {
            return running;
        }

        public boolean isDone() {
            return done;
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

            marshaller.marshal(new ConverterWrapper(id, this), outputPath.getParent().resolve(".convert").toFile());
        }
    }
}
