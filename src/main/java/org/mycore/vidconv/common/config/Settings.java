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
package org.mycore.vidconv.common.config;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.stream.Collectors;

import jakarta.xml.bind.JAXBException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.vidconv.common.util.JsonUtils;
import org.mycore.vidconv.frontend.entity.SettingsWrapper;
import org.mycore.vidconv.frontend.entity.SettingsWrapper.Output;

/**
 * @author Ren\u00E9 Adler (eagle)
 *
 */
public class Settings {

    public static Comparator<Output> sortOutputs = (o1, o2) -> {
        if (o1.getFormat().equals(o2.getFormat())) {
            if (o1.getVideo().getScale() != null && o2.getVideo().getScale() != null) {
                final Integer[] sc1 = Arrays.stream(o1.getVideo().getScale().split(":")).map(Integer::valueOf)
                        .toArray(Integer[]::new);
                final Integer[] sc2 = Arrays.stream(o2.getVideo().getScale().split(":")).map(Integer::valueOf)
                        .toArray(Integer[]::new);
                return sc1[0] < 0 && sc1[0] < 0 ? Integer.compare(sc2[1], sc1[1]) : Integer.compare(sc2[1], sc1[1]);
            }

            return 0;
        }

        return o1.getFormat().compareTo(o2.getFormat());
    };

    private static final Logger LOGGER = LogManager.getLogger();

    private static Settings INSTANCE;

    private final File configDir = ConfigurationDir.getConfigurationDirectory();

    private SettingsWrapper settings;

    public static Settings instance() {
        if (INSTANCE == null) {
            INSTANCE = new Settings();
        }

        return INSTANCE;
    }

    private Settings() {
        if (!configDir.exists()) {
            configDir.mkdirs();
        }

        try {
            loadSettings();
        } catch (JAXBException | IOException e) {
            LOGGER.error("Could not instantiate Settings.", e);
        }
    }

    /**
     * @return the settings
     */
    public SettingsWrapper getSettings() {
        return settings;
    }

    /**
     * @param settings the settings to set
     * @throws JAXBException 
     * @throws IOException 
     */
    public void setSettings(SettingsWrapper settings) throws JAXBException, IOException {
        settings.setOutput(settings.getOutput().stream()
                .sorted(sortOutputs).collect(Collectors.toList()));
        this.settings = settings;
        saveSettings(this.settings);
    }

    /**
     * clears all settings
     */
    public void clearSettings() {
        this.settings = null;
        ConfigurationDir.getConfigFile("settings.json").delete();
    }

    private void loadSettings() throws JAXBException, IOException {
        final File sFile = ConfigurationDir.getConfigFile("settings.json");
        if (sFile.exists()) {
            setSettings(JsonUtils.loadJSON(sFile, SettingsWrapper.class, true));
        }
    }

    private void saveSettings(final SettingsWrapper settings) throws JAXBException, IOException {
        final File sFile = ConfigurationDir.getConfigFile("settings.json");
        JsonUtils.saveJSON(sFile, settings, true, true);
    }
}
