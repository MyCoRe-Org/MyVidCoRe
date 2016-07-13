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

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;

import javax.inject.Singleton;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import javax.xml.bind.JAXBException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.vidconv.config.Settings;
import org.mycore.vidconv.encoder.FFMpegImpl;
import org.mycore.vidconv.entity.SettingsWrapper;

/**
 * @author Ren\u00E9 Adler (eagle)
 *
 */
@Path("settings")
@Singleton
public class SettingsResource {

    private static final Logger LOGGER = LogManager.getLogger(SettingsResource.class);

    private static final Settings SETTINGS = Settings.instance();

    @GET
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    public Response getSettings() {
        return Response.ok().status(Response.Status.OK).entity(SETTINGS.getSettings())
                .build();
    }

    @POST
    @Consumes({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    public Response postSettings(final SettingsWrapper settings) {
        try {
            SETTINGS.setSettings(settings);
            LOGGER.info(FFMpegImpl.command(settings));
            return Response.ok().status(Response.Status.OK).entity(settings)
                    .build();
        } catch (JAXBException | IOException | InterruptedException e) {
            LOGGER.error(e.getMessage(), e);
            final StreamingOutput so = (OutputStream os) -> e.printStackTrace(new PrintStream(os));
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(so).build();
        }
    }
}
