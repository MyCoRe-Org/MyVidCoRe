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
import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Timer;
import java.util.TimerTask;
import java.util.function.Function;
import java.util.stream.Collectors;

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
import org.mycore.vidconv.common.util.JsonUtils;
import org.mycore.vidconv.plugin.GenericPlugin;
import org.mycore.vidconv.plugin.annotation.Plugin;
import org.mycore.vidconv.plugin.annotation.Plugin.Type;

/**
 * @author Ren\u00E9 Adler (eagle)
 *
 */
@Plugin(name = "System Monitor Plugin", description = "Monitors System.", type = Type.GENERIC)
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

        private OperatingSystemMXBean osBean;

        private List<Method> mCache;

        public SystemMonitorTask(SystemMonitorPlugin parent) {
            this.parent = parent;

            osBean = ManagementFactory.getOperatingSystemMXBean();
            mCache = Arrays.stream(osBean.getClass().getDeclaredMethods())
                .map(m -> {
                    m.setAccessible(true);
                    return m;
                }).filter(m -> Modifier.isPublic(m.getModifiers())).collect(Collectors.toList());
        }

        /* (non-Javadoc)
         * @see java.util.TimerTask#run()
         */
        @Override
        public void run() {
            SysMonitor sysm = new SysMonitor(mCache.stream().map(m -> new SysAttrib(getPropertyName(m),
                getSystemBeanValue((Object o) -> o.toString(), m.getName()))).collect(Collectors.toList()));

            EventManager.instance()
                .fireEvent(
                    new Event<SysMonitor>(EVENT_DATA, sysm, parent.getClass()).setInternal(false));
        }

        private <T> T getSystemBeanValue(Function<Object, T> valueTransformer, String... mmethodNames) {
            return mCache.stream()
                .filter(m -> Arrays.stream(mmethodNames).anyMatch(mn -> mn.equalsIgnoreCase(m.getName()))).findFirst()
                .map(m -> getMethodValue(m, valueTransformer)).orElse(null);

        }

        private <T> T getMethodValue(Method m, Function<Object, T> valueTransformer) {
            try {
                return valueTransformer.apply(m.invoke(osBean));
            } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
                return null;
            }
        }

        private String getPropertyName(Method m) {
            String pn = m.getName().replaceAll("get", "");
            return pn.substring(0, 1).toLowerCase(Locale.ROOT) + pn.substring(1);
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

        @XmlValue
        private String value;

        private SysAttrib() {
        }

        public SysAttrib(String name, String value) {
            this();
            this.name = name;
            this.value = value;
        }

    }

}
