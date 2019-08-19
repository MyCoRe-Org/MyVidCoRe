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
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.util.concurrent.TimeUnit;

@Documented
@Retention(RUNTIME)
@Target(METHOD)
/**
 * Used to define the {@link javax.ws.rs.core.HttpHeaders#CACHE_CONTROL} header
 * via annotation
 * 
 * @author Ren\u00E9 Adler (eagle)
 * 
 * @see <a href=
 *      'http://www.w3.org/Protocols/rfc2616/rfc2616-sec14.html#sec14.9.3'>W3C
 *      Header Field Definitions</a>
 *
 */
public @interface CacheControl {

    /**
     * sets {@code private} directive
     * 
     * @see <a href=
     *      "https://tools.ietf.org/html/rfc7234#section-5.2.2.6">private
     *      definition</a>
     */
    FieldArgument private_() default @FieldArgument();

    /**
     * sets {@code no-cache} directive
     * 
     * @see <a href=
     *      "https://tools.ietf.org/html/rfc7234#section-5.2.2.2">no-cache
     *      definition</a>
     */
    FieldArgument noCache() default @FieldArgument;

    /**
     * if {@link #noCache()}, sets {@code noCache} directive argument to these
     * values
     * 
     * @see <a href=
     *      "https://tools.ietf.org/html/rfc7234#section-5.2.2.2">no-cache
     *      definition</a>
     */
    String[] noCacheFields() default {};

    /**
     * if true, sets {@code no-store} directive
     * 
     * @see <a href=
     *      "https://tools.ietf.org/html/rfc7234#section-5.2.2.3">no-store
     *      definition</a>
     */
    boolean noStore() default false;

    /**
     * if true, sets {@code no-transform} directive
     * 
     * @see <a href=
     *      "https://tools.ietf.org/html/rfc7234#section-5.2.2.4">no-transform
     *      definition</a>
     */
    boolean noTransform() default false;

    /**
     * if true, sets {@code must-revalidate} directive
     * 
     * @see <a href=
     *      "https://tools.ietf.org/html/rfc7234#section-5.2.2.1">must-revalidate
     *      definition</a>
     */
    boolean mustRevalidate() default false;

    /**
     * if true, sets {@code proxy-revalidate} directive
     * 
     * @see <a href=
     *      "https://tools.ietf.org/html/rfc7234#section-5.2.2.7">proxy-revalidate
     *      definition</a>
     */
    boolean proxyRevalidate() default false;

    /**
     * if true, sets {@code public} directive
     * 
     * @see <a href="https://tools.ietf.org/html/rfc7234#section-5.2.2.5">public
     *      definition</a>
     */
    boolean public_() default false;

    /**
     * Sets {@code max-age} directive
     * 
     * @see <a href=
     *      "https://tools.ietf.org/html/rfc7234#section-5.2.2.8">max-age
     *      definition</a>
     */
    Age maxAge() default @Age(time = -1, unit = TimeUnit.SECONDS);

    /**
     * Sets {@code s-maxage} directive
     * 
     * @see <a href=
     *      "https://tools.ietf.org/html/rfc7234#section-5.2.2.9">s-maxage
     *      definition</a>
     */
    Age sMaxAge() default @Age(time = -1, unit = TimeUnit.SECONDS);

    /**
     * Sets further Cache-Control Extensions
     * 
     * @see <a href="https://tools.ietf.org/html/rfc7234#section-5.2.3">Cache
     *      Control Extensions</a>
     */
    Extension[] extensions() default {};

    @Retention(RUNTIME)
    @Target(ElementType.ANNOTATION_TYPE)
    public static @interface Age {
        long time();

        TimeUnit unit() default TimeUnit.MINUTES;

    }

    public static @interface FieldArgument {
        /**
         * if true, this directive is present in header value
         */
        boolean active() default false;

        /**
         * if {@link #active()}, sets directive argument to these values
         */
        String[] fields() default {};
    }

    public static @interface Extension {
        String directive();

        String argument() default "";
    }

}
