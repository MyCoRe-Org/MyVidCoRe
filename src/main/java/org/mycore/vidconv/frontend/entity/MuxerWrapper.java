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

import jakarta.xml.bind.annotation.XmlRootElement;

/**
 * @author Ren\u00E9 Adler (eagle)
 *
 */
@XmlRootElement(name = "muxer")
public class MuxerWrapper {

    private String name;

    private String extension;

    private String mimeType;

    private String audioCodec;

    private String videoCodec;

    private String subtitleCodec;

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
     * @return the extension
     */
    public String getExtension() {
        return extension;
    }

    /**
     * @param extension the extension to set
     */
    public void setExtension(String extension) {
        this.extension = extension;
    }

    /**
     * @return the mimeType
     */
    public String getMimeType() {
        return mimeType;
    }

    /**
     * @param mimeType the mimeType to set
     */
    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }

    /**
     * @return the audioCodec
     */
    public String getAudioCodec() {
        return audioCodec;
    }

    /**
     * @param audioCodec the audioCodec to set
     */
    public void setAudioCodec(String audioCodec) {
        this.audioCodec = audioCodec;
    }

    /**
     * @return the videoCodec
     */
    public String getVideoCodec() {
        return videoCodec;
    }

    /**
     * @param videoCodec the videoCodec to set
     */
    public void setVideoCodec(String videoCodec) {
        this.videoCodec = videoCodec;
    }

    /**
     * @return the subtitleCodec
     */
    public String getSubtitleCodec() {
        return subtitleCodec;
    }

    /**
     * @param subtitleCodec the subtitleCodec to set
     */
    public void setSubtitleCodec(String subtitleCodec) {
        this.subtitleCodec = subtitleCodec;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("MuxerWrapper [");
        if (name != null) {
            builder.append("name=");
            builder.append(name);
            builder.append(", ");
        }
        if (extension != null) {
            builder.append("extension=");
            builder.append(extension);
            builder.append(", ");
        }
        if (mimeType != null) {
            builder.append("mimeType=");
            builder.append(mimeType);
            builder.append(", ");
        }
        if (audioCodec != null) {
            builder.append("audioCodec=");
            builder.append(audioCodec);
            builder.append(", ");
        }
        if (videoCodec != null) {
            builder.append("videoCodec=");
            builder.append(videoCodec);
            builder.append(", ");
        }
        if (subtitleCodec != null) {
            builder.append("subtitleCodec=");
            builder.append(subtitleCodec);
        }
        builder.append("]");
        return builder.toString();
    }

}
