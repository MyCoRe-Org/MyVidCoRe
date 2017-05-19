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
package org.mycore.vidconv.frontend;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import javax.ws.rs.core.Feature;
import javax.ws.rs.core.FeatureContext;
import javax.xml.bind.annotation.XmlRootElement;

import org.glassfish.jersey.moxy.json.MoxyJsonFeature;
import org.glassfish.jersey.moxy.xml.MoxyXmlFeature;
import org.mycore.vidconv.frontend.filter.CacheFilter;
import org.mycore.vidconv.frontend.provider.GenericExceptionMapper;
import org.mycore.vidconv.frontend.provider.XmlMessageBodyReader;
import org.mycore.vidconv.frontend.provider.XmlMessageBodyWriter;
import org.reflections.Reflections;

/**
 * @author Ren\u00E9 Adler (eagle)
 *
 */
public class FrontendFeature implements Feature {

	private static final List<Class<?>> CACHED_ENTITIES = Collections.synchronizedList(new ArrayList<>());

	public static List<Class<?>> populateEntities(List<String> pkgs) throws IOException {
		if (CACHED_ENTITIES.isEmpty()) {
			CACHED_ENTITIES.addAll(
					pkgs.stream().map(pkg -> new Reflections(pkg).getTypesAnnotatedWith(XmlRootElement.class))
							.flatMap(ts -> ts.stream()).collect(Collectors.toList()));
		}
		return CACHED_ENTITIES;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.ws.rs.core.Feature#configure(javax.ws.rs.core.FeatureContext)
	 */
	@Override
	public boolean configure(FeatureContext context) {
		context.register(MoxyJsonFeature.class);
		context.register(MoxyXmlFeature.class);

		// internal features
		context.register(CacheFilter.class);
		context.register(GenericExceptionMapper.class);
		context.register(XmlMessageBodyReader.class);
		context.register(XmlMessageBodyWriter.class);

		return true;
	}
}
