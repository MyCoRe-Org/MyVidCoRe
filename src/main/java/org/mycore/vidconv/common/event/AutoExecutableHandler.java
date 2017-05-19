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
package org.mycore.vidconv.common.event;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.text.MessageFormat;
import java.time.Instant;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Stream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.vidconv.Application;
import org.mycore.vidconv.common.event.annotation.AutoExecutable;
import org.mycore.vidconv.common.event.annotation.Shutdown;
import org.mycore.vidconv.common.event.annotation.Startup;
import org.reflections.Reflections;

/**
 * @author Ren\u00E9 Adler (eagle)
 *
 */
public class AutoExecutableHandler {

    private static final Logger LOGGER = LogManager.getLogger();

    private static boolean haltOnError = true;

    private static Set<Class<?>> executables;

    static {
        final Reflections reflections = new Reflections(Application.class.getPackage().getName());
        executables = reflections.getTypesAnnotatedWith(AutoExecutable.class);
    }

    /**
     * Register class as {@link AutoExecutable}.
     *
     * @param executable
     *            the class to execute
     */
    public static void register(Class<?> executable) {
        if (executable.isAnnotationPresent(AutoExecutable.class)) {
            executables.add(executable);
        } else {
            LOGGER.warn("Class \"" + executable.getName() + "\" should have the \"AutoExecutable\" annotation.");
        }
    }

    /**
     * @return the haltOnError
     */
    public static boolean isHaltOnError() {
        return haltOnError;
    }

    /**
     * @param haltOnError
     *            the haltOnError to set
     */
    public static void setHaltOnError(boolean haltOnError) {
        AutoExecutableHandler.haltOnError = haltOnError;
    }

    /**
     * Starts registered startup hooks.
     */
    public static void startup() {
        executables.stream()
            .sorted((o1, o2) -> Integer.compare(o2.getAnnotation(AutoExecutable.class).priority(),
                o1.getAnnotation(AutoExecutable.class).priority()))
            .forEachOrdered(autoExecutable -> runExecutables(autoExecutable, Startup.class));
    }

    /**
     * Starts registered shutdown hooks.
     */
    public static void shutdown() {
        executables.stream()
            .sorted((o1, o2) -> Integer.compare(o1.getAnnotation(AutoExecutable.class).priority(),
                o2.getAnnotation(AutoExecutable.class).priority()))
            .forEachOrdered(autoExecutable -> runExecutables(autoExecutable, Shutdown.class));
    }

    private static void runExecutables(Class<?> autoExecutable, Class<? extends Annotation> type) {
        log("Run " + autoExecutable.getAnnotation(AutoExecutable.class).name() + " with priority of "
            + autoExecutable.getAnnotation(AutoExecutable.class).priority());
        try {
            sort(type, Arrays.stream(autoExecutable.getDeclaredMethods()).filter(m -> m.isAnnotationPresent(type)))
                .forEachOrdered(m -> {
                    try {
                        log("...invoke " + m.getName() + "() for " + type.getSimpleName());
                        if (!m.isAccessible()) {
                            m.setAccessible(true);
                        }
                        if (Modifier.isStatic(m.getModifiers())) {
                            m.invoke(null);
                        } else {
                            m.invoke(autoExecutable.newInstance());
                        }
                    } catch (Exception e) {
                        throw new RuntimeException(e.getCause());
                    }
                });
        } catch (RuntimeException e) {
            if (haltOnError) {
                throw e;
            }
            LOGGER.warn(e.getMessage(), e);
        }
    }

    private static Stream<Method> sort(Class<? extends Annotation> type, Stream<Method> methods) {
        if (type.equals(Startup.class)) {
            // reverse ordering: highest priority first
            return methods.sorted((o1, o2) -> Integer.compare(o2.getAnnotation(Startup.class).priority(),
                o1.getAnnotation(Startup.class).priority()));
        } else if (type.equals(Shutdown.class)) {
            return methods.sorted((o1, o2) -> Integer.compare(o1.getAnnotation(Shutdown.class).priority(),
                o2.getAnnotation(Shutdown.class).priority()));
        }

        return methods;
    }

    private static void log(String msg) {
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info(msg);
        } else {
            System.out.println(MessageFormat.format("{0} INFO\t{1}: {2}", Instant.now().toString(),
                AutoExecutableHandler.class.getSimpleName(), msg));
        }
    }
}
