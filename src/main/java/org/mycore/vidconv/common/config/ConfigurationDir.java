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
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Locale;

import org.apache.logging.log4j.LogManager;

/**
 * @author Ren\u00E9 Adler (eagle)
 *
 */
public class ConfigurationDir {

    private static final String APP_NAME = "MyVidCoRe";

    private static String configDir;

    public static void setConfigurationDirectory(String configDir) {
        if (configDir != null) {
            ConfigurationDir.configDir = configDir;
        }
    }

    public static File getConfigurationDirectory() {
        if (configDir != null) {
            return new File(configDir);
        }
        // Windows Vista onwards:
        String localAppData = isWindows() ? System.getenv("LOCALAPPDATA") : null;
        // on every other platform
        String userDir = System.getProperty("user.home");
        String parentDir = localAppData != null ? localAppData : userDir;
        return new File(parentDir, getConfigBaseName());
    }

    private static boolean isWindows() {
        return System.getProperty("os.name").toLowerCase(Locale.ROOT).contains("windows");
    }

    private static String getConfigBaseName() {
        return isWindows() ? APP_NAME : "." + APP_NAME.toLowerCase(Locale.ROOT);
    }

    /**
     * Returns a File object, if {@link #getConfigurationDirectory()} does not
     * return <code>null</code> and directory exists.
     * 
     * @param relativePath
     *            relative path to file or directory with configuration
     *            directory as base.
     * @return null if configuration directory does not exist or is disabled.
     */
    public static File getConfigFile(String relativePath) {
        File configurationDirectory = getConfigurationDirectory();
        if (configurationDirectory == null || !configurationDirectory.isDirectory()) {
            return null;
        }
        return new File(configurationDirectory, relativePath);
    }

    /**
     * Returns URL of a config resource. Same as
     * {@link #getConfigResource(String, ClassLoader)} with second argument
     * <code>null</code>.
     * 
     * @param relativePath
     *            as defined in {@link #getConfigFile(String)}
     */
    public static URL getConfigResource(String relativePath) {
        return getConfigResource(relativePath, null);
    }

    /**
     * Returns URL of a config resource. If {@link #getConfigFile(String)}
     * returns an existing file for "resources"+{relativePath}, its URL is
     * returned. In any other case this method returns the same as
     * {@link ClassLoader#getResource(String)}
     * 
     * @param relativePath
     *            as defined in {@link #getConfigFile(String)}
     * @param classLoader
     *            a classLoader to resolve the resource (see above), null
     *            defaults to this class' class loader
     */
    public static URL getConfigResource(String relativePath, ClassLoader classLoader) {
        File resolvedFile = getConfigFile("resources/" + relativePath);
        if (resolvedFile != null && resolvedFile.exists()) {
            try {
                return resolvedFile.toURI().toURL();
            } catch (MalformedURLException e) {
                LogManager.getLogger(ConfigurationDir.class)
                    .warn("Exception while returning URL for file: " + resolvedFile, e);
            }
        }
        return getClassPathResource(relativePath,
            classLoader == null ? ConfigurationDir.class.getClassLoader() : classLoader);
    }

    private static URL getClassPathResource(String relativePath, ClassLoader classLoader) {
        return classLoader.getResource(relativePath);
    }
}
