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
package org.mycore.vidconv.resource;

import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

import javax.inject.Singleton;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.vidconv.encoder.FFMpegImpl;
import org.mycore.vidconv.entity.CodecWrapper.Type;
import org.mycore.vidconv.entity.CodecsWrapper;
import org.mycore.vidconv.entity.FormatsWrapper;

/**
 * @author Ren\u00E9 Adler (eagle)
 *
 */
@Path("converter")
@Singleton
public class ConverterResource {

    private static final Logger LOGGER = LogManager.getLogger(ConverterResource.class);

    @GET
    @Path("codecs{filter:(/[^/]+?)?}{value:(/([^/]+)?)?}")
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    public Response getCodecs(@PathParam("filter") String filter, @PathParam("value") String value) {
        try {
            CodecsWrapper codecs = FFMpegImpl.codecs();

            filter = Optional.ofNullable(filter.replaceAll("/", "")).orElse("");
            value = Optional.ofNullable(value.replaceAll("/", "")).orElse("");

            if (!filter.isEmpty() && !value.isEmpty()) {
                if ("type".equals(filter)) {
                    codecs = CodecsWrapper.getByType(codecs, Type.valueOf(value));
                } else if ("name".equals(filter)) {
                    codecs = CodecsWrapper.getByName(codecs, value);
                } else if ("description".equals(filter)) {
                    codecs = CodecsWrapper.getByDescription(codecs, value);
                } else if ("encoder".equals(filter)) {
                    codecs = CodecsWrapper.getByEncoder(codecs, value);
                } else if ("decoder".equals(filter)) {
                    codecs = CodecsWrapper.getByDecoder(codecs, value);
                }
            }

            return Response.ok().status(Response.Status.OK).entity(codecs)
                .build();
        } catch (InterruptedException | ExecutionException e) {
            LOGGER.error(e.getMessage(), e);
            final StreamingOutput so = (OutputStream os) -> e
                .printStackTrace(new PrintStream(os, false, StandardCharsets.UTF_8.toString()));
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(so).build();
        }
    }

    @GET
    @Path("formats{filter:(/[^/]+?)?}{value:(/([^/]+)?)?}")
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    public Response getFormats(@PathParam("filter") String filter, @PathParam("value") String value) {
        try {
            FormatsWrapper formats = FFMpegImpl.formats();

            filter = Optional.ofNullable(filter.replaceAll("/", "")).orElse("");
            value = Optional.ofNullable(value.replaceAll("/", "")).orElse("");

            if (!filter.isEmpty() && !value.isEmpty()) {
                if ("name".equals(filter)) {
                    formats = FormatsWrapper.getByName(formats, value);
                } else if ("description".equals(filter)) {
                    formats = FormatsWrapper.getByDescription(formats, value);
                }
            }

            return Response.ok().status(Response.Status.OK).entity(formats)
                .build();
        } catch (InterruptedException | ExecutionException e) {
            LOGGER.error(e.getMessage(), e);
            final StreamingOutput so = (OutputStream os) -> e
                .printStackTrace(new PrintStream(os, false, StandardCharsets.UTF_8.toString()));
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(so).build();
        }
    }

    @GET
    @Path("encoder/{name}")
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    public Response getEncoder(@PathParam("name") String name) {
        try {
            return Response.ok().status(Response.Status.OK).entity(FFMpegImpl.encoder(name))
                .build();
        } catch (InterruptedException | NumberFormatException | ExecutionException e) {
            LOGGER.error(e.getMessage(), e);
            final StreamingOutput so = (OutputStream os) -> e
                .printStackTrace(new PrintStream(os, false, StandardCharsets.UTF_8.toString()));
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(so).build();
        }
    }
}
