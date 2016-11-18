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
package org.mycore.vidconv.backend.service;

import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.vidconv.common.event.EventManager;
import org.mycore.vidconv.common.event.Listener;
import org.mycore.vidconv.common.event.annotation.AutoExecutable;
import org.mycore.vidconv.common.event.annotation.Startup;
import org.mycore.vidconv.plugin.annotation.Plugin;
import org.reflections.Reflections;

/**
 * @author Ren\u00E9 Adler (eagle)
 *
 */
@AutoExecutable(name = "Plugin Service")
public class PluginService {

    private static final Logger LOGGER = LogManager.getLogger(PluginService.class);

    private static final EventManager EVENT_MANGER = EventManager.instance();

    private static Set<Class<?>> plugins;

    static {
        final Reflections reflections = new Reflections("org.mycore.vidconv.plugin");
        plugins = reflections.getTypesAnnotatedWith(Plugin.class);
    }

    @Startup
    public static void startup() {
        loadPlugins();
    }

    private static void loadPlugins() {
        plugins.stream().forEach(p -> loadPlugin(p));
    }

    private static void loadPlugin(Class<?> plugin) {
        try {
            LOGGER.info("...load plugin " + plugin.getName());
            EVENT_MANGER.addListener((Listener) plugin.newInstance());
        } catch (InstantiationException | IllegalAccessException e) {
            LOGGER.error("Couldn't load plugin \"" + plugin.getName() + "\".", e);
        }
    }

}
