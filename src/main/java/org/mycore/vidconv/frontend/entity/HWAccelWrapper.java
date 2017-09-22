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
package org.mycore.vidconv.frontend.entity;

import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

/**
 * @author Ren\u00E9 Adler (eagle)
 *
 */
@XmlRootElement(name = "hwaccel")
public class HWAccelWrapper<T extends HWAccelDeviceSpec>
    implements Comparable<HWAccelWrapper<? extends HWAccelDeviceSpec>> {

    private HWAccelType type;

    private int index;

    private String name;

    private T deviceSpec;

    /**
     * @return the type
     */
    public HWAccelType getType() {
        return type;
    }

    /**
     * @param type the type to set
     */
    public void setType(HWAccelType type) {
        this.type = type;
    }

    /**
     * @return the index
     */
    public int getIndex() {
        return index;
    }

    /**
     * @param index the index to set
     */
    public void setIndex(int index) {
        this.index = index;
    }

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
     * @return the deviceSpec
     */
    public T getDeviceSpec() {
        return deviceSpec;
    }

    /**
     * @param deviceSpech the additionalInfo to set
     */
    @SuppressWarnings("unchecked")
    public void setDeviceSpec(T deviceSpech) {
        this.deviceSpec = (T) deviceSpech.clone();
    }

    /* (non-Javadoc)
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + index;
        result = prime * result + ((type == null) ? 0 : type.hashCode());
        return result;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof HWAccelWrapper)) {
            return false;
        }
        HWAccelWrapper<? extends HWAccelDeviceSpec> other = (HWAccelWrapper<?>) obj;
        if (index != other.index) {
            return false;
        }
        
        return type != other.type;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("HWAccelWrapper [");
        if (type != null) {
            builder.append("type=");
            builder.append(type);
            builder.append(", ");
        }
        builder.append("index=");
        builder.append(index);
        builder.append(", ");
        if (name != null) {
            builder.append("name=");
            builder.append(name);
            builder.append(", ");
        }
        if (deviceSpec != null) {
            builder.append("deviceSpec=");
            builder.append(deviceSpec);
        }
        builder.append("]");
        return builder.toString();
    }

    @XmlType(name = "HWAccelType")
    @XmlEnum
    public static enum HWAccelType {

        NVIDIA, UNKNOWN;
    }

    /* (non-Javadoc)
     * @see java.lang.Comparable#compareTo(java.lang.Object)
     */
    @Override
    public int compareTo(HWAccelWrapper<? extends HWAccelDeviceSpec> o) {
        if (o == null || type != o.type) {
            return -1;
        }

        if (index != o.index) {
            return deviceSpec.compareTo(o.deviceSpec);
        }

        return 0;
    }

}
