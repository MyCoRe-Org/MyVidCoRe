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
package org.mycore.vidconv.common;

import java.io.File;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.InaccessibleObjectException;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * @author Ren\u00E9 Adler (eagle)
 *
 */
public class ClassTools {

    private static final Logger LOGGER = LogManager.getLogger();

    private static volatile ClassLoader extendedClassLoader;

    static {
        updateClassLoader(); // first init
    }

    public static <T> T newInstance(Class<?> cls) {
        return newInstance(cls, true);
    }

    @SuppressWarnings("unchecked")
    public static <T> T newInstance(Class<?> cls, boolean allowSingleton) {
        return (T) Arrays.stream(cls.getDeclaredConstructors()).filter(c -> c.getParameterCount() == 0).findFirst()
                .map(c -> {
                    try {
                        return trySetAccessible(c) ? (T) c.newInstance() : null;
                    } catch (InstantiationException | IllegalAccessException | IllegalArgumentException
                            | InvocationTargetException e) {
                        if (allowSingleton) {
                            return null;
                        }
                        throw new UnsupportedOperationException(e.getMessage(), e);
                    }
                }).map(t -> Optional.ofNullable(t).orElseGet(() -> newInstanceBySingeltonMethod(cls)))
                .orElseThrow(() -> new UnsupportedOperationException(
                        "Empty argument constructor needed on class " + cls + "."));
    }

    @SuppressWarnings("unchecked")
    private static <T> T newInstanceBySingeltonMethod(Class<?> cls) {
        return Arrays.stream(cls.getMethods())
                .filter(m -> Stream.of("instance", "getinstance").anyMatch(mm -> mm.equalsIgnoreCase((m.getName()))))
                .findFirst().map(m -> {
                    try {
                        return trySetAccessible(m) ? (T) m.invoke(cls, new Object[0]) : null;
                    } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
                        throw new UnsupportedOperationException(e.getMessage(), e);
                    }
                }).orElseThrow(() -> new UnsupportedOperationException(
                        "Empty argument singelton method needed on class " + cls + "."));
    }

    public static boolean trySetAccessible(AccessibleObject obj) {
        try {
            Objects.requireNonNull(obj).setAccessible(true);
            return true;
        } catch (InaccessibleObjectException | SecurityException e) {
            LOGGER.warn("Couldn't set accessible flag for class {} because of {}.", obj.getClass(), e.getMessage());
            return false;
        }
    }

    public static Object loadClassFromURL(String classPath, String className)
            throws MalformedURLException, ReflectiveOperationException {
        return loadClassFromURL(new File(classPath), className);
    }

    public static Object loadClassFromURL(File file, String className)
            throws MalformedURLException, ReflectiveOperationException {
        if (file.exists()) {
            URL url = file.toURI().toURL();
            @SuppressWarnings("resource")
            URLClassLoader urlClassLoader = new URLClassLoader(new URL[] { url },
                    Thread.currentThread().getContextClassLoader());
            Class<?> clazz = urlClassLoader.loadClass(className);
            return clazz.getDeclaredConstructor().newInstance();
        }

        return null;
    }

    /**
     * Loads a class via default ClassLoader or
     * <code>Thread.currentThread().getContextClassLoader()</code>.
     * 
     * @param classname
     *            Name of class
     * @param <T>
     *            Type of Class
     * @return the initialized class
     * @throws ClassNotFoundException
     *             if both ClassLoader cannot load the Class
     */
    @SuppressWarnings("unchecked")
    public static <T> Class<? extends T> forName(String classname) throws ClassNotFoundException {
        Class<? extends T> forName;
        try {
            forName = (Class<? extends T>) Class.forName(classname);
        } catch (ClassNotFoundException cnfe) {
            forName = (Class<? extends T>) Class.forName(classname, true, extendedClassLoader);
        }
        return forName;
    }

    /**
     * @return a ClassLoader that should be used to load resources
     */
    public static ClassLoader getClassLoader() {
        return extendedClassLoader;
    }

    public static void updateClassLoader() {
        extendedClassLoader = Optional.ofNullable(Thread.currentThread().getContextClassLoader())
                .orElseGet(ClassTools.class::getClassLoader);
    }
}
