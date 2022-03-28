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
package org.mycore.vidconv.frontend.entity.adapter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import jakarta.xml.bind.annotation.XmlAnyElement;
import jakarta.xml.bind.annotation.adapters.XmlAdapter;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.mycore.vidconv.frontend.entity.adapter.NVCoderAdapter.AdaptedMap;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 * @author Ren\u00E9 Adler (eagle)
 *
 */
public class NVCoderAdapter extends XmlAdapter<AdaptedMap, Map<String, Boolean>> {

    private DocumentBuilder documentBuilder;

    public NVCoderAdapter() throws Exception {
        documentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
    }

    public static class AdaptedMap {
        @XmlAnyElement
        public List<Element> elements = new ArrayList<Element>();
    }

    @Override
    public AdaptedMap marshal(Map<String, Boolean> map) throws Exception {
        if (map == null || map.isEmpty()) {
            return null;
        }

        Document document = documentBuilder.newDocument();

        AdaptedMap adaptedMap = new AdaptedMap();

        map.entrySet().stream().filter(entry -> entry.getValue() != null).forEach(entry -> {
            Element element = document.createElement(entry.getKey());
            element.setTextContent(entry.getValue().toString());
            adaptedMap.elements.add(element);
        });

        return adaptedMap;
    }

    @Override
    public Map<String, Boolean> unmarshal(AdaptedMap adaptedMap) throws Exception {
        if (adaptedMap.elements.isEmpty()) {
            return null;
        }

        HashMap<String, Boolean> map = new HashMap<>();
        IntStream.range(0, adaptedMap.elements.size()).filter(
            i -> adaptedMap.elements.get(i).getNodeType() == Node.ELEMENT_NODE
                && !adaptedMap.elements.get(i).getTextContent().isEmpty())
            .forEach(i -> {
                Node node = adaptedMap.elements.get(i);
                map.put(node.getLocalName(), Boolean.valueOf(node.getTextContent()));
            });

        return map;
    }

}
