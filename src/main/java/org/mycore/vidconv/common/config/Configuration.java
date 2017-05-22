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
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeSet;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.vidconv.common.AppPropertiesResolver;

import com.google.common.base.Predicates;
import com.google.common.collect.Maps;

/**
 * The Class Configuration.
 *
 * @author Ren\u00E9 Adler (eagle)
 */
public class Configuration {

    /** The single instance of this class that will be used at runtime. */
    private static Configuration singleton;

    /** The instance holder. */
    private Hashtable<SingletonKey, Object> instanceHolder = new Hashtable<SingletonKey, Object>();

    /** The Constant PROPERTY_SPLITTER. */
    protected static final Pattern PROPERTY_SPLITTER = Pattern.compile(",");

    /**
     * The properties instance that stores the values that have been read from
     * every configuration file. These properties are unresolved
     */
    protected AppProperties baseProperties;

    /**
     * The same as baseProperties but all %properties% are resolved.
     */
    protected AppProperties resolvedProperties;

    static {
        createSingleton();
    }

    /**
     * Returns the single instance of this class that can be used to read and
     * manage the configuration properties.
     * 
     * @return the single instance of <CODE>Configuration</CODE> to be used
     */
    public static Configuration instance() {
        return singleton;
    }

    /**
     * Use this method as a default value for {@link #getStrings(String, List)}.
     * 
     * @return an empty list of Strings
     * @see Collections#emptyList()
     */
    public static List<String> emptyList() {
        return Collections.emptyList();
    }

    /**
     * Instantiates the singleton by calling the protected constructor.
     */
    protected static void createSingleton() {
        try {
            singleton = new Configuration();
        } catch (IOException e) {
            throw new ConfigurationException("Could not instantiate Configuration.", e);
        }
    }

    /**
     * return the given properties sorted by keys.
     *
     * @param props
     *            - properties to be sorted if props is null - an empty
     *            properties object that supports sorting by key will be created
     * @return a new properties object sorted by keys
     */
    public static AppProperties sortProperties(AppProperties props) {
        AppProperties sortedProps = new AppProperties() {
            private static final long serialVersionUID = 1L;

            @Override
            public synchronized Enumeration<Object> keys() {
                return Collections.enumeration(new TreeSet<Object>(super.keySet()));
            }
        };
        if (props != null) {
            sortedProps.putAll(props);
        }
        return sortedProps;
    }

    /**
     * Protected constructor to create the singleton instance.
     *
     * @throws IOException
     *             Signals that an I/O exception has occurred.
     */
    protected Configuration() throws IOException {
        baseProperties = new AppProperties();
        resolvedProperties = new AppProperties();
    }

    /**
     * Debug.
     */
    private void debug() {
        AppProperties tmp = null;
        String comments = "Active config properties";
        File resolvedPropertiesFile = ConfigurationDir.getConfigFile("config.resolved.properties");
        if (resolvedPropertiesFile != null) {
            tmp = Configuration.sortProperties(getResolvedProperties());
            try (FileOutputStream fout = new FileOutputStream(resolvedPropertiesFile)) {
                tmp.store(fout, comments + "\nDo NOT edit this file!");
            } catch (IOException e) {
                LogManager.getLogger()
                    .warn("Could not store resolved properties to " + resolvedPropertiesFile.getAbsolutePath(), e);
            }
        }

        Logger logger = LogManager.getLogger();
        if (logger.isDebugEnabled()) {
            try (StringWriter sw = new StringWriter(); PrintWriter out = new PrintWriter(sw)) {
                tmp = tmp == null ? Configuration.sortProperties(getResolvedProperties()) : tmp;
                tmp.store(out, comments);
                out.flush();
                sw.flush();
                logger.debug(sw.toString());
            } catch (IOException e) {
                logger.debug("Error while debugging config properties.", e);
            }
        }
    }

    /**
     * Substitute all %properties%.
     */
    protected synchronized void resolveProperties() {
        AppProperties tmpProperties = AppProperties.copy(getBaseProperties());
        AppPropertiesResolver resolver = new AppPropertiesResolver(tmpProperties);
        resolvedProperties = AppProperties.copy(resolver.resolveAll(tmpProperties));
    }

    /**
     * Gets the resolved properties.
     *
     * @return the resolved properties
     */
    private AppProperties getResolvedProperties() {
        return resolvedProperties;
    }

    /**
     * Gets the base properties.
     *
     * @return the base properties
     */
    private AppProperties getBaseProperties() {
        return baseProperties;
    }

    /**
     * Gets the properties map.
     *
     * @return the properties map
     */
    public Map<String, String> getPropertiesMap() {
        return Collections.unmodifiableMap(getResolvedProperties().getAsMap());
    }

    /**
     * Returns all the properties beginning with the specified string.
     *
     * @param startsWith
     *            the string all the returned properties start with
     * @return the list of properties
     */
    public Map<String, String> getPropertiesMap(final String startsWith) {
        return getPropertiesMap().entrySet().stream().filter(p -> p.getKey().startsWith(startsWith))
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    /**
     * Returns a new instance of the class specified in the configuration
     * property with the given name.
     *
     * @param <T>
     *            the generic type
     * @param name
     *            the non-null and non-empty qualified name of the configuration
     *            property
     * @param defaultname
     *            the qualified class name
     * @return Instance of the value of the configuration property
     * @throws ConfigurationException
     *             if the property is not set or the class can not be loaded or
     *             instantiated
     */
    public <T> T getInstanceOf(String name, String defaultname) throws ConfigurationException {
        String classname = getString(name, defaultname);
        if (classname == null) {
            throw new ConfigurationException("Configuration property missing: " + name);
        }

        return this.<T> loadClass(classname);
    }

    /**
     * Load class.
     *
     * @param <T>
     *            the generic type
     * @param classname
     *            the classname
     * @return the t
     * @throws ConfigurationException
     *             the configuration exception
     */
    protected <T> T loadClass(String classname) throws ConfigurationException {
        LogManager.getLogger().debug("Loading Class: " + classname);

        T o = null;
        Class<? extends T> cl;
        try {
            @SuppressWarnings("unchecked")
            Class<? extends T> forName = (Class<? extends T>) Class.forName(classname);
            cl = forName;
        } catch (ClassNotFoundException ex) {
            throw new ConfigurationException("Could not load class " + classname, ex);
        }

        try {
            try {
                o = cl.newInstance();
            } catch (IllegalAccessException | InstantiationException e) {
                // check for singleton
                Method[] querymethods = cl.getMethods();

                for (Method querymethod : querymethods) {
                    if (querymethod.getName().toLowerCase(Locale.ROOT).equals("instance")
                        || querymethod.getName().toLowerCase(Locale.ROOT).equals("getinstance")) {
                        Object[] ob = new Object[0];
                        @SuppressWarnings("unchecked")
                        T invoke = (T) querymethod.invoke(cl, ob);
                        o = invoke;
                        break;
                    }
                }
                if (o == null) {
                    throw e;
                }
            }
        } catch (ExceptionInInitializerError e) {
            String msg = "Could not instantiate class " + classname;
            throw new ConfigurationException(msg, e.getException());
        } catch (Throwable t) {
            String msg = "Could not instantiate class " + classname;
            throw new ConfigurationException(msg, t);
        }
        return o;
    }

    /**
     * Returns a new instance of the class specified in the configuration
     * property with the given name.
     *
     * @param <T>
     *            the generic type
     * @param name
     *            the non-null and non-empty qualified name of the configuration
     *            property
     * @param defaultObj
     *            the default object;
     * @return Instance of the value of the configuration property
     * @throws ConfigurationException
     *             if the property is not set or the class can not be loaded or
     *             instantiated
     */
    public <T> T getInstanceOf(String name, T defaultObj) {
        String classname = getString(name, null);
        if (classname == null) {
            return defaultObj;
        }

        return this.<T> loadClass(classname);
    }

    /**
     * Returns a new instance of the class specified in the configuration
     * property with the given name.
     *
     * @param <T>
     *            the generic type
     * @param name
     *            the non-null and non-empty name of the configuration property
     * @return the value of the configuration property as a String, or null
     * @throws ConfigurationException
     *             if the property is not set or the class can not be loaded or
     *             instantiated
     */
    public <T> T getInstanceOf(String name) throws ConfigurationException {
        return getInstanceOf(name, (String) null);
    }

    /**
     * Returns a instance of the class specified in the configuration property
     * with the given name. If the class was previously instantiated by this
     * method this instance is returned.
     *
     * @param <T>
     *            the generic type
     * @param name
     *            the non-null and non-empty name of the configuration property
     * @param defaultname
     *            the defaultname
     * @return the instance of the class named by the value of the configuration
     *         property
     * @throws ConfigurationException
     *             if the property is not set or the class can not be loaded or
     *             instantiated
     */
    public <T> T getSingleInstanceOf(String name, String defaultname) throws ConfigurationException {
        String className = defaultname == null ? getString(name) : getString(name, defaultname);
        SingletonKey key = new SingletonKey(name, className);
        @SuppressWarnings("unchecked")
        T inst = (T) instanceHolder.get(key);
        if (inst != null) {
            return inst;
        }
        inst = this.<T> getInstanceOf(name, defaultname); // we need a new
                                                          // instance, get it
        instanceHolder.put(key, inst); // save the instance in the hashtable
        return inst;
    }

    /**
     * Returns a instance of the class specified in the configuration property
     * with the given name. If the class was prevously instantiated by this
     * method this instance is returned.
     *
     * @param <T>
     *            the generic type
     * @param name
     *            non-null and non-empty name of the configuration property
     * @return the instance of the class named by the value of the configuration
     *         property
     * @throws ConfigurationException
     *             if the property is not set or the class can not be loaded or
     *             instantiated
     */
    public <T> T getSingleInstanceOf(String name) {
        return getSingleInstanceOf(name, (String) null);
    }

    /**
     * Returns the configuration property with the specified name as a String.
     * 
     * @param name
     *            the non-null and non-empty name of the configuration property
     * @return the value of the configuration property as a String
     * @throws ConfigurationException
     *             if the property with this name is not set
     */
    public String getString(String name) {
        String value = getString(name, null);

        if (value == null) {
            throw new ConfigurationException("Configuration property " + name + " is not set");
        }

        return value.trim();
    }

    /**
     * Returns the configuration property with the specified name as a list of
     * strings. Values should be delimited by ','
     * 
     * @param name
     *            the non-null and non-empty name of the configuration property
     * @return the value of the configuration property as a unmodifiable list of
     *         strings.
     * @throws ConfigurationException
     *             if the property with this name is not set
     */
    public List<String> getStrings(String name) {
        String value = getString(name);
        return splitString(value);
    }

    /**
     * Split string.
     *
     * @param value
     *            the value
     * @return the list
     */
    private List<String> splitString(String value) {
        return PROPERTY_SPLITTER.splitAsStream(value).map(String::trim).filter(s -> !s.isEmpty())
            .collect(Collectors.toList());
    }

    /**
     * Returns the configuration property with the specified name as a list of
     * strings. Values should be delimited by ','
     * 
     * @param name
     *            the non-null and non-empty name of the configuration property
     * @param defaultValue
     *            the value to return if the configuration property is not set
     * @return the value of the configuration property as a unmodifiable list of
     *         strings or <code>defaultValue</code>.
     */
    public List<String> getStrings(String name, List<String> defaultValue) {
        String value = getString(name, null);
        return value == null ? defaultValue : splitString(value);
    }

    /**
     * Returns the configuration property with the specified name as a String,
     * or returns a given default value if the property is not set.
     * 
     * @param name
     *            the non-null and non-empty name of the configuration property
     * @param defaultValue
     *            the value to return if the configuration property is not set
     * @return the value of the configuration property as a String
     */
    public String getString(String name, String defaultValue) {
        if (getBaseProperties().isEmpty()) {
            throw new ConfigurationException("Configuration is still not initialized");
        }
        return getResolvedProperties().getProperty(name, defaultValue);
    }

    /**
     * Returns the configuration property with the specified name as an <CODE>
     * int</CODE> value.
     * 
     * @param name
     *            the non-null and non-empty name of the configuration property
     * @return the value of the configuration property as an <CODE>int</CODE>
     *         value
     * @throws NumberFormatException
     *             if the configuration property is not an <CODE>int</CODE>
     *             value
     * @throws ConfigurationException
     *             if the property with this name is not set
     */
    public int getInt(String name) throws NumberFormatException {
        return Integer.parseInt(getString(name));
    }

    /**
     * Returns the configuration property with the specified name as an <CODE>
     * int</CODE> value, or returns a given default value if the property is not
     * set.
     * 
     * @param name
     *            the non-null and non-empty name of the configuration property
     *            /** Returns the configuration property with the specified name
     *            as an <CODE>int</CODE> value, or returns a given default value
     *            if the property is not set.
     * @param defaultValue
     *            the value to return if the configuration property is not set
     * @return the value of the specified property as an <CODE>int</CODE> value
     * @throws NumberFormatException
     *             if the configuration property is set but is not an <CODE>int
     *             </CODE> value
     */
    public int getInt(String name, int defaultValue) throws NumberFormatException {
        String value = getString(name, null);

        return value == null ? defaultValue : Integer.parseInt(value);
    }

    /**
     * Returns the configuration property with the specified name as a <CODE>
     * long</CODE> value.
     * 
     * @param name
     *            the non-null and non-empty name of the configuration property
     * @return the value of the configuration property as a <CODE>long</CODE>
     *         value
     * @throws NumberFormatException
     *             if the configuration property is not a <CODE>long</CODE>
     *             value
     * @throws ConfigurationException
     *             if the property with this name is not set
     */
    public long getLong(String name) throws NumberFormatException {
        return Long.parseLong(getString(name));
    }

    /**
     * Returns the configuration property with the specified name as a <CODE>
     * long</CODE> value, or returns a given default value if the property is
     * not set.
     *
     * @param name
     *            the non-null and non-empty name of the configuration property
     * @param defaultValue
     *            the value to return if the configuration property is not set
     * @return the value of the specified property as a <CODE>long</CODE> value
     * @throws NumberFormatException
     *             if the configuration property is set but is not a <CODE>long
     *             </CODE> value
     */
    public long getLong(String name, long defaultValue) throws NumberFormatException {
        String value = getString(name, null);

        return value == null ? defaultValue : Long.parseLong(value);
    }

    /**
     * Returns the configuration property with the specified name as a <CODE>
     * float</CODE> value.
     * 
     * @param name
     *            the non-null and non-empty name of the configuration property
     * @return the value of the configuration property as a <CODE>float</CODE>
     *         value
     * @throws NumberFormatException
     *             if the configuration property is not a <CODE>float</CODE>
     *             value
     * @throws ConfigurationException
     *             if the property with this name is not set
     */
    public float getFloat(String name) throws NumberFormatException {
        return Float.parseFloat(getString(name));
    }

    /**
     * Returns the configuration property with the specified name as a <CODE>
     * float</CODE> value, or returns a given default value if the property is
     * not set.
     *
     * @param name
     *            the non-null and non-empty name of the configuration property
     * @param defaultValue
     *            the value to return if the configuration property is not set
     * @return the value of the specified property as a <CODE>float</CODE> value
     * @throws NumberFormatException
     *             if the configuration property is set but is not a <CODE>
     *             float</CODE> value
     */
    public float getFloat(String name, float defaultValue) throws NumberFormatException {
        String value = getString(name, null);

        return value == null ? defaultValue : Float.parseFloat(value);
    }

    /**
     * Returns the configuration property with the specified name as a <CODE>
     * double</CODE> value.
     * 
     * @param name
     *            the non-null and non-empty name of the configuration property
     * @return the value of the configuration property as a <CODE>double
     *         </CODE> value
     * @throws NumberFormatException
     *             if the configuration property is not a <CODE>double</CODE>
     *             value
     * @throws ConfigurationException
     *             if the property with this name is not set
     */
    public double getDouble(String name) throws NumberFormatException {
        return Double.parseDouble(getString(name));
    }

    /**
     * Returns the configuration property with the specified name as a <CODE>
     * double</CODE> value, or returns a given default value if the property is
     * not set.
     *
     * @param name
     *            the non-null and non-empty name of the configuration property
     * @param defaultValue
     *            the value to return if the configuration property is not set
     * @return the value of the specified property as a <CODE>double</CODE>
     *         value
     * @throws NumberFormatException
     *             if the configuration property is set but is not a <CODE>
     *             double</CODE> value
     */
    public double getDouble(String name, double defaultValue) throws NumberFormatException {
        String value = getString(name, null);

        return value == null ? defaultValue : Double.parseDouble(value);
    }

    /**
     * Returns the configuration property with the specified name as a <CODE>
     * boolean</CODE> value.
     * 
     * @param name
     *            the non-null and non-empty name of the configuration property
     * @return <CODE>true</CODE>, if and only if the specified property has the
     *         value <CODE>true</CODE>
     * @throws ConfigurationException
     *             if the property with this name is not set
     */
    public boolean getBoolean(String name) {
        String value = getString(name);

        return "true".equals(value.trim());
    }

    /**
     * Returns the configuration property with the specified name as a <CODE>
     * boolean</CODE> value, or returns a given default value if the property is
     * not set. If the property is set and its value is not <CODE>true
     * </CODE>, then <code>false</code> is returned.
     *
     * @param name
     *            the non-null and non-empty name of the configuration property
     * @param defaultValue
     *            the value to return if the configuration property is not set
     * @return the value of the specified property as a <CODE>boolean</CODE>
     *         value
     */
    public boolean getBoolean(String name, boolean defaultValue) {
        String value = getString(name, null);

        return value == null ? defaultValue : "true".equals(value.trim());
    }

    /**
     * Sets the configuration property with the specified name to a new <CODE>
     * String</CODE> value. If the parameter <CODE>value</CODE> is <CODE>
     * null</CODE>, the property will be deleted.
     * 
     * @param name
     *            the non-null and non-empty name of the configuration property
     * @param value
     *            the new value of the configuration property, possibly <CODE>
     *            null</CODE>
     */
    public void set(String name, String value) {
        if (value == null) {
            getBaseProperties().remove(name);
        } else {
            getBaseProperties().setProperty(name, value);
        }
        resolveProperties();
    }

    /**
     * Initialize.
     *
     * @param props
     *            the props
     * @param clear
     *            the clear
     */
    public synchronized void initialize(Map<String, String> props, boolean clear) {
        HashMap<String, String> copy = new HashMap<>(props);
        copy.remove(null);
        if (clear) {
            getBaseProperties().clear();
        } else {
            Map<String, String> nullValues = Maps.filterValues(copy, Predicates.<String> isNull());
            for (String key : nullValues.keySet()) {
                getBaseProperties().remove(key);
            }
        }
        Map<String, String> notNullValues = Maps.filterValues(copy, Predicates.notNull());
        for (Entry<String, String> entry : notNullValues.entrySet()) {
            getBaseProperties().setProperty(entry.getKey(), entry.getValue());
        }
        resolveProperties();
        debug();
    }

    /**
     * Sets the configuration property with the specified name to a new <CODE>
     * int</CODE> value.
     * 
     * @param name
     *            the non-null and non-empty name of the configuration property
     * @param value
     *            the new value of the configuration property
     */
    public void set(String name, int value) {
        set(name, String.valueOf(value));
    }

    /**
     * Sets the configuration property with the specified name to a new <CODE>
     * long</CODE> value.
     * 
     * @param name
     *            the non-null and non-empty name of the configuration property
     * @param value
     *            the new value of the configuration property
     */
    public void set(String name, long value) {
        set(name, String.valueOf(value));
    }

    /**
     * Sets the configuration property with the specified name to a new <CODE>
     * float</CODE> value.
     * 
     * @param name
     *            the non-null and non-empty name of the configuration property
     * @param value
     *            the new value of the configuration property
     */
    public void set(String name, float value) {
        set(name, String.valueOf(value));
    }

    /**
     * Sets the configuration property with the specified name to a new <CODE>
     * double</CODE> value.
     * 
     * @param name
     *            the non-null and non-empty name of the configuration property
     * @param value
     *            the new value of the configuration property
     */
    public void set(String name, double value) {
        set(name, String.valueOf(value));
    }

    /**
     * Sets the configuration property with the specified name to a new <CODE>
     * boolean</CODE> value.
     * 
     * @param name
     *            the non-null and non-empty name of the configuration property
     * @param value
     *            the new value of the configuration property
     */
    public void set(String name, boolean value) {
        set(name, String.valueOf(value));
    }

    /**
     * Lists all configuration properties currently set to a PrintStream. Useful
     * for debugging, e. g. by calling
     * <P>
     * <CODE>Configuration.instance().list( System.out );</CODE>
     * </P>
     *
     * @param out
     *            the PrintStream to list the configuration properties on
     * @see java.util.Properties#list( PrintStream )
     */
    public void list(PrintStream out) {
        getResolvedProperties().list(out);
    }

    /**
     * Lists all configuration properties currently set to a PrintWriter. Useful
     * for debugging.
     *
     * @param out
     *            the PrintWriter to list the configuration properties on
     * @see java.util.Properties#list( PrintWriter )
     */
    public void list(PrintWriter out) {
        getResolvedProperties().list(out);
    }

    /**
     * Stores all configuration properties currently set to an OutputStream.
     *
     * @param out
     *            the OutputStream to write the configuration properties to
     * @param header
     *            the header to prepend before writing the list of properties
     * @throws IOException
     *             if writing to the OutputStream throws an <CODE>IOException
     *             </CODE>
     * @see java.util.Properties#store
     */
    public void store(OutputStream out, String header) throws IOException {
        getResolvedProperties().store(out, header);
    }

    /**
     * Returns a String containing the configuration properties currently set.
     * Useful for debugging, e. g. by calling
     * <P>
     * <CODE>System.out.println( Configuration.instance() );</CODE>
     * </P>
     * 
     * @return a String containing the configuration properties currently set
     */
    @Override
    public String toString() {
        return getResolvedProperties().toString();
    }

    /**
     * The Class SingletonKey.
     */
    private static class SingletonKey {

        /** The class name. */
        private String property, className;

        /**
         * Instantiates a new singleton key.
         *
         * @param property
         *            the property
         * @param className
         *            the class name
         */
        public SingletonKey(String property, String className) {
            super();
            this.property = property;
            this.className = className;
        }

        /*
         * (non-Javadoc)
         * 
         * @see java.lang.Object#hashCode()
         */
        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((className == null) ? 0 : className.hashCode());
            result = prime * result + ((property == null) ? 0 : property.hashCode());
            return result;
        }

        /*
         * (non-Javadoc)
         * 
         * @see java.lang.Object#equals(java.lang.Object)
         */
        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            SingletonKey other = (SingletonKey) obj;
            if (className == null) {
                if (other.className != null) {
                    return false;
                }
            } else if (!className.equals(other.className)) {
                return false;
            }
            if (property == null) {
                if (other.property != null) {
                    return false;
                }
            } else if (!property.equals(other.property)) {
                return false;
            }
            return true;
        }
    }
}
