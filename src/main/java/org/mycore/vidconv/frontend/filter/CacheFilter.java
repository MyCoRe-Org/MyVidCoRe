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
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.glassfish.grizzly.http.util.HttpStatus;
import org.mycore.vidconv.frontend.annotation.CacheControl;
import org.mycore.vidconv.frontend.annotation.LastModified;
import org.mycore.vidconv.frontend.annotation.LastModified.LastModifiedProvider;

import jakarta.annotation.Priority;
import jakarta.ws.rs.HttpMethod;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.container.ResourceInfo;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.ext.Provider;
import jakarta.ws.rs.ext.RuntimeDelegate;

/**
 * @author Ren\u00E9 Adler (eagle)
 *
 */
@Provider
@Priority(Priorities.HEADER_DECORATOR)
public class CacheFilter implements ContainerResponseFilter {

    private static final RuntimeDelegate.HeaderDelegate<jakarta.ws.rs.core.CacheControl> HEADER_DELEGATE = RuntimeDelegate
            .getInstance().createHeaderDelegate(jakarta.ws.rs.core.CacheControl.class);

    private static final Logger LOGGER = LogManager.getLogger();

    private static final String PATTERN_RFC1123 = "EEE, dd MMM yyyy HH:mm:ss zzz";

    @Context
    private ResourceInfo resourceInfo;

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
        jakarta.ws.rs.core.CacheControl cc;
        String currentCacheControl = requestContext.getHeaderString(HttpHeaders.CACHE_CONTROL);
        if (currentCacheControl != null) {
            if (responseContext.getHeaderString(HttpHeaders.AUTHORIZATION) == null) {
                return;
            }
            cc = HEADER_DELEGATE.fromString(currentCacheControl);
        } else {
            // from https://developer.mozilla.org/en-US/docs/Glossary/cacheable
            if (!requestContext.getMethod().equals(HttpMethod.GET)
                    && !requestContext.getMethod().equals(HttpMethod.HEAD)) {
                return;
            }
            boolean statusCacheable = Stream.of(HttpStatus.OK_200, HttpStatus.NON_AUTHORATIVE_INFORMATION_203,
                    HttpStatus.NO_CONTENT_204, HttpStatus.PARTIAL_CONTENT_206, HttpStatus.MULTIPLE_CHOICES_300,
                    HttpStatus.MOVED_PERMANENTLY_301, HttpStatus.NOT_FOUND_404, HttpStatus.METHOD_NOT_ALLOWED_405,
                    HttpStatus.GONE_410, HttpStatus.REQUEST_URI_TOO_LONG_414, HttpStatus.NOT_IMPLEMENTED_501)
                    .anyMatch(i -> i.getStatusCode() == responseContext.getStatus());
            if (!statusCacheable) {
                return;
            }
            cc = getCacheConrol(resourceInfo.getResourceMethod().getAnnotation(CacheControl.class));
        }

        boolean isPrivate = cc.isPrivate() && cc.getPrivateFields().isEmpty();
        boolean isNoCache = cc.isNoCache() && cc.getNoCacheFields().isEmpty();
        if (responseContext.getHeaderString(HttpHeaders.AUTHORIZATION) != null) {
            addAuthorizationHeaderException(cc, isPrivate, isNoCache);
        }

        addLastModifiedHeader(responseContext);

        String headerValue = HEADER_DELEGATE.toString(cc);

        LogManager.getLogger()
                .debug(() -> "Cache-Control filter: " + requestContext.getUriInfo().getPath() + " " + headerValue);

        responseContext.getHeaders().putSingle(HttpHeaders.CACHE_CONTROL, headerValue);
        if (Stream.of(resourceInfo.getResourceClass(), resourceInfo.getResourceMethod())
                .map(t -> t.getAnnotation(Produces.class)).filter(Objects::nonNull).map(Produces::value)
                .flatMap(Stream::of).distinct().count() > 1) {
            // resource may produce differenct MediaTypes, we have to set Vary
            // header
            List<String> varyHeaders = Optional.ofNullable(responseContext.getHeaderString(HttpHeaders.VARY))
                    .map(Object::toString).map(s -> s.split(",")).map(Stream::of).orElseGet(Stream::empty)
                    .collect(Collectors.toList());
            if (!varyHeaders.contains(HttpHeaders.ACCEPT)) {
                varyHeaders.add(HttpHeaders.ACCEPT);
            }
            responseContext.getHeaders().putSingle(HttpHeaders.VARY,
                    varyHeaders.stream().collect(Collectors.joining(",")));
        }
    }

    private jakarta.ws.rs.core.CacheControl getCacheConrol(CacheControl cacheControlAnnotation) {
        jakarta.ws.rs.core.CacheControl cc = new jakarta.ws.rs.core.CacheControl();
        if (cacheControlAnnotation != null) {
            cc.setMaxAge(
                    (int) cacheControlAnnotation.maxAge().unit().toSeconds(cacheControlAnnotation.maxAge().time()));
            cc.setSMaxAge(
                    (int) cacheControlAnnotation.sMaxAge().unit().toSeconds(cacheControlAnnotation.sMaxAge().time()));
            Optional.ofNullable(cacheControlAnnotation.private_()).filter(CacheControl.FieldArgument::active)
                    .map(CacheControl.FieldArgument::fields).map(Stream::of).ifPresent(s -> {
                        cc.setPrivate(true);
                        cc.getPrivateFields().addAll(s.collect(Collectors.toList()));
                    });
            if (cacheControlAnnotation.public_()) {
                cc.getCacheExtension().put("public", null);
            }
            cc.setNoTransform(cacheControlAnnotation.noTransform());
            cc.setNoStore(cacheControlAnnotation.noStore());
            Optional.ofNullable(cacheControlAnnotation.noCache()).filter(CacheControl.FieldArgument::active)
                    .map(CacheControl.FieldArgument::fields).map(Stream::of).ifPresent(s -> {
                        cc.setNoCache(true);
                        cc.getNoCacheFields().addAll(s.collect(Collectors.toList()));
                    });
            cc.setMustRevalidate(cacheControlAnnotation.mustRevalidate());
            cc.setProxyRevalidate(cacheControlAnnotation.proxyRevalidate());
        } else {
            cc.setNoTransform(false); // should have been default
        }
        return cc;
    }

    private void addAuthorizationHeaderException(jakarta.ws.rs.core.CacheControl cc, boolean isPrivate,
            boolean isNoCache) {
        cc.setPrivate(true);
        if (!cc.getPrivateFields().contains(HttpHeaders.AUTHORIZATION) && !isPrivate) {
            cc.getPrivateFields().add(HttpHeaders.AUTHORIZATION);
        }
        cc.setNoCache(true);
        if (!cc.getNoCacheFields().contains(HttpHeaders.AUTHORIZATION) && !isNoCache) {
            cc.getNoCacheFields().add(HttpHeaders.AUTHORIZATION);
        }
    }

    private void addLastModifiedHeader(ContainerResponseContext responseContext) {
        Method method = resourceInfo.getResourceMethod();
        if (responseContext.getEntity() != null && method.isAnnotationPresent(LastModified.class)) {
            LastModified lm = method.getAnnotation(LastModified.class);

            try {
                LastModifiedProvider<?> provider = lm.value().getConstructor(method.getReturnType())
                        .newInstance(responseContext.getEntity());

                Optional.ofNullable(provider.lastModified())
                        .ifPresent(val -> responseContext.getHeaders().putSingle(HttpHeaders.LAST_MODIFIED,
                                new SimpleDateFormat(PATTERN_RFC1123, Locale.ROOT).format(val)));
            } catch (InstantiationException | IllegalAccessException | IllegalArgumentException
                    | InvocationTargetException | NoSuchMethodException | SecurityException e) {
                LOGGER.warn(e.getMessage(), e);
            }
        }
    }

}
