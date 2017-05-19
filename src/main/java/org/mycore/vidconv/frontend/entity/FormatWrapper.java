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

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

/**
 * @author Ren\u00E9 Adler (eagle)
 *
 */
@XmlRootElement(name = "format")
@XmlType(name="Format", propOrder = { "name", "description", "demuxer", "muxer" })
public class FormatWrapper {

    private Boolean muxer;

    private Boolean demuxer;

    private String name;

    private String description;

    /**
     * @return the muxer
     */
    @XmlAttribute
    public Boolean getMuxer() {
        return muxer;
    }

    /**
     * @param muxer the muxer to set
     */
    public void setMuxer(Boolean muxer) {
        this.muxer = muxer;
    }

    /**
     * @return the demuxer
     */
    @XmlAttribute
    public Boolean getDemuxer() {
        return demuxer;
    }

    /**
     * @param demuxer the demuxer to set
     */
    public void setDemuxer(Boolean demuxer) {
        this.demuxer = demuxer;
    }

    /**
     * @return the name
     */
    @XmlAttribute
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
    @XmlAttribute
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
        builder.append("Format [");
        if (muxer != null) {
            builder.append("muxer=");
            builder.append(muxer);
            builder.append(", ");
        }
        if (demuxer != null) {
            builder.append("demuxer=");
            builder.append(demuxer);
            builder.append(", ");
        }
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
