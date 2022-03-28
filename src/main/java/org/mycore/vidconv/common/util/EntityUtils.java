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
package org.mycore.vidconv.common.util;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.reflections.Reflections;
import org.reflections.scanners.Scanners;

import jakarta.xml.bind.annotation.XmlRootElement;

/**
 * @author Ren\u00E9 Adler (eagle)
 *
 */
public class EntityUtils {

    private static final List<Class<?>> CACHED_ENTITIES = Collections.synchronizedList(new ArrayList<>());

    private static Predicate<Class<?>> classPackageMatches(String pkg) {
        return cls -> {
            String cn = cls.getName();
            return cn.substring(0, cn.lastIndexOf(".")).equals(pkg);
        };
    }

    public static List<Class<?>> populateEntities(String pkg) throws IOException {
        return populateEntities(Arrays.asList(pkg));
    }

    public static List<Class<?>> populateEntities(List<String> pkgs) throws IOException {
        synchronized (CACHED_ENTITIES) {
            if (CACHED_ENTITIES.isEmpty() || pkgs.stream().anyMatch(
                    p -> CACHED_ENTITIES.stream().filter(classPackageMatches(p)).count() == 0)) {
                CACHED_ENTITIES.addAll(
                        pkgs.stream()
                                .map(pkg -> new Reflections(pkg, Scanners.TypesAnnotated)
                                        .getTypesAnnotatedWith(XmlRootElement.class, true))
                                .flatMap(ts -> ts.stream()).distinct().collect(Collectors.toList()));
            }
            return CACHED_ENTITIES;
        }
    }

}
