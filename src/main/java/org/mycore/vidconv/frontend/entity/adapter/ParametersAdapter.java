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
import java.util.regex.Pattern;
import java.util.stream.IntStream;

import javax.xml.bind.annotation.XmlAnyElement;
import javax.xml.bind.annotation.adapters.XmlAdapter;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * @author Ren\u00E9 Adler (eagle)
 *
 */
public class ParametersAdapter extends XmlAdapter<Element, Map<String, String>> {

    private DocumentBuilder documentBuilder;

    public ParametersAdapter() throws Exception {
        documentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
    }

    public static class AdaptedMap {
        @XmlAnyElement
        public List<Element> elements = new ArrayList<Element>();
    }

    @Override
    public Element marshal(Map<String, String> map) throws Exception {
        if (map == null || map.isEmpty()) {
            return null;
        }

        Document document = documentBuilder.newDocument();
        Element rootElement = document.createElement("parameter");
        document.appendChild(rootElement);

        map.entrySet().stream().filter(entry -> !entry.getValue().isEmpty()).forEach(entry -> {
            String name = Pattern.matches("^\\d.*", entry.getKey())
                ? "_" + entry.getKey()
                : entry.getKey();
            Element element = document.createElement(name);
            element.setTextContent(entry.getValue());
            rootElement.appendChild(element);
        });
        return rootElement;
    }

    @Override
    public Map<String, String> unmarshal(Element rootElement) throws Exception {
        NodeList nodeList = rootElement.getChildNodes();
        if (nodeList.getLength() == 0) {
            return null;
        }

        HashMap<String, String> map = new HashMap<String, String>();
        IntStream.range(0, nodeList.getLength()).filter(
            i -> nodeList.item(i).getNodeType() == Node.ELEMENT_NODE && !nodeList.item(i).getTextContent().isEmpty())
            .forEach(i -> {
                Node node = nodeList.item(i);
                String name = Pattern.matches("^_\\d.*", node.getLocalName())
                    ? node.getLocalName().substring(1)
                    : node.getLocalName();
                map.put(name, node.getTextContent());
            });

        return map;
    }

}
