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

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;

import org.mycore.vidconv.plugin.GenericPlugin;

/**
 * @author Ren\u00E9 Adler (eagle)
 *
 */
@XmlRootElement(name = "plugins")
public class PluginsWrapper {

    private List<PluginWrapper> plugins;

    public static PluginsWrapper build(Map<String, GenericPlugin> plugins) {
        PluginsWrapper pw = new PluginsWrapper();
        pw.setPlugins(plugins.entrySet().stream().map(e -> new PluginWrapper(e.getKey(), e.getValue().isEnabled()))
            .collect(Collectors.toList()));
        return pw;
    }

    /**
     * @return the plugins
     */
    @XmlElement
    public List<PluginWrapper> getPlugins() {
        return plugins;
    }

    /**
     * @param plugins the plugins to set
     */
    public void setPlugins(List<PluginWrapper> plugins) {
        this.plugins = plugins;
    }

}
