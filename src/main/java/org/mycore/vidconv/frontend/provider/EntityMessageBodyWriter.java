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
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.ext.MessageBodyWriter;
import jakarta.xml.bind.annotation.XmlRootElement;

import org.mycore.vidconv.common.config.Configuration;
import org.mycore.vidconv.common.util.EntityFactory;
import org.mycore.vidconv.common.util.EntityUtils;

/**
 * @author Ren\u00E9 Adler (eagle)
 *
 */
public class EntityMessageBodyWriter<T> implements MessageBodyWriter<T> {

	/*
	 * (non-Javadoc)
	 * 
	 * @see jakarta.ws.rs.ext.MessageBodyWriter#isWriteable(java.lang.Class,
	 * java.lang.reflect.Type, java.lang.annotation.Annotation[],
	 * jakarta.ws.rs.core.MediaType)
	 */
	@Override
	public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
		return (MediaType.APPLICATION_JSON_TYPE.isCompatible(mediaType)
				|| MediaType.APPLICATION_XML_TYPE.isCompatible(mediaType))
				&& type.getAnnotation(XmlRootElement.class) != null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see jakarta.ws.rs.ext.MessageBodyWriter#getSize(java.lang.Object,
	 * java.lang.Class, java.lang.reflect.Type,
	 * java.lang.annotation.Annotation[], jakarta.ws.rs.core.MediaType)
	 */
	@Override
	public long getSize(T t, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
		return -1;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see jakarta.ws.rs.ext.MessageBodyWriter#writeTo(java.lang.Object,
	 * java.lang.Class, java.lang.reflect.Type,
	 * java.lang.annotation.Annotation[], jakarta.ws.rs.core.MediaType,
	 * jakarta.ws.rs.core.MultivaluedMap, java.io.OutputStream)
	 */
	@Override
	public void writeTo(T t, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType,
			MultivaluedMap<String, Object> httpHeaders, OutputStream entityStream)
			throws IOException, WebApplicationException {
		Class<?>[] entities = EntityUtils
				.populateEntities(Configuration.instance().getStrings("APP.Jersey.DynamicEntities")).stream()
				.toArray(Class<?>[]::new);

		if (mediaType.equals(MediaType.APPLICATION_JSON_TYPE)) {
			new EntityFactory<>(t, entities).toJSON(entityStream);
			return;
		}

		new EntityFactory<>(t, entities).toXML(entityStream);
	}

}
