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
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.StringTokenizer;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import javax.inject.Singleton;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HEAD;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.StreamingOutput;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.vidconv.common.util.MimeType;
import org.mycore.vidconv.frontend.annotation.CacheControl;
import org.mycore.vidconv.frontend.entity.ExceptionWrapper;
import org.mycore.vidconv.frontend.util.RangeStreamingOutput;
import org.mycore.vidconv.frontend.util.ZipStreamingOutput;
import org.mycore.vidconv.frontend.widget.Widget;
import org.mycore.vidconv.frontend.widget.WidgetManager;

/**
 * @author Ren\u00E9 Adler (eagle)
 *
 */
@Path("widget")
@Singleton
public class WidgetResource {

    private static final Logger LOGGER = LogManager.getLogger(WidgetResource.class);

    private static final WidgetManager WIDGET_MANAGER = WidgetManager.instance();

    @GET()
    @CacheControl(noTransform = true, noStore = true, private_ = @CacheControl.FieldArgument(active = true), noCache = @CacheControl.FieldArgument(active = true))
    @Path("{widget:.+}/{action:(status|start|stop)}")
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    public Response widgetActions(@PathParam("widget") String widgetOptions, @PathParam("action") String action) {
        try {
            final StringTokenizer tok = new StringTokenizer(widgetOptions, "/");

            final String widgetName = tok.nextToken();
            final List<String> widgetParams = new ArrayList<>();

            while (tok.hasMoreTokens()) {
                widgetParams.add(tok.nextToken());
            }

            final Widget widget = WIDGET_MANAGER.get(widgetName);

            if (widget != null) {
                if ("status".equals(action)) {
                    return Response.ok().status(Response.Status.OK)
                            .entity(widgetParams.isEmpty() ? widget.status() : widget.status(widgetParams))
                            .build();
                } else if ("start".equals(action)) {
                    if (widgetParams.isEmpty())
                        widget.start();
                    else
                        widget.start(widgetParams);
                } else if ("stop".equals(action)) {
                    if (widgetParams.isEmpty())
                        widget.stop();
                    else
                        widget.stop(widgetParams);
                }

                return Response.ok().status(Response.Status.OK)
                        .build();
            } else {
                LOGGER.error("widget \"" + widgetOptions + "\" not found.");
                return Response.status(Response.Status.NOT_FOUND).build();
            }
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
            final ExceptionWrapper error = new ExceptionWrapper(e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(error).build();
        }
    }

    @HEAD
    @Path("{widget:.+}/{action:(download)}")
    public Response widgetDownloadHeader(@PathParam("widget") String widgetOptions,
            @PathParam("action") String action) {
        try {
            final StringTokenizer tok = new StringTokenizer(widgetOptions, "/");

            final String widgetName = tok.nextToken();
            final List<String> widgetParams = new ArrayList<>();

            while (tok.hasMoreTokens()) {
                widgetParams.add(tok.nextToken());
            }

            final Widget widget = WIDGET_MANAGER.get(widgetName);

            if (widget != null) {
                final java.nio.file.Path path = widgetParams.isEmpty() ? widget.download()
                        : widget.download(widgetParams);

                if (path != null) {
                    return Response.ok().status(Response.Status.PARTIAL_CONTENT)
                            .header(HttpHeaders.CONTENT_LENGTH, path.toFile().length()).build();
                } else {
                    LOGGER.error("download path was empty.");
                    return Response.status(Response.Status.NOT_FOUND).build();
                }
            } else {
                LOGGER.error("widget \"" + widgetOptions + "\" not found.");
                return Response.status(Response.Status.NOT_FOUND).build();
            }
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
            final ExceptionWrapper error = new ExceptionWrapper(e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(error).build();
        }
    }

    @GET
    @Path("{widget:.+}/{action:(download)}")
    @CacheControl(noTransform = true, noStore = true, private_ = @CacheControl.FieldArgument(active = true), noCache = @CacheControl.FieldArgument(active = true))
    @Produces("*/*")
    public Response widgetDownload(@HeaderParam("Range") String range, @PathParam("widget") String widgetOptions,
            @PathParam("action") String action) {
        try {
            final StringTokenizer tok = new StringTokenizer(widgetOptions, "/");

            final String widgetName = tok.nextToken();
            final List<String> widgetParams = new ArrayList<>();

            while (tok.hasMoreTokens()) {
                widgetParams.add(tok.nextToken());
            }

            final Widget widget = WIDGET_MANAGER.get(widgetName);

            if (widget != null) {
                final java.nio.file.Path path = widgetParams.isEmpty() ? widget.download()
                        : widget.download(widgetParams);

                if (path != null) {
                    return buildStream(path, range);
                } else {
                    LOGGER.error("download path was empty.");
                    return Response.status(Response.Status.NOT_FOUND).build();
                }
            } else {
                LOGGER.error("widget \"" + widgetOptions + "\" not found.");
                return Response.status(Response.Status.NOT_FOUND).build();
            }
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(new ExceptionWrapper(e)).build();
        }
    }

    @GET
    @Path("{widget:.+}/{action:(compress)}")
    @CacheControl(noTransform = true, noStore = true, private_ = @CacheControl.FieldArgument(active = true), noCache = @CacheControl.FieldArgument(active = true))
    @Produces("application/x-zip-compressed")
    public Response widgetCompress(@PathParam("widget") String widgetOptions, @PathParam("action") String action) {
        try {
            final StringTokenizer tok = new StringTokenizer(widgetOptions, "/");

            final String widgetName = tok.nextToken();
            final List<String> widgetParams = new ArrayList<>();

            while (tok.hasMoreTokens()) {
                widgetParams.add(tok.nextToken());
            }

            final Widget widget = WIDGET_MANAGER.get(widgetName);

            if (widget != null) {
                final ZipStreamingOutput zipStream = widgetParams.isEmpty() ? widget.compress()
                        : widget.compress(widgetParams);

                if (zipStream != null) {
                    return Response.ok(zipStream, "application/x-zip-compressed").header("content-disposition",
                            "attachment; filename = \"" + zipStream.path().getFileName().toString() + ".zip\"").build();
                } else {
                    LOGGER.error("compressed download was empty.");
                    return Response.status(Response.Status.NOT_FOUND).build();
                }
            } else {
                LOGGER.error("widget \"" + widgetOptions + "\" not found.");
                return Response.status(Response.Status.NOT_FOUND).build();
            }
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(new ExceptionWrapper(e)).build();
        }
    }

    private static final Pattern RANGE_PATTERN = Pattern.compile("([^=]+)=(\\d+)(?:-(\\d+)?)");

    private Response buildStream(final java.nio.file.Path asset, final String range) throws Exception {
        final String mimeType = MimeType.detect(asset);

        return Stream.of(RANGE_PATTERN.matcher(Optional.ofNullable(range).orElse("")))
                .filter(rm -> rm.find()).findFirst()
                .map(rm -> {
                    try {
                        final File assetFile = asset.toFile();
                        final long from = Long.valueOf(rm.group(2));
                        final long to = Optional.ofNullable(rm.group(3)).map(Long::parseLong)
                                .orElse(assetFile.length() - 1);

                        final String responseRange = String.format(Locale.ROOT, "bytes %d-%d/%d", from, to,
                                assetFile.length());
                        final RandomAccessFile raf = new RandomAccessFile(assetFile, "r");
                        raf.seek(from);

                        final long len = to - from + 1;
                        final RangeStreamingOutput streamer = new RangeStreamingOutput(len, raf);

                        return Response.ok(streamer, mimeType)
                                .status(Response.Status.PARTIAL_CONTENT)
                                .header("Accept-Ranges", "bytes")
                                .header("Content-Range", responseRange)
                                .header(HttpHeaders.CONTENT_LENGTH, streamer.getLenth())
                                .header(HttpHeaders.LAST_MODIFIED, new Date(assetFile.lastModified())).build();
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }).orElseGet(() -> {
                    final StreamingOutput streamer = new StreamingOutput() {
                        @Override
                        public void write(final OutputStream output) throws IOException {
                            Files.copy(asset, output);
                        }
                    };

                    return Response
                            .ok(streamer, mimeType)
                            .header("content-disposition",
                                    "attachment; filename = \"" + asset.getFileName().toString() + "\"")
                            .build();
                });
    }
}
