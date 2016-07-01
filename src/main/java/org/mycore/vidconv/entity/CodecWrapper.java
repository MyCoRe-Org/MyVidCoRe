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
package org.mycore.vidconv.entity;

import java.util.List;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

/**
 * @author Ren\u00E9 Adler (eagle)
 *
 */
@XmlRootElement(name = "codec")
@XmlType(propOrder = { "type", "name", "description", "lossy", "lossless", "decoderLib", "encoderLib" })
public class CodecWrapper {

    public enum Type {
        AUDIO, VIDEO, SUBTITLE
    }

    private Type type;

    private String name;

    private String description;

    private Boolean lossy;

    private Boolean lossless;

    private List<String> encoderLib;

    private List<String> decoderLib;

    public CodecWrapper() {
    }

    /**
     * @return the type
     */
    @XmlAttribute
    public Type getType() {
        return type;
    }

    /**
     * @return the name
     */
    @XmlAttribute
    public String getName() {
        return name;
    }

    /**
     * @return the description
     */
    @XmlAttribute
    public String getDescription() {
        return description;
    }

    /**
     * @return the lossy
     */
    @XmlAttribute
    public Boolean getLossy() {
        return lossy;
    }

    /**
     * @return the lossless
     */
    @XmlAttribute
    public Boolean getLossless() {
        return lossless;
    }

    /**
     * @return the encoderLib
     */
    @XmlElementWrapper(name = "encoders")
    @XmlElement(name = "encoder")
    public List<String> getEncoderLib() {
        return encoderLib;
    }

    /**
     * @return the decoderLib
     */
    @XmlElementWrapper(name = "decoders")
    @XmlElement(name = "decoder")
    public List<String> getDecoderLib() {
        return decoderLib;
    }

    /**
     * @param type the type to set
     */
    public void setType(Type type) {
        this.type = type;
    }

    /**
     * @param name the name to set
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * @param description the description to set
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * @param lossy the lossy to set
     */
    public void setLossy(Boolean lossy) {
        this.lossy = lossy;
    }

    /**
     * @param lossless the lossless to set
     */
    public void setLossless(Boolean lossless) {
        this.lossless = lossless;
    }

    /**
     * @param encoderLib the encoderLib to set
     */
    public void setEncoderLib(List<String> encoderLib) {
        this.encoderLib = encoderLib;
    }

    /**
     * @param decoderLib the decoderLib to set
     */
    public void setDecoderLib(List<String> decoderLib) {
        this.decoderLib = decoderLib;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        final int maxLen = 10;
        StringBuilder builder = new StringBuilder();
        builder.append("Codec [");
        if (type != null) {
            builder.append("type=");
            builder.append(type);
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
            builder.append(", ");
        }
        if (lossy != null) {
            builder.append("lossy=");
            builder.append(lossy);
            builder.append(", ");
        }
        if (lossless != null) {
            builder.append("lossless=");
            builder.append(lossless);
            builder.append(", ");
        }
        if (encoderLib != null) {
            builder.append("encoderLib=");
            builder.append(encoderLib.subList(0, Math.min(encoderLib.size(), maxLen)));
            builder.append(", ");
        }
        if (decoderLib != null) {
            builder.append("decoderLib=");
            builder.append(decoderLib.subList(0, Math.min(decoderLib.size(), maxLen)));
        }
        builder.append("]");
        return builder.toString();
    }
}
