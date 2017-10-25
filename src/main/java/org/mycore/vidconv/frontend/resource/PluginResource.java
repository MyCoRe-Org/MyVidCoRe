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

import java.util.concurrent.TimeUnit;

import javax.inject.Singleton;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.mycore.vidconv.backend.service.PluginService;
import org.mycore.vidconv.frontend.annotation.CacheMaxAge;
import org.mycore.vidconv.frontend.entity.PluginsWrapper;

/**
 * @author Ren\u00E9 Adler (eagle)
 *
 */
@Path("plugins")
@Singleton
public class PluginResource {

    @GET
    @CacheMaxAge(time = 1, unit = TimeUnit.HOURS)
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    public PluginsWrapper plugins() {
        return PluginsWrapper.build(PluginService.plugins());
    }
}
