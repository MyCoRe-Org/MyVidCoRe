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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import org.mycore.vidconv.service.ConverterService.ConverterJob;

/**
 * @author Ren\u00E9 Adler (eagle)
 *
 */
@XmlRootElement(name = "converters")
public class ConvertersWrapper {

    private Integer total;

    private Integer start;

    private Integer limit;

    private List<ConverterWrapper> converters;

    ConvertersWrapper() {
        this.converters = new ArrayList<>();
    }

    public ConvertersWrapper(final Map<String, ConverterJob> converters) {
        this();

        this.converters = converters.entrySet().stream()
                .map(e -> new ConverterWrapper(e.getKey(), e.getValue())).sorted()
                .collect(Collectors.toCollection(ArrayList<ConverterWrapper>::new));
    }

    public ConvertersWrapper(final List<ConverterWrapper> converters) {
        this();

        this.converters = converters;
    }

    @XmlElement(name = "converter")
    public List<ConverterWrapper> getConverters() {
        return converters.stream().map(c -> c.getBasicCopy())
                .collect(Collectors.toCollection(ArrayList<ConverterWrapper>::new));
    }

    /**
     * @return the total
     */
    @XmlAttribute(name = "total")
    public Integer getTotal() {
        return Optional.ofNullable(total).orElse(converters.size());
    }

    /**
     * @param total the total to set
     */
    public void setTotal(Integer total) {
        this.total = total;
    }

    /**
     * @return the start
     */
    @XmlAttribute(name = "start")
    public Integer getStart() {
        return start;
    }

    /**
     * @param start the start to set
     */
    public void setStart(Integer start) {
        this.start = start;
    }

    /**
     * @return the limit
     */
    @XmlAttribute(name = "limit")
    public Integer getLimit() {
        return limit;
    }

    /**
     * @param limit the limit to set
     */
    public void setLimit(Integer limit) {
        this.limit = limit;
    }
}
