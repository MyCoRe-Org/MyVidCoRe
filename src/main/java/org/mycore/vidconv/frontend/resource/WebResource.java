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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

import javax.inject.Singleton;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.vidconv.frontend.entity.ResourceWrapper;

/**
 * @author Ren\u00E9 Adler (eagle)
 *
 */
@Path("web")
@Singleton
public class WebResource {

    private static final Logger LOGGER = LogManager.getLogger(WebResource.class);

    private static final ClassLoader CLASS_LOADER = WebResource.class.getClassLoader();

    private static final String RESOURCE_DIR = "META-INF/resources";

    private static final String INDEX_FILE = "index.html";

    @GET
    @Produces("*/*")
    public Response getWebResource() {
        return getWebResource(INDEX_FILE);
    }

    @GET
    @Path("{fileName:.+}")
    @Produces("*/*")
    public Response getWebResource(@PathParam("fileName") String fileName) {
        try {
            final Optional<ResourceWrapper> res = getResource(fileName);
            if (res.isPresent()) {
                final ResourceWrapper r = res.get();

                CacheControl cc = new CacheControl();
                cc.setMaxAge(86400);
                cc.setPrivate(true);

                return Response.ok().status(Response.Status.OK)
                    .tag(r.getETag())
                    .type(r.getMimeType())
                    .entity(r.getContent())
                    .cacheControl(cc)
                    .build();
            } else {
                LOGGER.error("resource \"" + fileName + "\" not found.");
                return Response.status(Response.Status.NOT_FOUND).build();
            }
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
            final StreamingOutput so = (OutputStream os) -> e
                .printStackTrace(new PrintStream(os, false, StandardCharsets.UTF_8.toString()));
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(so).build();
        }
    }

    private Optional<ResourceWrapper> getResource(String fileName) {
        final String fn = RESOURCE_DIR + File.separator + fileName;

        try (final InputStream is = CLASS_LOADER.getResourceAsStream(fn)) {
            final ResourceWrapper res = new ResourceWrapper(fn, is);
            LOGGER.info("loaded resource \"" + fileName + "\" with mime type \"" + res.getMimeType() + "\"");
            return Optional.of(res);
        } catch (NullPointerException e) {
            return Optional.empty();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
