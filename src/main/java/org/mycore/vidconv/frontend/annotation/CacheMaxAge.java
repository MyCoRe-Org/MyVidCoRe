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
package org.mycore.vidconv.frontend.annotation;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.util.concurrent.TimeUnit;

@Documented
@Retention(RUNTIME)
@Target(METHOD)
/**
 * Set the "Max-age" Cache header.
 * 
 * @author Ren\u00E9 Adler (eagle)
 * 
 * @see <a href=
 *      'http://www.w3.org/Protocols/rfc2616/rfc2616-sec14.html#sec14.9.3'>W3C
 *      Header Field Definitions</a>
 *
 */
public @interface CacheMaxAge {

	/**
	 * @return The amount of time to cache this resource.
	 */
	long time();

	/**
	 * @return The {@link TimeUnit} for the given {@link #time()}.
	 */
	TimeUnit unit();

}
