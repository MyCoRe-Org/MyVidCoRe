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
import java.util.stream.Collectors;

import jakarta.xml.bind.annotation.XmlRootElement;

import org.mycore.vidconv.common.util.StringUtils;

/**
 * @author Ren\u00E9 Adler (eagle)
 *
 */
@XmlRootElement(name = "filter")
public class FiltersWrapper {

    private List<FilterWrapper> filters;

    public static FiltersWrapper getByName(FiltersWrapper filters, String name) {
        return new FiltersWrapper()
            .setFilters(filters.getFilters().stream().filter(c -> StringUtils.filter(c.getName(), name))
                .collect(Collectors.toList()));
    }

    /**
     * @return the filters
     */
    public List<FilterWrapper> getFilters() {
        return filters;
    }

    /**
     * @param filters the filters to set
     */
    public FiltersWrapper setFilters(List<FilterWrapper> filters) {
        this.filters = Collections.unmodifiableList(filters);
        return this;
    }

}
