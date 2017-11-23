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
package org.mycore.vidconv.frontend.widget;

import java.nio.file.Path;
import java.util.List;

import org.mycore.vidconv.frontend.util.ZipStreamingOutput;

/**
 * @author Ren\u00E9 Adler (eagle)
 *
 */
public class Widget {

    private String name;

    public Widget(final String name) {
        this.name = name;

        // auto register widget
        WidgetManager.instance().register(this);
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
     * Returns the widget status.
     * 
     * @return the status object
     */
    public Object status() {
        throw new UnsupportedOperationException("Status is not implemented");
    };

    /**
     * Returns the widget status for parameters.
     * 
     * @param params a list of parameters
     * @return the status object
     */
    public Object status(List<String> params) {
        throw new UnsupportedOperationException("Status is not implemented");
    };

    /**
     * Starts the widget.
     */
    public void start() throws Exception {
        throw new UnsupportedOperationException("Start is not implemented");
    }

    /**
     * Starts the widget.
     * 
     * @param params a list of parameters
     */
    public void start(List<String> params) throws Exception {
        throw new UnsupportedOperationException("Start is not implemented");
    }

    /**
     * Stops the widget.
     */
    public void stop() throws Exception {
        throw new UnsupportedOperationException("Stop is not implemented");
    }

    /**
     * Stops the widget.
     * 
     * @param params a list of parameters
     */
    public void stop(List<String> params) throws Exception {
        throw new UnsupportedOperationException("Stop is not implemented");
    }

    /**
     * Returns a path for download.
     * 
     * @return the file to download
     * @throws Exception
     */
    public Path download() throws Exception {
        throw new UnsupportedOperationException("Download is not implemented");
    }

    /**
     * Returns a path for download.
     * 
     * @param params a list of parameters
     * 
     * @return the file to download
     * @throws Exception
     */
    public Path download(List<String> params) throws Exception {
        throw new UnsupportedOperationException("Download is not implemented");
    }

    /**
     * Returns a ZipStreamingOutput for compressed download
     * 
     * @return the ZipStreamingOutput to download compressed
     * @throws Exception
     */
    public ZipStreamingOutput compress() throws Exception {
        throw new UnsupportedOperationException("Compress is not implemented");
    }

    /**
     * Returns a ZipStreamingOutput for compressed download
     * 
     * @param params a list of parameters
     * 
     * @return the ZipStreamingOutput to download compressed
     * @throws Exception
     */
    public ZipStreamingOutput compress(List<String> params) throws Exception {
        throw new UnsupportedOperationException("Compress is not implemented");
    }
}
