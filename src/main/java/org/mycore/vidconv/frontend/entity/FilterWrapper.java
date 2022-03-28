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

import jakarta.xml.bind.annotation.XmlRootElement;

/**
 * @author Ren\u00E9 Adler (eagle)
 *
 */
@XmlRootElement(name = "filter")
public class FilterWrapper {

    private Boolean timelineSupport;

    private Boolean sliceSupport;

    private Boolean commandSupport;

    private String name;

    private String description;

    private String ioSupport;

    /**
     * @return the timelineSupport
     */
    public Boolean getTimelineSupport() {
        return timelineSupport;
    }

    /**
     * @param timelineSupport the timelineSupport to set
     */
    public void setTimelineSupport(Boolean timelineSupport) {
        this.timelineSupport = timelineSupport;
    }

    /**
     * @return the sliceSupport
     */
    public Boolean getSliceSupport() {
        return sliceSupport;
    }

    /**
     * @param sliceSupport the sliceSupport to set
     */
    public void setSliceSupport(Boolean sliceSupport) {
        this.sliceSupport = sliceSupport;
    }

    /**
     * @return the commandSupport
     */
    public Boolean getCommandSupport() {
        return commandSupport;
    }

    /**
     * @param commandSupport the commandSupport to set
     */
    public void setCommandSupport(Boolean commandSupport) {
        this.commandSupport = commandSupport;
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
     * @return the ioSupport
     */
    public String getIoSupport() {
        return ioSupport;
    }

    /**
     * @param ioSupport the ioSupport to set
     */
    public void setIoSupport(String ioSupport) {
        this.ioSupport = ioSupport;
    }

}
