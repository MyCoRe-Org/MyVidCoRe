/*
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
package org.mycore.vidconv.frontend.provider;

import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyReader;
import javax.xml.bind.annotation.XmlRootElement;

import org.mycore.vidconv.common.config.Configuration;
import org.mycore.vidconv.common.util.EntityFactory;
import org.mycore.vidconv.common.util.EntityUtils;

/**
 * @author Ren\u00E9 Adler (eagle)
 *
 */
public class EntityMessageBodyReader<T> implements MessageBodyReader<T> {

    /*
     * (non-Javadoc)
     * 
     * @see javax.ws.rs.ext.MessageBodyReader#isReadable(java.lang.Class,
     * java.lang.reflect.Type, java.lang.annotation.Annotation[],
     * javax.ws.rs.core.MediaType)
     */
    @Override
    public boolean isReadable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return (MediaType.APPLICATION_JSON_TYPE.isCompatible(mediaType)
                || MediaType.APPLICATION_XML_TYPE.isCompatible(mediaType))
                && type.getAnnotation(XmlRootElement.class) != null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.ws.rs.ext.MessageBodyReader#readFrom(java.lang.Class,
     * java.lang.reflect.Type, java.lang.annotation.Annotation[],
     * javax.ws.rs.core.MediaType, javax.ws.rs.core.MultivaluedMap,
     * java.io.InputStream)
     */
    @Override
    public T readFrom(Class<T> type, Type genericType, Annotation[] annotations, MediaType mediaType,
            MultivaluedMap<String, String> httpHeaders, InputStream entityStream)
            throws IOException, WebApplicationException {
        Class<?>[] entities = EntityUtils
                .populateEntities(Configuration.instance().getStrings("APP.Jersey.DynamicEntities")).stream()
                .toArray(Class<?>[]::new);

        if (mediaType.equals(MediaType.APPLICATION_JSON_TYPE)) {
            return new EntityFactory<>(type, entities).fromJSON(entityStream);
        }

        return new EntityFactory<>(type, entities).fromXML(entityStream);
    }

}
