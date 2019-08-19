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

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import javax.inject.Singleton;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.xml.bind.JAXBException;

import org.glassfish.jersey.media.multipart.FormDataBodyPart;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataParam;
import org.mycore.vidconv.backend.encoder.FFMpegImpl;
import org.mycore.vidconv.backend.service.ConverterService;
import org.mycore.vidconv.frontend.annotation.CacheControl;
import org.mycore.vidconv.frontend.entity.CodecWrapper.Type;
import org.mycore.vidconv.frontend.entity.CodecsWrapper;
import org.mycore.vidconv.frontend.entity.DecodersWrapper;
import org.mycore.vidconv.frontend.entity.EncodersWrapper;
import org.mycore.vidconv.frontend.entity.FormatsWrapper;
import org.mycore.vidconv.frontend.entity.HWAccelsWrapper;
import org.mycore.vidconv.frontend.widget.WidgetManager;

/**
 * @author Ren\u00E9 Adler (eagle)
 *
 */
@Path("converter")
@Singleton
public class ConverterResource {

    @GET
    @CacheControl(maxAge = @CacheControl.Age(time = 1, unit = TimeUnit.HOURS), proxyRevalidate = true)
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
    @CacheControl(maxAge = @CacheControl.Age(time = 1, unit = TimeUnit.HOURS), proxyRevalidate = true)
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
    @CacheControl(maxAge = @CacheControl.Age(time = 1, unit = TimeUnit.HOURS), proxyRevalidate = true)
    @Path("encoder/{name}")
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    public EncodersWrapper getEncoder(@PathParam("name") String name) throws Exception {
        return FFMpegImpl.encoder(name);
    }

    @GET
    @CacheControl(maxAge = @CacheControl.Age(time = 1, unit = TimeUnit.HOURS), proxyRevalidate = true)
    @Path("decoder/{name}")
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    public DecodersWrapper getDecoder(@PathParam("name") String name) throws Exception {
        return FFMpegImpl.decoder(name);
    }

    @GET
    @CacheControl(maxAge = @CacheControl.Age(time = 1, unit = TimeUnit.HOURS), proxyRevalidate = true)
    @Path("hwaccels")
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    public HWAccelsWrapper getHWAccels() throws Exception {
        return FFMpegImpl.detectHWAccels();
    }

    @POST
    @Path("addjob")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces({ MediaType.TEXT_PLAIN })
    public String addJob(
            @FormDataParam("file") InputStream is,
            @FormDataParam("file") FormDataContentDisposition fileDetail,
            @FormDataParam("filename") FormDataBodyPart fileName,
            @FormDataParam("id") FormDataBodyPart id,
            @FormDataParam("priority") FormDataBodyPart priority,
            @FormDataParam("callback") FormDataBodyPart callback)
            throws IOException, InterruptedException, JAXBException, ExecutionException {
        final String jobId = Optional.ofNullable(id).map(i -> i.getValueAs(String.class)).orElse(null);
        final int prio = Optional.ofNullable(priority).map(i -> i.getValueAs(Integer.class)).orElse(0);
        final String completeCallBack = Optional.ofNullable(callback).map(cb -> cb.getValueAs(String.class))
                .orElse(null);
        final String fn = Optional.ofNullable(fileName).map(f -> f.getValueAs(String.class)).filter(f -> !f.isEmpty())
                .orElseGet(() -> decodeFormDataFileName(fileDetail.getFileName()));

        ConverterService converterService = ((ConverterService) WidgetManager.instance()
                .get(ConverterService.class));

        java.nio.file.Path tmpFile = Paths.get(converterService.getTempDir())
                .resolve(Optional.ofNullable(jobId).orElseGet(() -> Long.toHexString(new Random().nextLong())))
                .resolve(fn);

        Files.createDirectories(tmpFile.getParent());

        Files.copy(is, tmpFile, StandardCopyOption.REPLACE_EXISTING);

        return converterService.addJob(tmpFile, jobId, prio, completeCallBack);
    }

    private String decodeFormDataFileName(String fileName) {
        // Workaround to fix filename attribute of multipart/form-data
        // https://tools.ietf.org/html/rfc7578#section-2.3
        return new String(fileName.getBytes(StandardCharsets.ISO_8859_1), StandardCharsets.UTF_8);
    }

    @GET
    @Path("removejob/{jobId}")
    public String removeJob(@PathParam("jobId") String jobId) throws IOException {
        ConverterService converterService = ((ConverterService) WidgetManager.instance()
                .get(ConverterService.class));

        return Boolean.toString(converterService.removeJob(jobId));
    }
}
