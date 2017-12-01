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

import java.util.Collections;
import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * @author Ren\u00E9 Adler (eagle)
 *
 */
@XmlRootElement(name = "hwaccels")
public class HWAccelsWrapper {

    private List<HWAccelWrapper<? extends HWAccelDeviceSpec>> hwaccels;

    /**
     * @return the hwaccels
     */
    @XmlElement(name = "hwaccels")
    public List<HWAccelWrapper<? extends HWAccelDeviceSpec>> getHWAccels() {
        return hwaccels;
    }

    /**
     * @param hwaccels the hwaccels to set
     */
    public HWAccelsWrapper setHWAccels(List<HWAccelWrapper<? extends HWAccelDeviceSpec>> hwaccels) {
        this.hwaccels = Collections.unmodifiableList(hwaccels);
        return this;
    }

}
