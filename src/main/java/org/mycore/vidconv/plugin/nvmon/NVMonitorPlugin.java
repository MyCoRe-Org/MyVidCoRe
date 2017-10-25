/*
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
package org.mycore.vidconv.plugin.nvmon;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutionException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import javax.xml.bind.JAXBException;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlValue;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.glassfish.grizzly.websockets.WebSocket;
import org.glassfish.grizzly.websockets.WebSocketApplication;
import org.glassfish.grizzly.websockets.WebSocketEngine;
import org.mycore.vidconv.common.event.Event;
import org.mycore.vidconv.common.event.EventManager;
import org.mycore.vidconv.common.event.Listener;
import org.mycore.vidconv.common.util.Executable;
import org.mycore.vidconv.common.util.JsonUtils;
import org.mycore.vidconv.common.util.StreamConsumer;
import org.mycore.vidconv.plugin.GenericPlugin;
import org.mycore.vidconv.plugin.annotation.Plugin;
import org.mycore.vidconv.plugin.annotation.Plugin.Type;

/**
 * @author Ren\u00E9 Adler (eagle)
 *
 */
@Plugin(name = "Nvidia Monitor Plugin", description = "Monitors Nvidia GPU Cards.", type = Type.GENERIC)
public class NVMonitorPlugin extends GenericPlugin implements Runnable {

    public static final String EVENT_DATA = "nvmontor-data";

    public static final String WS_PATH = "/ws/nvmonitor";

    private static final Logger LOGGER = LogManager.getLogger();

    private static final String NV_EXECUTABLE = "nvidia-smi";

    private boolean enabled = false;

    private NVMonitorApplication wsApp;

    private String exePath;

    private Thread thread;

    private Timer timer;

    private StreamConsumer outputConsumer;

    private List<String> names;

    private List<String> units;

    /* (non-Javadoc)
     * @see org.mycore.vidconv.plugin.SimplePlugin#isEnabled()
     */
    @Override
    public boolean isEnabled() {
        return enabled;
    }

    /* (non-Javadoc)
     * @see org.mycore.vidconv.plugin.SimplePlugin#enable()
     */
    @Override
    public void enable() {
        exePath = findExecutableOnPath(NV_EXECUTABLE);
        if (exePath != null) {
            thread = new Thread(this);
            thread.start();
            LOGGER.info("register WebSocket on path \"" + WS_PATH + "\"...");
            wsApp = new NVMonitorApplication();
            WebSocketEngine.getEngine().register("", WS_PATH, wsApp);
            enabled = true;
        }
    }

    /* (non-Javadoc)
     * @see org.mycore.vidconv.plugin.SimplePlugin#disable()
     */
    @Override
    public void disable() {
        if (wsApp != null) {
            LOGGER.info("unregister WebSocket on path \"" + WS_PATH + "\"...");
            WebSocketEngine.getEngine().unregister(wsApp);
        }

        if (thread != null) {
            timer.cancel();
            thread.interrupt();
        }
        enabled = false;
    }

    /* (non-Javadoc)
     * @see java.lang.Runnable#run()
     */
    @Override
    public void run() {
        try {
            final Executable exec = new Executable(exePath + " dmon");

            final Process p = exec.run();

            timer = new Timer();
            timer.scheduleAtFixedRate(new NVMonitorTask(this), 0, 1000);

            outputConsumer = exec.outputConsumer();

            p.waitFor();
        } catch (InterruptedException | ExecutionException | IOException e) {
            LOGGER.error(e.getMessage(), e);
        }
    }

    private static String findExecutableOnPath(String name) {
        for (String dirname : System.getenv("PATH").split(File.pathSeparator)) {
            File file = new File(dirname, name);
            if (file.isFile() && file.canExecute()) {
                return file.getAbsolutePath();
            }
        }
        return null;
    }

    private static class NVMonitorTask extends TimerTask {

        private static final Pattern PATTERN_COLS = Pattern.compile("([^#\\s]+)");

        private NVMonitorPlugin parent;

        public NVMonitorTask(NVMonitorPlugin parent) {
            this.parent = parent;
        }

        /* (non-Javadoc)
         * @see java.util.TimerTask#run()
         */
        @Override
        public void run() {
            if (parent.outputConsumer != null && !parent.outputConsumer.getStreamOutput().isEmpty()) {
                final String out = parent.outputConsumer.getStreamOutput();
                final NVMonitor nvm = new NVMonitor();
                try (Scanner scanner = new Scanner(out)) {
                    while (scanner.hasNextLine()) {
                        String line = scanner.nextLine();
                        Matcher m = PATTERN_COLS.matcher(line);
                        if (line.startsWith("#")) {
                            if (parent.names == null) {
                                parent.names = new ArrayList<>();
                                while (m.find()) {
                                    parent.names.add(m.group(1));
                                }
                            } else if (parent.units == null) {
                                parent.units = new ArrayList<>();
                                while (m.find()) {
                                    parent.units.add(m.group(1));
                                }
                            }
                        } else {
                            final List<String> vals = new ArrayList<>();
                            while (m.find()) {
                                vals.add(m.group(1));
                            }

                            if (!vals.isEmpty() && vals.size() == parent.names.size()) {
                                nvm.entries.add(
                                    new NVEntry(
                                        IntStream.range(0, parent.names.size())
                                            .mapToObj(i -> new NVAttrib(parent.names.get(i), parent.units.get(i),
                                                vals.get(i)))
                                            .collect(Collectors.toList())));
                            }
                        }
                    }
                }
                parent.outputConsumer.clear(out.length());

                if (!nvm.entries.isEmpty()) {
                    EventManager.instance()
                        .fireEvent(
                            new Event<NVMonitor>(EVENT_DATA, nvm, parent.getClass()).setInternal(false));
                }
            }
        }

    }

    private static class NVMonitorApplication extends WebSocketApplication implements Listener {

        public NVMonitorApplication() {
            EventManager.instance().addListener(this);
        }

        /* (non-Javadoc)
         * @see org.mycore.vidconv.common.event.Listener#handleEvent(org.mycore.vidconv.common.event.Event)
         */
        @Override
        public void handleEvent(Event<?> event) throws Exception {
            if (event.getSource().equals(NVMonitorPlugin.class)) {
                NVMonitor nvm = (NVMonitor) event.getObject();
                try {
                    String json = JsonUtils.toJSON(nvm);
                    this.getWebSockets().parallelStream().filter(WebSocket::isConnected)
                        .forEach(ws -> ws.send(json));
                } catch (JAXBException | IOException e) {
                    LOGGER.error(e.getMessage(), e);
                }
            }
        }

    }

    @XmlRootElement(name = "nvmonitor")
    private static class NVMonitor {

        @XmlElement
        public List<NVEntry> entries = new ArrayList<>();

    }

    @XmlRootElement(name = "entry")
    private static class NVEntry {

        @XmlElement
        public List<NVAttrib> attribs;

        private NVEntry() {
        }

        public NVEntry(List<NVAttrib> attribs) {
            this();
            this.attribs = attribs;
        }
    }

    @XmlRootElement(name = "attrib")
    private static class NVAttrib {

        @XmlAttribute
        private String name;

        @XmlAttribute
        private String unit;

        @XmlValue
        private String value;

        private NVAttrib() {
        }

        public NVAttrib(String name, String unit, String value) {
            this();
            this.name = name;
            this.unit = unit;
            this.value = value;
        }

    }
}
