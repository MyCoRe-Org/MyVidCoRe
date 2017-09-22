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
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.stream.StreamSource;

import org.eclipse.persistence.jaxb.UnmarshallerProperties;

/**
 * @author Ren\u00E9 Adler (eagle)
 *
 */
public class JsonUtils {

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
        final JAXBContext jc = JAXBContext.newInstance(entityClass);
        final Unmarshaller unmarshaller = jc.createUnmarshaller();
        unmarshaller.setProperty(UnmarshallerProperties.MEDIA_TYPE, "application/json");
        unmarshaller.setProperty(UnmarshallerProperties.JSON_INCLUDE_ROOT, includeRoot);

        final StreamSource json = new StreamSource(is);
        return unmarshaller.unmarshal(json, entityClass).getValue();
    }
}
