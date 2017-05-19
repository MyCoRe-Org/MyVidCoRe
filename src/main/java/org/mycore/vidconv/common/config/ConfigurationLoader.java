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
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Map;
import java.util.StringTokenizer;

import org.apache.commons.io.input.TeeInputStream;

/**
 * @author Ren\u00E9 Adler (eagle)
 *
 */
public class ConfigurationLoader {

	protected AppProperties properties;

	public ConfigurationLoader() {
		this.properties = new AppProperties();
		try (InputStream in = getConfigInputStream()) {
			loadFromStream(in);
		} catch (IOException e) {
			throw new ConfigurationException("Could not load config properties.", e);
		}
	}

	private InputStream getConfigInputStream() throws IOException {
		ConfigurationInputStream configurationInputStream = ConfigurationInputStream.getConfigPropertiesInstance();
		File configFile = ConfigurationDir.getConfigFile("config.active.properties");
		if (configFile != null) {
			FileOutputStream fout = new FileOutputStream(configFile);
			TeeInputStream tin = new TeeInputStream(configurationInputStream, fout, true);
			return tin;
		}
		return configurationInputStream;
	}

	public Map<String, String> load() {
		return properties.getAsMap();
	}

	/**
	 * Loads configuration properties from a specified properties file and adds
	 * them to the properties currently set. This method scans the <CODE>
	 * CLASSPATH</CODE> for the properties file, it may be a plain file, but may
	 * also be located in a zip or jar file. If the properties file contains a
	 * property called <CODE>Configuration.Include</CODE>, the files specified
	 * in that property will also be read. Multiple include files have to be
	 * separated by spaces or colons.
	 * 
	 * @param filename
	 *            the properties file to be loaded
	 * @throws ConfigurationException
	 *             if the file can not be loaded
	 */
	private void loadFromFile(String filename) {
		File configProperties = new File(filename);
		InputStream input = null;
		try {
			if (configProperties.canRead()) {
				input = new FileInputStream(configProperties);
			} else {
				URL url = this.getClass().getResource("/" + filename);
				if (url == null) {
					throw new ConfigurationException("Could not find file or resource:" + filename);
				}
				input = url.openStream();
			}
			loadFromStream(input);
		} catch (IOException e) {
			throw new ConfigurationException("Could not load configuration from: " + filename, e);
		}
	}

	private void loadFromStream(InputStream input) throws IOException {
		try (InputStream in = input) {
			properties.load(in);
		}
		String include = properties.getProperty("Configuration.Include", null);

		if (include != null) {
			StringTokenizer st = new StringTokenizer(include, ", ");
			properties.remove("Configuration.Include");
			while (st.hasMoreTokens()) {
				loadFromFile(st.nextToken());
			}
		}
	}
}
