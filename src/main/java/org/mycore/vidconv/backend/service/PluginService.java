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

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.vidconv.common.ClassTools;
import org.mycore.vidconv.common.config.Settings;
import org.mycore.vidconv.common.event.annotation.AutoExecutable;
import org.mycore.vidconv.common.event.annotation.Startup;
import org.mycore.vidconv.frontend.entity.SettingsWrapper;
import org.mycore.vidconv.plugin.GenericPlugin;
import org.mycore.vidconv.plugin.annotation.Plugin;
import org.reflections.Reflections;

/**
 * @author Ren\u00E9 Adler (eagle)
 *
 */
@AutoExecutable(name = "Plugin Service")
public class PluginService {

    private static final Logger LOGGER = LogManager.getLogger(PluginService.class);

    private static final Settings SETTINGS = Settings.instance();

    private static final Set<Class<?>> PLUGIN_CACHE;

    private static Map<String, GenericPlugin> plugins;

    static {
        final Reflections reflections = new Reflections("org.mycore.vidconv.plugin");
        PLUGIN_CACHE = reflections.getTypesAnnotatedWith(Plugin.class);
    }

    @SuppressWarnings("unchecked")
    @Startup
    protected static void loadPlugins() {
        plugins = new ConcurrentHashMap<>();

        PLUGIN_CACHE.stream().forEach(p -> loadPlugin((Class<? extends GenericPlugin>) p));
    }

    private static void loadPlugin(Class<? extends GenericPlugin> p) {
        Plugin pa = p.getAnnotation(Plugin.class);
        LOGGER.info("load plugin " + pa.name() + "...");
        try {
            plugins.put(pa.name(), ClassTools.newInstance(p));

            boolean enabled = Optional.ofNullable(isPluginEnabled(pa.name())).orElse(pa.enabled());
            if (enabled) {
                enablePlugin(pa.name());
            } else {
                LOGGER.info("...disabled.");
            }
        } catch (InstantiationException | IllegalAccessException e) {
            LOGGER.error("...error on loading", e);
        }
    }

    private static Boolean isPluginEnabled(String name) {
        if (SETTINGS.getSettings() != null) {
            Optional<SettingsWrapper.Plugin> pn = Optional.ofNullable(SETTINGS.getSettings().getPlugins())
                    .map(pl -> pl.stream()
                            .filter(sp -> sp.getName() == name)
                            .findFirst())
                    .orElse(Optional.empty());

            return pn.isPresent() ? pn.get().isEnabled() : null;
        }

        return null;
    }

    public static Map<String, GenericPlugin> plugins() {
        return plugins;
    }

    public static void enablePlugin(String name) throws InstantiationException, IllegalAccessException {
        GenericPlugin plugin = plugins.get(name);
        if (plugin != null) {
            plugin.enable();
            if (plugin.isEnabled()) {
                LOGGER.info("...enabled.");
            } else {
                LOGGER.info("...disabled.");
            }
        }
    }

    public static void disablePlugin(String name) {
        GenericPlugin plugin = plugins.get(name);
        if (plugin != null) {
            plugin.disable();
        }
    }
}
