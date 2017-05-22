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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.Properties;

import org.apache.logging.log4j.LogManager;

/**
 * A InputStream from (preferably) property files. All available InputStreams
 * are combined in this order:
 * <ol>
 * <li>application modules</li>
 * <li>installation specific files</li>
 * </ol>
 * 
 * @author Ren\u00E9 Adler (eagle)
 */
public class ConfigurationInputStream extends InputStream {

    /** The Constant CONFIG_PROPERTIES. */
    private static final String CONFIG_PROPERTIES = "config.properties";

    /** The Constant lbr. */
    private static final byte[] lbr = System.getProperty("line.separator").getBytes(StandardCharsets.ISO_8859_1);

    /** The in. */
    protected InputStream in;

    /** The e. */
    private Enumeration<? extends InputStream> e;

    /** The empty. */
    private boolean empty;

    /**
     * Combined Stream of all config files named <code>filename</code>
     * available.
     *
     * @param filename
     *            , e.g. mycore.properties or messages_de.properties
     * @throws IOException
     *             Signals that an I/O exception has occurred.
     */
    public ConfigurationInputStream(String filename) throws IOException {
        this(filename, null);
    }

    /**
     * Instantiates a new configuration input stream.
     *
     * @param filename
     *            the filename
     * @param initStream
     *            the init stream
     * @throws IOException
     *             Signals that an I/O exception has occurred.
     */
    private ConfigurationInputStream(String filename, InputStream initStream) throws IOException {
        super();
        this.empty = true;
        this.e = getInputStreams(filename, initStream);
        if (e.hasMoreElements()) {
            nextStream();
        }
    }

    /**
     * {@link InputStream} that includes all properties from
     * <strong>config.properties</strong>. Use system property
     *
     * @return the config properties instance
     * @throws IOException
     *             Signals that an I/O exception has occurred.
     */
    public static ConfigurationInputStream getConfigPropertiesInstance() throws IOException {
        File configurationDirectory = ConfigurationDir.getConfigurationDirectory();
        InputStream initStream = null;
        if (configurationDirectory != null) {
            LogManager.getLogger().info("Current configuration directory: " + configurationDirectory.getAbsolutePath());
            if (configurationDirectory.isDirectory()) {
                initStream = getBaseDirInputStream(configurationDirectory);
            }
        }
        return new ConfigurationInputStream(CONFIG_PROPERTIES, initStream);
    }

    /**
     * Checks if is empty.
     *
     * @return true, if is empty
     */
    public boolean isEmpty() {
        return empty;
    }

    /**
     * Gets the input streams.
     *
     * @param filename
     *            the filename
     * @param initStream
     *            the init stream
     * @return the input streams
     * @throws IOException
     *             Signals that an I/O exception has occurred.
     */
    private Enumeration<? extends InputStream> getInputStreams(String filename, InputStream initStream)
        throws IOException {
        LinkedList<InputStream> cList = new LinkedList<>();
        if (initStream != null) {
            empty = false;
            cList.add(initStream);
        }
        InputStream propertyStream = getPropertyStream(filename);
        if (propertyStream != null) {
            empty = false;
            cList.add(propertyStream);
            cList.add(new ByteArrayInputStream(lbr));
        }
        File localProperties = ConfigurationDir.getConfigFile(filename);
        if (localProperties != null && localProperties.canRead()) {
            empty = false;
            LogManager.getLogger().info("Loading additional properties from " + localProperties.getAbsolutePath());
            cList.add(new FileInputStream(localProperties));
            cList.add(new ByteArrayInputStream(lbr));
        }
        return Collections.enumeration(cList);
    }

    /**
     * Gets the base dir input stream.
     *
     * @param configurationDirectory
     *            the configuration directory
     * @return the base dir input stream
     * @throws IOException
     *             Signals that an I/O exception has occurred.
     */
    private static ByteArrayInputStream getBaseDirInputStream(File configurationDirectory) throws IOException {
        Properties dataProp = new Properties();
        // On Windows we require forward slashes
        dataProp.setProperty("APP.basedir", configurationDirectory.getAbsolutePath().replace('\\', '/'));
        ByteArrayOutputStream out = new ByteArrayOutputStream(1024);
        dataProp.store(out, null);
        ByteArrayInputStream inputStream = new ByteArrayInputStream(out.toByteArray());
        return inputStream;
    }

    /**
     * Gets the property stream.
     *
     * @param filename
     *            the filename
     * @return the property stream
     * @throws IOException
     *             Signals that an I/O exception has occurred.
     */
    private static InputStream getPropertyStream(String filename) throws IOException {
        File configProperties = new File(filename);
        InputStream input = null;
        if (configProperties.canRead()) {
            input = new FileInputStream(configProperties);
        } else {
            URL url = ConfigurationInputStream.class.getClassLoader().getResource(filename);
            if (url != null) {
                input = url.openStream();
            }
        }
        return input;
    }

    /**
     * Continues reading in the next stream if an EOF is reached.
     *
     * @throws IOException
     *             Signals that an I/O exception has occurred.
     */
    protected final void nextStream() throws IOException {
        if (in != null) {
            in.close();
        }

        if (e.hasMoreElements()) {
            in = e.nextElement();
            if (in == null) {
                throw new NullPointerException();
            }
        } else {
            in = null;
        }

    }

    /*
     * (non-Javadoc)
     * 
     * @see java.io.InputStream#available()
     */
    @Override
    public int available() throws IOException {
        if (in == null) {
            return 0; // no way to signal EOF from available()
        }
        return in.available();
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.io.InputStream#read()
     */
    @Override
    public int read() throws IOException {
        if (in == null) {
            return -1;
        }
        int c = in.read();
        if (c == -1) {
            nextStream();
            return read();
        }
        return c;
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.io.InputStream#read(byte[], int, int)
     */
    @Override
    public int read(byte b[], int off, int len) throws IOException {
        if (in == null) {
            return -1;
        }

        int n = in.read(b, off, len);
        if (n <= 0) {
            nextStream();
            return read(b, off, len);
        }
        return n;
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.io.InputStream#close()
     */
    @Override
    public void close() throws IOException {
        do {
            nextStream();
        } while (in != null);
    }

}
