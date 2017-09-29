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

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import org.mycore.vidconv.common.util.StringUtils;
import org.mycore.vidconv.frontend.entity.CodecWrapper.Type;

/**
 * @author Ren\u00E9 Adler (eagle)
 *
 */
@XmlRootElement(name = "codecs")
public class CodecsWrapper {

    private List<CodecWrapper> codecs;

    public static CodecsWrapper getByType(CodecsWrapper codecs, Type type) {
        return new CodecsWrapper().setCodecs(codecs.getByType(type));
    }

    public static CodecsWrapper getByName(CodecsWrapper codecs, String name) {
        return new CodecsWrapper().setCodecs(codecs.getByName(name));
    }

    public static CodecsWrapper getByDescription(CodecsWrapper codecs, String search) {
        return new CodecsWrapper().setCodecs(codecs.getByDescription(search));
    }

    public static CodecsWrapper getByEncoder(CodecsWrapper codecs, String encoder) {
        return new CodecsWrapper().setCodecs(codecs.getByEncoder(encoder));
    }

    public static CodecsWrapper getByDecoder(CodecsWrapper codecs, String decoder) {
        return new CodecsWrapper().setCodecs(codecs.getByDecoder(decoder));
    }

    /**
     * @return the codecs
     */
    @XmlElement
    public synchronized List<CodecWrapper> getCodecs() {
        return codecs;
    }

    /**
     * @param codecs the codecs to set
     */
    public CodecsWrapper setCodecs(List<CodecWrapper> codecs) {
        this.codecs = Collections.synchronizedList(Collections.unmodifiableList(codecs));
        return this;
    }

    public synchronized List<CodecWrapper> getByType(Type type) {
        return codecs.stream().filter(c -> c.getType() == type).collect(Collectors.toList());
    }

    public synchronized List<CodecWrapper> getByName(String name) {
        return codecs.stream().filter(c -> StringUtils.filter(c.getName(), name))
            .collect(Collectors.toList());
    }

    public synchronized List<CodecWrapper> getByDescription(String search) {
        return codecs.stream()
            .filter(c -> StringUtils.containsIgnoreCase(c.getDescription(), search))
            .collect(Collectors.toList());
    }

    public synchronized List<CodecWrapper> getByEncoder(String encoder) {
        return codecs.stream()
            .filter(c -> c.getEncoderLib() != null
                && c.getEncoderLib().stream().filter(e -> StringUtils.filter(e, encoder))
                    .count() != 0)
            .collect(Collectors.toList());
    }

    public synchronized List<CodecWrapper> getByDecoder(String decoder) {
        return codecs.stream()
            .filter(c -> c.getDecoderLib() != null
                && c.getDecoderLib().stream().filter(e -> StringUtils.filter(e, decoder))
                    .count() != 0)
            .collect(Collectors.toList());
    }

}
