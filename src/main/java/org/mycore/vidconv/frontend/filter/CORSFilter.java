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

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import jakarta.annotation.Priority;
import jakarta.ws.rs.HttpMethod;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.ext.Provider;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.vidconv.common.config.Configuration;

/**
 * @author Ren\u00E9 Adler (eagle)
 *
 */
@Provider
@Priority(Priorities.HEADER_DECORATOR)
public class CORSFilter implements ContainerResponseFilter {

    private static final Logger LOGGER = LogManager.getLogger();

    private static final String ORIGIN = "Origin";

    private static final String ACCESS_CONTROL_ALLOW_CREDENTIALS = "Access-Control-Allow-Credentials";

    private static final String ACCESS_CONTROL_ALLOW_ORIGIN = "Access-Control-Allow-Origin";

    private static final String ACCESS_CONTROL_ALLOW_METHODS = "Access-Control-Allow-Methods";

    private static final String ACCESS_CONTROL_ALLOW_HEADERS = "Access-Control-Allow-Headers";

    private static final String ACCESS_CONTROL_EXPOSE_HEADERS = "Access-Control-Expose-Headers";

    private static final String ACCESS_CONTROL_MAX_AGE = "Access-Control-Max-Age";

    /*
     * (non-Javadoc)
     * 
     * @see jakarta.ws.rs.container.ContainerResponseFilter#filter(jakarta.ws.rs.
     * container.ContainerRequestContext,
     * jakarta.ws.rs.container.ContainerResponseContext)
     */
    @Override
    public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext)
            throws IOException {
        String origin = requestContext.getHeaderString(ORIGIN);
        if (origin == null) {
            return; // No CORS Request
        }

        LOGGER.debug("{} {}", requestContext.getMethod(), requestContext.getUriInfo().getPath());

        MultivaluedMap<String, Object> responseHeaders = responseContext.getHeaders();

        boolean authenticatedRequest = requestContext.getSecurityContext().getAuthenticationScheme() != null;
        if (authenticatedRequest) {
            responseHeaders.putSingle(ACCESS_CONTROL_ALLOW_CREDENTIALS, true);
        }

        responseHeaders.putSingle(ACCESS_CONTROL_ALLOW_ORIGIN,
                Configuration.instance().getString("APP.Jersey.CORSOrigin", "*"));
        responseHeaders
                .putSingle(
                        ACCESS_CONTROL_ALLOW_METHODS, Stream
                                .of(HttpMethod.DELETE, HttpMethod.GET, HttpMethod.HEAD, HttpMethod.OPTIONS,
                                        HttpMethod.PATCH, HttpMethod.POST, HttpMethod.PUT)
                                .collect(Collectors.joining(",")));
        responseHeaders.putSingle(ACCESS_CONTROL_ALLOW_HEADERS,
                Stream.of(HttpHeaders.CACHE_CONTROL, HttpHeaders.CONTENT_TYPE, HttpHeaders.EXPIRES)
                        .collect(Collectors.joining(",")));

        responseHeaders.putSingle(ACCESS_CONTROL_EXPOSE_HEADERS,
                Stream.of(HttpHeaders.CONTENT_DISPOSITION).collect(Collectors.joining(",")));

        long cacheSeconds = TimeUnit.DAYS.toSeconds(1);
        responseHeaders.putSingle(ACCESS_CONTROL_MAX_AGE, cacheSeconds);

        if (HttpMethod.OPTIONS.equals(requestContext.getMethod())) {
            responseContext.setStatus(200);
        }
    }

}
