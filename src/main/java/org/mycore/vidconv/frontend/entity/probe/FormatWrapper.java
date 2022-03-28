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
package org.mycore.vidconv.frontend.entity.probe;

import java.util.List;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlType;

/**
 * @author Ren\u00E9 Adler (eagle)
 *
 */
@XmlRootElement(name = "format")
@XmlType(name="Probe.Format")
@XmlAccessorType(XmlAccessType.FIELD)
public class FormatWrapper {

    @XmlAttribute(name = "duration")
    private String duration;

    @XmlAttribute(name = "probe_score")
    private String probeScore;

    @XmlAttribute(name = "nb_streams")
    private String nbStreams;

    @XmlAttribute(name = "format_long_name")
    private String formatLongName;

    @XmlElement(name = "tag")
    private List<TagWrapper> tags;

    @XmlAttribute(name = "filename")
    private String filename;

    @XmlAttribute(name = "start_time")
    private String startTime;

    @XmlAttribute(name = "nb_programs")
    private String nbPrograms;

    @XmlAttribute(name = "bit_rate")
    private String bitRate;

    @XmlAttribute(name = "format_name")
    private String formatName;

    @XmlAttribute(name = "size")
    private String size;

    /**
     * @return the duration
     */
    public String getDuration() {
        return duration;
    }

    /**
     * @param duration the duration to set
     */
    public void setDuration(String duration) {
        this.duration = duration;
    }

    /**
     * @return the probeScore
     */
    public String getProbeScore() {
        return probeScore;
    }

    /**
     * @param probeScore the probeScore to set
     */
    public void setProbeScore(String probeScore) {
        this.probeScore = probeScore;
    }

    /**
     * @return the nbStreams
     */
    public String getNbStreams() {
        return nbStreams;
    }

    /**
     * @param nbStreams the nbStreams to set
     */
    public void setNbStreams(String nbStreams) {
        this.nbStreams = nbStreams;
    }

    /**
     * @return the formatLongName
     */
    public String getFormatLongName() {
        return formatLongName;
    }

    /**
     * @param formatLongName the formatLongName to set
     */
    public void setFormatLongName(String formatLongName) {
        this.formatLongName = formatLongName;
    }

    /**
     * @return the tags
     */
    public List<TagWrapper> getTags() {
        return tags;
    }

    /**
     * @param tags the tags to set
     */
    public void setTags(List<TagWrapper> tags) {
        this.tags = tags;
    }

    /**
     * @return the filename
     */
    public String getFilename() {
        return filename;
    }

    /**
     * @param filename the filename to set
     */
    public void setFilename(String filename) {
        this.filename = filename;
    }

    /**
     * @return the startTime
     */
    public String getStartTime() {
        return startTime;
    }

    /**
     * @param startTime the startTime to set
     */
    public void setStartTime(String startTime) {
        this.startTime = startTime;
    }

    /**
     * @return the nbPrograms
     */
    public String getNbPrograms() {
        return nbPrograms;
    }

    /**
     * @param nbPrograms the nbPrograms to set
     */
    public void setNbPrograms(String nbPrograms) {
        this.nbPrograms = nbPrograms;
    }

    /**
     * @return the bitRate
     */
    public String getBitRate() {
        return bitRate;
    }

    /**
     * @param bitRate the bitRate to set
     */
    public void setBitRate(String bitRate) {
        this.bitRate = bitRate;
    }

    /**
     * @return the formatName
     */
    public String getFormatName() {
        return formatName;
    }

    /**
     * @param formatName the formatName to set
     */
    public void setFormatName(String formatName) {
        this.formatName = formatName;
    }

    /**
     * @return the size
     */
    public String getSize() {
        return size;
    }

    /**
     * @param size the size to set
     */
    public void setSize(String size) {
        this.size = size;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        final int maxLen = 10;
        StringBuilder builder = new StringBuilder();
        builder.append("FormatWrapper [");
        if (duration != null) {
            builder.append("duration=");
            builder.append(duration);
            builder.append(", ");
        }
        if (probeScore != null) {
            builder.append("probeScore=");
            builder.append(probeScore);
            builder.append(", ");
        }
        if (nbStreams != null) {
            builder.append("nbStreams=");
            builder.append(nbStreams);
            builder.append(", ");
        }
        if (formatLongName != null) {
            builder.append("formatLongName=");
            builder.append(formatLongName);
            builder.append(", ");
        }
        if (tags != null) {
            builder.append("tags=");
            builder.append(tags.subList(0, Math.min(tags.size(), maxLen)));
            builder.append(", ");
        }
        if (filename != null) {
            builder.append("filename=");
            builder.append(filename);
            builder.append(", ");
        }
        if (startTime != null) {
            builder.append("startTime=");
            builder.append(startTime);
            builder.append(", ");
        }
        if (nbPrograms != null) {
            builder.append("nbPrograms=");
            builder.append(nbPrograms);
            builder.append(", ");
        }
        if (bitRate != null) {
            builder.append("bitRate=");
            builder.append(bitRate);
            builder.append(", ");
        }
        if (formatName != null) {
            builder.append("formatName=");
            builder.append(formatName);
            builder.append(", ");
        }
        if (size != null) {
            builder.append("size=");
            builder.append(size);
        }
        builder.append("]");
        return builder.toString();
    }

}
