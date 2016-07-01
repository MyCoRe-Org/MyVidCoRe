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
package org.mycore.vidconv.resource;

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import javax.inject.Singleton;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.vidconv.entity.ExceptionWrapper;
import org.mycore.vidconv.widget.Widget;
import org.mycore.vidconv.widget.WidgetManager;

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
    @Path("{widget:.+}/{action:(status|start|stop)}")
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    public Response getWidgetStatus(@PathParam("widget") String widgetOptions, @PathParam("action") String action) {
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

}
