/*
 * This file is part of ***  M y C o R e  ***
 * See http://www.mycore.de/ for details.
 *
 * This program is free software; you can use it, redistribute it
 * and / or modify it under the terms of the GNU General Public License
 * (GPL) as published by the Free Software Foundation; either version 2
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
package org.mycore.vidconv.plugin.sysmon;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Timer;
import java.util.TimerTask;
import java.util.stream.IntStream;

import javax.xml.bind.JAXBException;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.glassfish.grizzly.websockets.WebSocket;
import org.glassfish.grizzly.websockets.WebSocketApplication;
import org.glassfish.grizzly.websockets.WebSocketEngine;
import org.mycore.vidconv.common.event.Event;
import org.mycore.vidconv.common.event.EventManager;
import org.mycore.vidconv.common.event.Listener;
import org.mycore.vidconv.common.util.JsonUtils;
import org.mycore.vidconv.plugin.GenericPlugin;
import org.mycore.vidconv.plugin.annotation.Plugin;
import org.mycore.vidconv.plugin.annotation.Plugin.Type;

import oshi.SystemInfo;
import oshi.hardware.CentralProcessor;
import oshi.hardware.GlobalMemory;
import oshi.hardware.HardwareAbstractionLayer;
import oshi.hardware.Sensors;

/**
 * @author Ren\u00E9 Adler (eagle)
 *
 */
@Plugin(name = "System Monitor Plugin", description = "Monitors System.", type = Type.GENERIC, enabled = true)
public class SystemMonitorPlugin extends GenericPlugin {

    public static final String EVENT_DATA = "sysmonitor-data";

    public static final String WS_PATH = "/ws/sysmonitor";

    private static final Logger LOGGER = LogManager.getLogger();

    private boolean enabled = false;

    private SystemMonitorApplication wsApp;

    private Timer timer;

    /* (non-Javadoc)
     * @see org.mycore.vidconv.plugin.GenericPlugin#isEnabled()
     */
    @Override
    public boolean isEnabled() {
        return enabled;
    }

    /* (non-Javadoc)
     * @see org.mycore.vidconv.plugin.GenericPlugin#enable()
     */
    @Override
    public void enable() {
        LOGGER.info("register WebSocket on path \"" + WS_PATH + "\"...");
        wsApp = new SystemMonitorApplication();
        WebSocketEngine.getEngine().register("", WS_PATH, wsApp);
        timer = new Timer();
        timer.scheduleAtFixedRate(new SystemMonitorTask(this), 0, 1000);
        enabled = true;
    }

    /* (non-Javadoc)
     * @see org.mycore.vidconv.plugin.GenericPlugin#disable()
     */
    @Override
    public void disable() {
        if (wsApp != null) {
            LOGGER.info("unregister WebSocket on path \"" + WS_PATH + "\"...");
            WebSocketEngine.getEngine().unregister(wsApp);
        }
        Optional.ofNullable(timer).ifPresent(Timer::cancel);
        enabled = false;
    }

    private static class SystemMonitorTask extends TimerTask {

        private SystemMonitorPlugin parent;

        private SystemInfo si;

        private HardwareAbstractionLayer hal;

        private long[] prevTicks;

        private long[][] prevProcTicks;

        public SystemMonitorTask(SystemMonitorPlugin parent) {
            this.parent = parent;

            this.si = new SystemInfo();
            this.hal = si.getHardware();
        }

        /* (non-Javadoc)
         * @see java.util.TimerTask#run()
         */
        @Override
        public void run() {
            List<SysAttrib> attribs = new ArrayList<>();

            cpuLoad(attribs, hal.getProcessor());
            memory(attribs, hal.getMemory());
            sensors(attribs, hal.getSensors());

            SysMonitor sysm = new SysMonitor(attribs);

            EventManager.instance()
                    .fireEvent(new Event<SysMonitor>(EVENT_DATA, sysm, parent.getClass()).setInternal(false));
        }

        private void cpuLoad(List<SysAttrib> attribs, CentralProcessor processor) {
            if (processor != null) {
                if (prevProcTicks != null) {
                    double[] load = processor.getProcessorCpuLoadBetweenTicks(prevProcTicks);
                    IntStream.range(0, load.length).forEach(i -> attribs
                            .add(new SysAttrib("cpu-" + i + "-load", String.format(Locale.ROOT, " %.1f", load[i] * 100),
                                    "%")));
                }

                prevProcTicks = processor.getProcessorCpuLoadTicks();

                if (prevTicks != null) {
                    attribs.add(new SysAttrib("cpu-load", String.format(Locale.ROOT, " %.1f",
                            processor.getSystemCpuLoadBetweenTicks(prevTicks) * 100), "%"));
                }

                prevTicks = processor.getSystemCpuLoadTicks();
            }
        }

        private void memory(List<SysAttrib> attribs, GlobalMemory memory) {
            if (memory != null) {
                attribs.add(new SysAttrib("mem-total", Long.toString(memory.getTotal()), "B"));
                attribs.add(new SysAttrib("mem-available", Long.toString(memory.getAvailable()), "B"));

                if (memory.getVirtualMemory() != null) {
                    attribs.add(
                            new SysAttrib("swap-total", Long.toString(memory.getVirtualMemory().getSwapTotal()),
                                    "B"));
                    attribs.add(
                            new SysAttrib("swap-used", Long.toString(memory.getVirtualMemory().getSwapUsed()),
                                    "B"));
                }
            }
        }

        private void sensors(List<SysAttrib> attribs, Sensors sensors) {
            if (sensors != null) {
                attribs.add(new SysAttrib("cpu-temp", String.format(Locale.ROOT, " %.1f",
                        sensors.getCpuTemperature()), "C"));
                attribs.add(new SysAttrib("cpu-voltage", String.format(Locale.ROOT, " %.1f",
                        sensors.getCpuVoltage()), "V"));

                int[] fans = sensors.getFanSpeeds();
                IntStream.range(0, fans.length)
                        .forEach(i -> new SysAttrib("fan-" + i, Integer.toString(fans[i]), "rpm"));
            }
        }
    }

    private static class SystemMonitorApplication extends WebSocketApplication implements Listener {

        public SystemMonitorApplication() {
            EventManager.instance().addListener(this);
        }

        /* (non-Javadoc)
         * @see org.mycore.vidconv.common.event.Listener#handleEvent(org.mycore.vidconv.common.event.Event)
         */
        @Override
        public void handleEvent(Event<?> event) throws Exception {
            if (event.getSource().equals(SystemMonitorPlugin.class)) {
                SysMonitor sysm = (SysMonitor) event.getObject();
                try {
                    String json = JsonUtils.toJSON(sysm);
                    this.getWebSockets().parallelStream().filter(WebSocket::isConnected)
                            .forEach(ws -> ws.send(json));
                } catch (JAXBException | IOException e) {
                    LOGGER.error(e.getMessage(), e);
                }
            }
        }

    }

    @XmlRootElement(name = "sysmonitor")
    private static class SysMonitor {

        @XmlElement
        public List<SysAttrib> attribs = new ArrayList<>();

        private SysMonitor() {
        }

        public SysMonitor(List<SysAttrib> attribs) {
            this();
            this.attribs = attribs;
        }

    }

    @XmlRootElement(name = "attrib")
    private static class SysAttrib {

        @XmlAttribute
        private String name;

        @XmlAttribute
        private String value;

        @XmlAttribute
        private String unit;

        private SysAttrib() {
        }

        public SysAttrib(String name, String value, String unit) {
            this();
            this.name = name;
            this.value = value;
            this.unit = unit;
        }

    }

}
