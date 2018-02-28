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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.URL;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.stream.StreamSource;

import org.eclipse.persistence.jaxb.MarshallerProperties;
import org.eclipse.persistence.jaxb.UnmarshallerProperties;
import org.mycore.vidconv.common.config.Configuration;

import com.google.common.collect.Streams;

/**
 * @author Ren\u00E9 Adler (eagle)
 *
 */
public class JsonUtils {

    private static final Configuration CONFIG = Configuration.instance();

    static {
        populateEntities(null);
    }

    public static <T> T loadJSON(URL file, Class<T> entityClass) throws JAXBException, IOException {
        return loadJSON(file.openStream(), entityClass, false);
    }

    public static <T> T loadJSON(URL file, Class<T> entityClass, boolean includeRoot)
        throws JAXBException, IOException {
        return loadJSON(file.openStream(), entityClass, includeRoot);
    }

    public static <T> T loadJSON(File file, Class<T> entityClass) throws JAXBException, IOException {
        return loadJSON(file, entityClass, false);
    }

    public static <T> T loadJSON(File file, Class<T> entityClass, boolean includeRoot)
        throws JAXBException, IOException {
        final File sFile = file;

        if (sFile.exists()) {
            FileInputStream fis = null;
            try {
                fis = new FileInputStream(sFile);
                return loadJSON(fis, entityClass, includeRoot);
            } finally {
                if (fis != null) {
                    fis.close();
                }
            }
        }

        return null;
    }

    public static <T> T loadJSON(InputStream is, Class<T> entityClass) throws JAXBException, IOException {
        return loadJSON(is, entityClass, false);
    }

    public static <T> T loadJSON(InputStream is, Class<T> entityClass, boolean includeRoot)
        throws JAXBException, IOException {
        final JAXBContext jc = JAXBContext.newInstance(populateEntities(entityClass));
        final Unmarshaller unmarshaller = jc.createUnmarshaller();
        unmarshaller.setProperty(UnmarshallerProperties.MEDIA_TYPE, "application/json");
        unmarshaller.setProperty(UnmarshallerProperties.JSON_INCLUDE_ROOT, includeRoot);

        final StreamSource json = new StreamSource(is);
        return unmarshaller.unmarshal(json, entityClass).getValue();
    }

    public static <T> void saveJSON(File file, T entity) throws JAXBException, IOException {
        saveJSON(file, entity, false, false);
    }

    public static <T> void saveJSON(File file, T entity, boolean includeRoot, boolean formated)
        throws JAXBException, IOException {
        final File sFile = file;

        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(sFile);
            saveJSON(fos, entity, includeRoot, formated);
        } finally {
            if (fos != null) {
                fos.close();
            }
        }
    }

    public static <T> void saveJSON(OutputStream out, T entity) throws JAXBException, IOException {
        saveJSON(out, entity, false, false);
    }

    public static <T> void saveJSON(OutputStream out, T entity, boolean includeRoot, boolean formated)
        throws JAXBException, IOException {
        final JAXBContext jc = JAXBContext.newInstance(populateEntities(entity.getClass()));
        final Marshaller marshaller = jc.createMarshaller();
        marshaller.setProperty(MarshallerProperties.MEDIA_TYPE, "application/json");
        marshaller.setProperty(MarshallerProperties.JSON_INCLUDE_ROOT, includeRoot);
        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, formated);

        marshaller.marshal(entity, out);
    }

    public static <T> String toJSON(T entity) throws JAXBException, IOException {
        return toJSON(entity, false);
    }

    public static <T> String toJSON(T entity, boolean includeRoot) throws JAXBException, IOException {
        final JAXBContext jc = JAXBContext.newInstance(populateEntities(entity.getClass()));
        final Marshaller marshaller = jc.createMarshaller();
        marshaller.setProperty(MarshallerProperties.MEDIA_TYPE, "application/json");
        marshaller.setProperty(MarshallerProperties.JSON_INCLUDE_ROOT, includeRoot);

        final StringWriter sw = new StringWriter();
        marshaller.marshal(entity, sw);
        return sw.toString();
    }

    public static <T> T fromJSON(String json, Class<T> entityClass) throws JAXBException, IOException {
        return fromJSON(json, entityClass, false);
    }

    public static <T> T fromJSON(String json, Class<T> entityClass, boolean includeRoot)
        throws JAXBException, IOException {
        final JAXBContext jc = JAXBContext.newInstance(populateEntities(entityClass));
        final Unmarshaller unmarshaller = jc.createUnmarshaller();
        unmarshaller.setProperty(UnmarshallerProperties.MEDIA_TYPE, "application/json");
        unmarshaller.setProperty(UnmarshallerProperties.JSON_INCLUDE_ROOT, includeRoot);

        final StreamSource ss = new StreamSource(new StringReader(json));
        return unmarshaller.unmarshal(ss, entityClass).getValue();
    }

    private static Class<?>[] populateEntities(Class<?> entityClass) {
        Optional<Class<?>> cls = Optional.ofNullable(entityClass);
        try {
            List<Class<?>> pe = EntityUtils.populateEntities(CONFIG.getStrings("APP.Jersey.DynamicEntities"));
            return cls.map(c -> Streams.concat(Stream.of(c), pe.stream())).orElseGet(() -> pe.stream())
                .toArray(Class<?>[]::new);
        } catch (IOException e) {
            return cls.map(c -> new Class<?>[] { c }).orElse(null);
        }
    }
}
