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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.vidconv.config.Settings;
import org.mycore.vidconv.encoder.FFMpegImpl;
import org.mycore.vidconv.entity.ConverterWrapper;
import org.mycore.vidconv.entity.ConvertersWrapper;
import org.mycore.vidconv.entity.SettingsWrapper;
import org.mycore.vidconv.event.Event;
import org.mycore.vidconv.event.EventManager;
import org.mycore.vidconv.event.Listener;
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

    private void addConverter(final Path inputPath) throws IOException, InterruptedException, ExecutionException {
        final SettingsWrapper settings = CONFIG.getSettings();

        if (settings != null) {
            final String id = Long.toHexString(new Random().nextLong());
            final String command = FFMpegImpl.command(settings);
            if (!Files.isDirectory(inputPath)) {
                final String fileName = inputPath.getFileName().toString();
                final Path outputPath = Paths.get(outputDir, id, FFMpegImpl.filename(settings, fileName));

                if (!Files.exists(outputPath.getParent()))
                    Files.createDirectories(outputPath.getParent());

                final ConverterJob converter = new ConverterJob(command, inputPath, outputPath);
                converts.put(id, converter);
                converterThreadPool.submit(converter);
            }
        }
    }

    public static class ConverterJob implements Runnable {

        private final Path inputPath;

        private final Path outputPath;

        private final String command;

        private boolean running;

        private boolean done;

        private Instant startTime;

        private Instant endTime;

        private StreamConsumer outputConsumer;

        private StreamConsumer errorConsumer;

        public ConverterJob(final String command, final Path inputPath, final Path outputPath) {
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
                done = false;
                running = true;
                startTime = Instant.now();

                final Process p = Runtime.getRuntime()
                        .exec(Arrays.stream(command.split(" ")).map(s -> MessageFormat.format(s,
                                inputPath.toFile().getAbsolutePath(), outputPath.toFile().getAbsolutePath()))
                                .toArray(String[]::new));

                outputConsumer = new StreamConsumer(p.getInputStream());
                errorConsumer = new StreamConsumer(p.getErrorStream());

                new Thread(outputConsumer).start();
                new Thread(errorConsumer).start();

                p.waitFor();
                running = false;
                done = true;
                endTime = Instant.now();
                LOGGER.info("Converting of " + inputPath.toString() + " done.");
            } catch (IOException | InterruptedException e) {
                LOGGER.error(e.getMessage(), e);
            }
        }

        public String getCommand() {
            return MessageFormat.format(command,
                    inputPath.toFile().getAbsolutePath(), outputPath.toFile().getAbsolutePath());
        }

        public String getFileName() {
            return outputPath.getFileName().toString();
        }

        public boolean isRunning() {
            return running;
        }

        public boolean isDone() {
            return done;
        }

        public Instant getStartTime() {
            return startTime;
        }

        public Instant getEndTime() {
            return endTime;
        }

        public String outputStream() {
            return outputConsumer != null ? outputConsumer.getStreamOutput() : null;
        }

        public String errorStream() {
            return errorConsumer != null ? errorConsumer.getStreamOutput() : null;
        }
    }
}
