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
package org.mycore.vidconv.frontend.resource;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

import javax.inject.Singleton;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.mycore.vidconv.backend.encoder.FFMpegImpl;
import org.mycore.vidconv.frontend.annotation.CacheMaxAge;
import org.mycore.vidconv.frontend.entity.CodecWrapper.Type;
import org.mycore.vidconv.frontend.entity.CodecsWrapper;
import org.mycore.vidconv.frontend.entity.DecodersWrapper;
import org.mycore.vidconv.frontend.entity.EncodersWrapper;
import org.mycore.vidconv.frontend.entity.FormatsWrapper;
import org.mycore.vidconv.frontend.entity.GPUsWrapper;

/**
 * @author Ren\u00E9 Adler (eagle)
 *
 */
@Path("converter")
@Singleton
public class ConverterResource {

    @GET
    @CacheMaxAge(time = 1, unit = TimeUnit.HOURS)
    @Path("codecs{filter:(/[^/]+?)?}{value:(/([^/]+)?)?}")
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    public CodecsWrapper getCodecs(@PathParam("filter") String filter, @PathParam("value") String value)
        throws Exception {
        CodecsWrapper codecs = FFMpegImpl.codecs();

        String f = Optional.ofNullable(filter.replaceAll("/", "")).orElse("");
        String v = Optional.ofNullable(value.replaceAll("/", "")).orElse("");

        if (!f.isEmpty() && !v.isEmpty()) {
            if ("type".equals(f)) {
                codecs = CodecsWrapper.getByType(codecs, Type.valueOf(v));
            } else if ("name".equals(f)) {
                codecs = CodecsWrapper.getByName(codecs, v);
            } else if ("description".equals(f)) {
                codecs = CodecsWrapper.getByDescription(codecs, v);
            } else if ("encoder".equals(f)) {
                codecs = CodecsWrapper.getByEncoder(codecs, v);
            } else if ("decoder".equals(f)) {
                codecs = CodecsWrapper.getByDecoder(codecs, v);
            }
        }

        return codecs;
    }

    @GET
    @CacheMaxAge(time = 1, unit = TimeUnit.HOURS)
    @Path("formats{filter:(/[^/]+?)?}{value:(/([^/]+)?)?}")
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    public FormatsWrapper getFormats(@PathParam("filter") String filter, @PathParam("value") String value)
        throws Exception {
        FormatsWrapper formats = FFMpegImpl.formats();

        String f = Optional.ofNullable(filter.replaceAll("/", "")).orElse("");
        String v = Optional.ofNullable(value.replaceAll("/", "")).orElse("");

        if (!f.isEmpty() && !v.isEmpty()) {
            if ("name".equals(f)) {
                formats = FormatsWrapper.getByName(formats, v);
            } else if ("description".equals(f)) {
                formats = FormatsWrapper.getByDescription(formats, v);
            }
        }

        return formats;
    }

    @GET
    @CacheMaxAge(time = 1, unit = TimeUnit.HOURS)
    @Path("encoder/{name}")
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    public EncodersWrapper getEncoder(@PathParam("name") String name) throws Exception {
        return FFMpegImpl.encoder(name);
    }

    @GET
    @CacheMaxAge(time = 1, unit = TimeUnit.HOURS)
    @Path("decoder/{name}")
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    public DecodersWrapper getDecoder(@PathParam("name") String name) throws Exception {
        return FFMpegImpl.decoder(name);
    }

    @GET
    @CacheMaxAge(time = 1, unit = TimeUnit.HOURS)
    @Path("gpus")
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    public GPUsWrapper getGPUs() throws Exception {
        return FFMpegImpl.detectGPUs();
    }
}
