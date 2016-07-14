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
package org.mycore.vidconv.event;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Ren\u00E9 Adler (eagle)
 *
 */
public class Event {

    private String type;

    private Map<String, Object> params;

    private Class<?> source;

    @SuppressWarnings("unchecked")
    public Event(final String type) {
        this(type, Collections.EMPTY_MAP, null);
    }

    public Event(final String type, Map<String, ?> params) {
        this(type, params, null);
    }

    public Event(final String type, Map<String, ?> params, Class<?> source) {
        this.type = type;
        this.params = new ConcurrentHashMap<>(params);
        this.source = source;
    }

    /**
     * @return the type
     */
    public String getType() {
        return type;
    }

    /**
     * @return the params
     */
    public Map<String, Object> getParams() {
        return params;
    }

    /**
     * @param name the parameter name
     * @param clazz the parameter type
     * @return
     */
    @SuppressWarnings("unchecked")
    public <T> T getParameter(final String name) {
        return (T) this.params.get(name);
    }

    /**
     * @param name the parameter name
     * @param value the parameter value
     */
    public void setParameter(final String name, Object value) {
        this.params.put(name, value);
    }

    /**
     * @return the source
     */
    public Class<?> getSource() {
        return source;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        final int maxLen = 10;
        StringBuilder builder = new StringBuilder();
        builder.append("Event [");
        if (type != null) {
            builder.append("type=");
            builder.append(type);
            builder.append(", ");
        }
        if (params != null) {
            builder.append("params=");
            builder.append(toString(params.entrySet(), maxLen));
            builder.append(", ");
        }
        if (source != null) {
            builder.append("source=");
            builder.append(source);
        }
        builder.append("]");
        return builder.toString();
    }

    private String toString(Collection<?> collection, int maxLen) {
        StringBuilder builder = new StringBuilder();
        builder.append("[");
        int i = 0;
        for (Iterator<?> iterator = collection.iterator(); iterator.hasNext() && i < maxLen; i++) {
            if (i > 0)
                builder.append(", ");
            builder.append(iterator.next());
        }
        builder.append("]");
        return builder.toString();
    }
}
