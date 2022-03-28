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
package org.mycore.vidconv.frontend.provider;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import org.apache.logging.log4j.LogManager;
import org.mycore.vidconv.frontend.entity.ExceptionWrapper;

/**
 * @author Ren\u00E9 Adler (eagle)
 *
 */
@Provider
public class GenericExceptionMapper implements ExceptionMapper<Exception> {

    /*
     * (non-Javadoc)
     * 
     * @see jakarta.ws.rs.ext.ExceptionMapper#toResponse(java.lang.Exception)
     */
    @Override
    public Response toResponse(Exception exception) {
        LogManager.getLogger().error(exception.getMessage(), exception);
        return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(new ExceptionWrapper(exception)).build();
    }

}
