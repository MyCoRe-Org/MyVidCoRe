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
package org.mycore.vidconv.config;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.stream.StreamSource;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.persistence.jaxb.MarshallerProperties;
import org.eclipse.persistence.jaxb.UnmarshallerProperties;
import org.mycore.vidconv.entity.SettingsWrapper;

/**
 * @author Ren\u00E9 Adler (eagle)
 *
 */
public class Settings {

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
     */
    public void setSettings(SettingsWrapper settings) throws JAXBException {
        this.settings = settings;
        saveSettings(this.settings);
    }

    private void loadSettings() throws JAXBException, IOException {
        final File sFile = ConfigurationDir.getConfigFile("settings.json");
        if (sFile.exists()) {
            final JAXBContext jc = JAXBContext.newInstance(SettingsWrapper.class);
            final Unmarshaller unmarshaller = jc.createUnmarshaller();
            unmarshaller.setProperty(UnmarshallerProperties.MEDIA_TYPE, "application/json");
            unmarshaller.setProperty(UnmarshallerProperties.JSON_INCLUDE_ROOT, true);

            FileInputStream fis = null;
            try {
                fis = new FileInputStream(sFile);
                final StreamSource json = new StreamSource(fis);
                setSettings(unmarshaller.unmarshal(json, SettingsWrapper.class).getValue());
            } catch (FileNotFoundException e) {
                LOGGER.error("No settings file (" + sFile.getAbsolutePath() + ") found.");
            } finally {
                if (fis != null) {
                    fis.close();
                }
            }
        }

    }

    private void saveSettings(final SettingsWrapper settings) throws JAXBException {
        final JAXBContext jc = JAXBContext.newInstance(SettingsWrapper.class);
        final Marshaller marshaller = jc.createMarshaller();
        marshaller.setProperty(MarshallerProperties.MEDIA_TYPE, "application/json");
        marshaller.setProperty(MarshallerProperties.JSON_INCLUDE_ROOT, true);
        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);

        final File sFile = ConfigurationDir.getConfigFile("settings.json");
        marshaller.marshal(settings, sFile);
    }
}
