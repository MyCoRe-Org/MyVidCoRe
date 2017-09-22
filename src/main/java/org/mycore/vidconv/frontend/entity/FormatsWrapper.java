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

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import javax.xml.bind.annotation.XmlRootElement;

import org.mycore.vidconv.common.util.StringUtils;

/**
 * @author Ren\u00E9 Adler (eagle)
 *
 */
@XmlRootElement(name = "formats")
public class FormatsWrapper {

    private List<FormatWrapper> formats;

    public static FormatsWrapper getByName(FormatsWrapper formats, String name) {
        return new FormatsWrapper()
            .setFormats(formats.getFormats().stream().filter(c -> StringUtils.filter(c.getName(), name))
                .collect(Collectors.toList()));
    }

    public static FormatsWrapper getByDescription(FormatsWrapper formats, String search) {
        return new FormatsWrapper()
            .setFormats(formats.getFormats().stream()
                .filter(c -> StringUtils.containsIgnoreCase(c.getDescription(), search))
                .collect(Collectors.toList()));
    }

    /**
     * @return the formats
     */
    public synchronized List<FormatWrapper> getFormats() {
        return formats;
    }

    /**
     * @param formats the formats to set
     */
    public FormatsWrapper setFormats(List<FormatWrapper> formats) {
        this.formats = Collections.synchronizedList(formats);
        return this;
    }

}
