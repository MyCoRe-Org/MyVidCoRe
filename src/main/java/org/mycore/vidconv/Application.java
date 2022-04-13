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
package org.mycore.vidconv;

import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Optional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.vidconv.backend.service.ConverterService;
import org.mycore.vidconv.backend.service.DirectoryWatchService;
import org.mycore.vidconv.backend.service.EmbeddedHttpServer;
import org.mycore.vidconv.common.config.Configuration;
import org.mycore.vidconv.common.config.ConfigurationDir;
import org.mycore.vidconv.common.event.AutoExecutableHandler;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;

/**
 * @author Ren\u00E9 Adler (eagle)
 *
 */
public class Application {

    private static final Logger LOGGER = LogManager.getRootLogger();

    private static Application app;

    @Parameter(names = { "-h", "--help" }, description = "Print help (this message) and exit", help = true)
    private boolean help;

    @Parameter(names = { "--port", "-p" }, description = "Set port listen on")
    private Integer port = 8085;

    @Parameter(names = "--host", description = "Set host listen on")
    private String host;

    @Parameter(names = { "-ct", "--converterThreads" }, description = "Set the num of converter threads")
    private Integer converterThreads;

    @Parameter(names = "--watchDir", description = "Set directory to watch for incomming videos")
    private String watchDir;

    @Parameter(names = "--outputDir", description = "Set directory to output converted videos")
    private String outputDir;

    @Parameter(names = "--tempDir", description = "Set directory for temporary files")
    private String tempDir = System.getProperty("java.io.tmpdir");

    @Parameter(names = { "--configDir", "-cd" }, description = "Set configuration dir")
    private String configDir;

    private Thread shutdownHook;

    private EmbeddedHttpServer embeddedHttpServer;

    public static void main(String[] args) {
        app = new Application();
        JCommander jcmd = new JCommander(app, null, args);

        if (app.help) {
            jcmd.usage();
        } else {
            try {
                if (app.watchDir == null || app.outputDir == null || app.tempDir == null) {
                    jcmd.usage();
                    return;
                }

                if (Files.notExists(Paths.get(app.watchDir))) {
                    throw new IllegalArgumentException("Watching directory \"" + app.watchDir + "\" isn't exists.");
                } else if (Paths.get(app.watchDir).equals(Paths.get(app.outputDir).getParent())
                        || Paths.get(app.watchDir).equals(Paths.get(app.outputDir))) {
                    throw new IllegalArgumentException(
                            "Watching directory isn't allowed to be the parent of output directory or the same.");
                } else if (Files.notExists(Paths.get(app.tempDir))) {
                    throw new IllegalArgumentException(
                            "Temporary file directory \"" + app.tempDir + "\" isn't exists.");
                } else if (Paths.get(app.tempDir).equals(Paths.get(app.outputDir).getParent())
                        || Paths.get(app.tempDir).equals(Paths.get(app.outputDir))) {
                    throw new IllegalArgumentException(
                            "Temporary file directory isn't allowed to be the parent of output directory or the same.");
                }

                app.run();
            } catch (Exception e) {
                LOGGER.error(e.getMessage(), e);
            }
        }
    }

    public static void exit() {
        try {
            app.stop();
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
        }
    }

    private Application() {
        if (!(Application.class.getClassLoader() instanceof URLClassLoader)) {
            System.out.println("Current ClassLoader is not extendable at runtime. Using workaround.");
            Thread.currentThread().setContextClassLoader(new URLClassLoader(new URL[0]));
        }
    }

    private void run() throws Exception {
        if (configDir != null) {
            ConfigurationDir.setConfigurationDirectory(configDir);
        }

        AutoExecutableHandler.setHaltOnError(false);
        shutdownHook = new Thread(() -> AutoExecutableHandler.shutdown());
        Runtime.getRuntime().addShutdownHook(shutdownHook);

        AutoExecutableHandler.startup();

        embeddedHttpServer = new EmbeddedHttpServer(host, port);
        embeddedHttpServer.start();

        Configuration.instance().set("APP.BaseURL",
                Optional.ofNullable(Configuration.instance().getString("APP.BaseURL", null))
                        .orElse(embeddedHttpServer.getURI().toString()));

        int availableProcessors = Runtime.getRuntime().availableProcessors();
        converterThreads = converterThreads == null ? Math.max(Math.floorDiv(availableProcessors, 4), 1)
                : converterThreads;
        LOGGER.info("Use {} converter threads for available {} processors.", converterThreads, availableProcessors);
        new ConverterService(outputDir, tempDir, converterThreads);

        DirectoryWatchService.instance().registerDirectory(Paths.get(watchDir));
    }

    private void stop() throws Exception {
        embeddedHttpServer.stop();
        AutoExecutableHandler.shutdown();
        Runtime.getRuntime().removeShutdownHook(shutdownHook);
    }
}
