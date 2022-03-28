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
package org.mycore.vidconv.frontend.filter;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import jakarta.annotation.Priority;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.container.ResourceInfo;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.ext.Provider;
import jakarta.xml.bind.annotation.XmlRootElement;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;
import org.glassfish.jersey.message.internal.ReaderWriter;
import org.mycore.vidconv.common.config.Configuration;
import org.mycore.vidconv.common.util.EntityFactory;
import org.mycore.vidconv.common.util.EntityUtils;

/**
 * @author Ren\u00E9 Adler (eagle)
 *
 */
@Provider
@Priority(Priorities.USER)
public class LoggingFilter implements ContainerRequestFilter, ContainerResponseFilter {

	private static final Logger LOGGER = LogManager.getLogger();

	@Context
	private ResourceInfo resourceInfo;

	@Override
	public void filter(ContainerRequestContext requestContext) throws IOException {
		ThreadContext.put("start-time", String.valueOf(System.currentTimeMillis()));

		LOGGER.info("Entering in Resource : /{} ", requestContext.getUriInfo().getPath());
		LOGGER.info("Method Name : {} ", resourceInfo.getResourceMethod().getName());
		LOGGER.info("Class : {} ", resourceInfo.getResourceClass().getCanonicalName());

		logQueryParameters(requestContext);
		logMethodAnnotations();
		logRequestHeader(requestContext);

		String entity = readEntityStream(requestContext);
		if (null != entity && entity.trim().length() > 0) {
			LOGGER.info("Request Entity Stream : {}", entity);
		}
	}

	@Override
	public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext)
			throws IOException {
		String stTime = ThreadContext.get("start-time");

		if (null == stTime || stTime.length() == 0) {
			return;
		}
		long startTime = Long.parseLong(stTime);

		logResponseHeader(responseContext);

		String entity = readEntity(responseContext);
		if (null != entity && entity.trim().length() > 0) {
			LOGGER.info("Response Entity Stream : {}", entity);
		}

		long executionTime = System.currentTimeMillis() - startTime;
		LOGGER.info("Total request execution time : {} milliseconds", executionTime);

		ThreadContext.clearAll();
	}

	private void logQueryParameters(ContainerRequestContext requestContext) {
		Iterator<String> iterator = requestContext.getUriInfo().getPathParameters().keySet().iterator();
		while (iterator.hasNext()) {
			String name = (String) iterator.next();
			List<String> obj = requestContext.getUriInfo().getPathParameters().get(name);
			String value = null;
			if (null != obj && obj.size() > 0) {
				value = (String) obj.get(0);
			}
			LOGGER.info("Query Parameter Name: {}, Value :{}", name, value);
		}
	}

	private void logMethodAnnotations() {
		Annotation[] annotations = resourceInfo.getResourceMethod().getDeclaredAnnotations();
		if (annotations != null && annotations.length > 0) {
			LOGGER.info("----Start Annotations of resource ----");
			Arrays.stream(annotations).forEach(LOGGER::info);
			LOGGER.info("----End Annotations of resource----");
		}
	}

	private void logRequestHeader(ContainerRequestContext requestContext) {
		LOGGER.info("----Start Header Section of request ----");
		LOGGER.info("Method Type : {}", requestContext.getMethod());
		requestContext.getHeaders().entrySet()
				.forEach(e -> LOGGER.info("Header Name: {}, Header Value :{} ", e.getKey(), e.getValue()));
		LOGGER.info("----End Header Section of request ----");
	}

	private void logResponseHeader(ContainerResponseContext responseContext) {
		LOGGER.info("----Start Header Section of response ----");
		responseContext.getHeaders().entrySet()
				.forEach(e -> LOGGER.info("Header Name: {}, Header Value :{} ", e.getKey(), e.getValue()));
		LOGGER.info("----End Header Section of response ----");
	}

	private String readEntityStream(ContainerRequestContext requestContext) {
		ByteArrayOutputStream outStream = new ByteArrayOutputStream();
		final InputStream inputStream = requestContext.getEntityStream();
		final StringBuilder builder = new StringBuilder();
		try {
			ReaderWriter.writeTo(inputStream, outStream);
			byte[] requestEntity = outStream.toByteArray();
			if (requestEntity.length == 0) {
				builder.append("");
			} else {
				builder.append(new String(requestEntity, StandardCharsets.UTF_8));
			}
			requestContext.setEntityStream(new ByteArrayInputStream(requestEntity));
		} catch (IOException ex) {
			LOGGER.error("----Exception occurred while reading entity stream :{}", ex.getMessage());
		}
		return builder.toString();
	}

	private String readEntity(ContainerResponseContext responseContext) {
		try {
			if (responseContext.getEntity() != null) {
				if (responseContext.getEntityClass().isAnnotationPresent(XmlRootElement.class)) {
					Class<?>[] entities = EntityUtils
							.populateEntities(Configuration.instance().getStrings("APP.Jersey.DynamicEntities"))
							.stream().toArray(Class<?>[]::new);
					return new EntityFactory<>(responseContext.getEntity(), entities).toJSON();
				} else {
					return responseContext.getEntity().toString();
				}
			}
		} catch (Exception ex) {
			LOGGER.error("----Exception occurred while reading entity :{}", ex.getMessage());
		}

		return null;
	}

}
