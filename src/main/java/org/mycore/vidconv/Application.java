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

import java.io.File;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.vidconv.event.Event;
import org.mycore.vidconv.event.EventManager;
import org.mycore.vidconv.event.Listener;
import org.mycore.vidconv.service.ConverterService;
import org.mycore.vidconv.service.DirectoryWatchService;
import org.mycore.vidconv.service.EmbeddedHttpServer;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;

/**
 * @author Ren\u00E9 Adler (eagle)
 *
 */
public class Application implements Listener {

    private static final Logger LOGGER = LogManager.getRootLogger();

    private static final EventManager EVENT_MANGER = EventManager.instance();

    @Parameter(names = { "-h", "--help" }, description = "Print help (this message) and exit", help = true)
    private boolean help;

    @Parameter(names = { "--port", "-p" }, description = "Set port listen on")
    private Integer port = 8085;

    @Parameter(names = "--host", description = "Set host listen on")
    private String host;

    @Parameter(names = { "-ct", "--converterThreads" }, description = "Set the num of converter threads")
    private Integer converterThreads;

    @Parameter(names = "--watchDir", description = "Set directory to watch for incomming videos")
    private String watchDir = System.getProperty("java.io.tmpdir");

    @Parameter(names = "--outputDir", description = "Set directory to output converted videos")
    private String outputDir = System.getProperty("java.io.tmpdir");

    public static void main(String[] args) {
        Application app = new Application();
        JCommander jcmd = new JCommander(app, args);

        if (app.help) {
            jcmd.usage();
        } else {
            try {
                app.run();
            } catch (Exception e) {
                LOGGER.error(e.getMessage(), e);
            }
        }
    }

    private void run() throws Exception {
        EVENT_MANGER.addListener(this);

        EmbeddedHttpServer embeddedHttpServer = new EmbeddedHttpServer(host, port);
        embeddedHttpServer.start();

        DirectoryWatchService directoryWatchService = new DirectoryWatchService();
        directoryWatchService.registerDirectory((new File(watchDir)).toPath());

        int availableProcessors = Runtime.getRuntime().availableProcessors();
        converterThreads = converterThreads == null ? availableProcessors - Math.floorDiv(availableProcessors, 4)
                : converterThreads;
        LOGGER.info("Make use of " + converterThreads + " from available "
                + Runtime.getRuntime().availableProcessors() + " processors.");
        new ConverterService(outputDir, converterThreads);
    }

    /* (non-Javadoc)
     * @see org.mycore.vidconv.event.Listener#handleEvent(org.mycore.vidconv.event.Event)
     */
    @Override
    public void handleEvent(Event event) throws Exception {
    }
}
