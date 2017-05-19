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
package org.mycore.vidconv.common;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

/**
 * The <code>AppPropertiesResolver</code> supports substitution of any
 * %reference% in a <code>String</code> or <code>Property</code> instance.
 * <p>
 * // possible use case<br>
 * AppProperties p = Configuration.instance().getProperties();<br>
 * AppPropertiesResolver r = new MCRPropertiesResolver(p);<br>
 * AppProperties resolvedProperties = r.resolveAll(p);<br>
 * </p>
 * 
 * @author Matthias Eichner
 */
public class AppPropertiesResolver extends TextResolver {

    public AppPropertiesResolver() {
        super();
    }

    public AppPropertiesResolver(Map<String, String> propertiesMap) {
        super(propertiesMap);
    }

    public AppPropertiesResolver(Properties properties) {
        super(properties);
    }

    @Override
    public String addVariable(String name, String value) {
        return super.addVariable(name, removeSelfReference(name, value));
    }

    private String removeSelfReference(String name, String value) {
        int pos1 = value.indexOf("%");
        if (pos1 != -1) {
            int pos2 = value.indexOf("%", pos1 + 1);
            if (pos2 != -1) {
                String ref = value.substring(pos1 + 1, pos2);
                if (name.equals(ref)) {
                    return value.replaceAll("\\s*%" + name + "%\\s*,?", "");
                }
            }
        }
        return value;
    }

    @Override
    protected void registerDefaultTerms()
        throws NoSuchMethodException, InvocationTargetException, IllegalAccessException, InstantiationException {
        registerTerm(Property.class);
    }

    private static class Property extends Variable {
        public Property(TextResolver textResolver) {
            super(textResolver);
        }

        @Override
        public String getStartEnclosingString() {
            return "%";
        }

        @Override
        public String getEndEnclosingString() {
            return "%";
        }
    }

    /**
     * Substitute all %references% of the given <code>Properties</code> and
     * return a new <code>Properties</code> object.
     * 
     * @param toResolve
     *            properties to resolve
     * @return resolved properties
     */
    public Properties resolveAll(Properties toResolve) {
        Properties resolvedProperties = new Properties();
        for (Entry<Object, Object> entrySet : toResolve.entrySet()) {
            String key = entrySet.getKey().toString();
            String value = removeSelfReference(key, entrySet.getValue().toString());
            String resolvedValue = this.resolve(value);
            resolvedProperties.put(key, resolvedValue);
        }
        return resolvedProperties;
    }

    /**
     * Substitute all %references% of the given <code>Map</code> and return a
     * new <code>Map</code> object.
     * 
     * @param toResolve
     *            properties to resolve
     * @return resolved properties
     */
    public Map<String, String> resolveAll(Map<String, String> toResolve) {
        Map<String, String> resolvedMap = new HashMap<>();
        for (Entry<String, String> entrySet : toResolve.entrySet()) {
            String key = entrySet.getKey();
            String value = removeSelfReference(key, entrySet.getValue());
            resolvedMap.put(key, this.resolve(value));
        }
        return resolvedMap;
    }

}
