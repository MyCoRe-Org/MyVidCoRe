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

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * @author Ren\u00E9 Adler (eagle)
 *
 */
@XmlRootElement(name = "ffprobe")
public class ProbeWrapper {

    private List<StreamWrapper> streams;

    private FormatWrapper format;

    @XmlElementWrapper(name = "streams")
    @XmlElement(name = "stream")
    public List<StreamWrapper> getStreams() {
        return streams;
    }

    public void setStreams(List<StreamWrapper> streams) {
        this.streams = streams;
    }

    @XmlElement(name = "format")
    public FormatWrapper getFormat() {
        return format;
    }

    public void setFormat(FormatWrapper format) {
        this.format = format;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        final int maxLen = 10;
        StringBuilder builder = new StringBuilder();
        builder.append("ProbeWrapper [");
        if (streams != null) {
            builder.append("streams=");
            builder.append(streams.subList(0, Math.min(streams.size(), maxLen)));
            builder.append(", ");
        }
        if (format != null) {
            builder.append("format=");
            builder.append(format);
        }
        builder.append("]");
        return builder.toString();
    }

}
