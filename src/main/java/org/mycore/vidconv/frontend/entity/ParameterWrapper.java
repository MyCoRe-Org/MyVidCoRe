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
package org.mycore.vidconv.frontend.entity;

import java.util.List;

/**
 * @author Ren\u00E9 Adler (eagle)
 *
 */
public class ParameterWrapper {

    private String name;

    private String type;

    private String description;

    private String fromValue;

    private String toValue;

    private String defaultValue;

    private List<ParameterValue> values;

    /**
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * @param name the name to set
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * @return the type
     */
    public String getType() {
        return type;
    }

    /**
     * @param type the type to set
     */
    public void setType(String type) {
        this.type = type;
    }

    /**
     * @return the description
     */
    public String getDescription() {
        return description;
    }

    /**
     * @param description the description to set
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * @return the fromValue
     */
    public String getFromValue() {
        return fromValue;
    }

    /**
     * @param fromValue the fromValue to set
     */
    public void setFromValue(String fromValue) {
        this.fromValue = parseValueConstant(fromValue);
    }

    /**
     * @return the toValue
     */
    public String getToValue() {
        return toValue;
    }

    /**
     * @param toValue the toValue to set
     */
    public void setToValue(String toValue) {
        this.toValue = parseValueConstant(toValue);
    }

    /**
     * @return the defaultValue
     */
    public String getDefaultValue() {
        return defaultValue;
    }

    /**
     * @param defaultValue the defaultValue to set
     */
    public void setDefaultValue(String defaultValue) {
        this.defaultValue = parseValueConstant(defaultValue);
    }

    /**
     * @return the values
     */
    public List<ParameterValue> getValues() {
        return values;
    }

    /**
     * @param values the values to set
     */
    public void setValues(List<ParameterValue> values) {
        this.values = values;
    }

    private String parseValueConstant(String value) {
        switch (value) {
            case "I64_MIN":
            case "INT_MIN":
                return new Integer(Integer.MIN_VALUE).toString();
            case "I64_MAX":
            case "INT_MAX":
                return new Integer(Integer.MAX_VALUE).toString();
            case "FLT_MIN":
                return new Float(Float.MIN_VALUE).toString();
            case "-FLT_MAX":
                return new Float(Float.MAX_VALUE * -1).toString();
            case "FLT_MAX":
                return new Float(Float.MAX_VALUE).toString();
            default:
                break;
        }

        return value;
    }

    public static class ParameterValue {
        private String name;

        private String description;

        /**
         * @return the name
         */
        public String getName() {
            return name;
        }

        /**
         * @param name the name to set
         */
        public void setName(String name) {
            this.name = name;
        }

        /**
         * @return the description
         */
        public String getDescription() {
            return description;
        }

        /**
         * @param description the description to set
         */
        public void setDescription(String description) {
            this.description = description;
        }

        /* (non-Javadoc)
         * @see java.lang.Object#toString()
         */
        @Override
        public String toString() {
            StringBuilder builder = new StringBuilder();
            builder.append("ParameterValue [");
            if (name != null) {
                builder.append("name=");
                builder.append(name);
                builder.append(", ");
            }
            if (description != null) {
                builder.append("description=");
                builder.append(description);
            }
            builder.append("]");
            return builder.toString();
        }

    }

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        final int maxLen = 10;
        StringBuilder builder = new StringBuilder();
        builder.append("ParameterWrapper [");
        if (name != null) {
            builder.append("name=");
            builder.append(name);
            builder.append(", ");
        }
        if (type != null) {
            builder.append("type=");
            builder.append(type);
            builder.append(", ");
        }
        if (description != null) {
            builder.append("description=");
            builder.append(description);
            builder.append(", ");
        }
        if (fromValue != null) {
            builder.append("fromValue=");
            builder.append(fromValue);
            builder.append(", ");
        }
        if (toValue != null) {
            builder.append("toValue=");
            builder.append(toValue);
            builder.append(", ");
        }
        if (defaultValue != null) {
            builder.append("defaultValue=");
            builder.append(defaultValue);
            builder.append(", ");
        }
        if (values != null) {
            builder.append("values=");
            builder.append(values.subList(0, Math.min(values.size(), maxLen)));
        }
        builder.append("]");
        return builder.toString();
    }

}
