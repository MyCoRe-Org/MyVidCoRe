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
package org.mycore.vidconv.frontend.resource;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import javax.inject.Singleton;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Response;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.vidconv.common.ClassTools;
import org.mycore.vidconv.frontend.annotation.CacheControl;
import org.mycore.vidconv.frontend.entity.ExceptionWrapper;
import org.mycore.vidconv.frontend.entity.ResourceWrapper;

/**
 * @author Ren\u00E9 Adler (eagle)
 *
 */
@Path("/")
@Singleton
public class WebResource {

    private static final Logger LOGGER = LogManager.getLogger(WebResource.class);

    private static final ClassLoader CLASS_LOADER = ClassTools.getClassLoader();

    private static final String SEPARATOR = "/";

    private static final String RESOURCE_DIR = "META-INF/resources";

    private static final String INDEX_FILE = "index.html";

    @GET
    @CacheControl(maxAge = @CacheControl.Age(time = 30, unit = TimeUnit.DAYS), proxyRevalidate = true)
    @Produces("*/*")
    public Response getWebResource() {
        return getWebResource(INDEX_FILE);
    }

    @GET
    @CacheControl(maxAge = @CacheControl.Age(time = 30, unit = TimeUnit.DAYS), proxyRevalidate = true)
    @Path("{fileName:.+}")
    @Produces("*/*")
    public Response getWebResource(@PathParam("fileName") String fileName) {
        try {
            final Optional<ResourceWrapper> res = getResource(fileName);
            if (res.isPresent()) {
                final ResourceWrapper r = res.get();
                return Response.ok().tag(r.getETag()).type(r.getMimeType()).entity(r.getContent()).build();
            } else {
                LOGGER.error("resource \"{}\" not found.", fileName);
                return Response.status(Response.Status.NOT_FOUND).build();
            }
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(new ExceptionWrapper(e)).build();
        }
    }

    private Optional<ResourceWrapper> getResource(String fileName) {
        final String fn = RESOURCE_DIR + SEPARATOR + fileName;
        try (final InputStream is = Optional.ofNullable(CLASS_LOADER.getResourceAsStream(fn))
                .orElseGet(() -> getResourceAsStream(fn))) {
            final ResourceWrapper res = new ResourceWrapper(fn, is);
            LOGGER.info("loaded resource \"" + fileName + "\" with mime type \"" + res.getMimeType() + "\"");
            return Optional.of(res);
        } catch (NullPointerException e) {
            return Optional.empty();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private InputStream getResourceAsStream(String fileName) {
        // FIXME workaround for Windows based systems
        URL jarLoc = getClass().getProtectionDomain().getCodeSource().getLocation();
        if (jarLoc.getFile().endsWith(".jar")) {
            try {
                return new URL("jar:" + jarLoc.toString() + "!/" + fileName).openStream();
            } catch (IOException e) {
                return null;
            }
        }

        return null;
    }
}
