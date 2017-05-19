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
package org.mycore.vidconv.frontend.filter;

import java.io.IOException;
import java.lang.reflect.Method;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;

import org.mycore.vidconv.frontend.annotation.CacheMaxAge;
import org.mycore.vidconv.frontend.annotation.NoCache;

/**
 * @author Ren\u00E9 Adler (eagle)
 *
 */
public class CacheFilter implements ContainerResponseFilter {

	@Context
	private ResourceInfo resourceInfo;

	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.ws.rs.container.ContainerResponseFilter#filter(javax.ws.rs.
	 * container.ContainerRequestContext,
	 * javax.ws.rs.container.ContainerResponseContext)
	 */
	@Override
	public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext)
			throws IOException {
		Method method = resourceInfo.getResourceMethod();
		if (method.isAnnotationPresent(CacheMaxAge.class)) {
			CacheMaxAge maxAge = method.getAnnotation(CacheMaxAge.class);
			responseContext.getHeaders().putSingle(HttpHeaders.CACHE_CONTROL,
					"max-age=" + maxAge.unit().toSeconds(maxAge.time()));
		} else if (method.isAnnotationPresent(NoCache.class)) {
			responseContext.getHeaders().putSingle(HttpHeaders.CACHE_CONTROL, "no-cache");
		}
	}

}
