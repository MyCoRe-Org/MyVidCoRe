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

import java.util.List;

/**
 * @author Ren\u00E9 Adler (eagle)
 *
 */
public class EncoderWrapper {

    private String name;

    private List<String> pixelFormats;

    private List<Integer> sampleRates;

    private List<String> sampleFormats;

    private List<String> channelLayouts;

    private List<ParameterWrapper> parameters;

    /**
     * 
     */
    public EncoderWrapper() {
        // TODO Auto-generated constructor stub
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
     * @return the pixelFormats
     */
    public List<String> getPixelFormats() {
        return pixelFormats;
    }

    /**
     * @param pixelFormats the pixelFormats to set
     */
    public void setPixelFormats(List<String> pixelFormats) {
        this.pixelFormats = pixelFormats.isEmpty() ? null : pixelFormats;
    }

    /**
     * @return the sampleRates
     */
    public List<Integer> getSampleRates() {
        return sampleRates;
    }

    /**
     * @param sampleRates the sampleRates to set
     */
    public void setSampleRates(List<Integer> sampleRates) {
        this.sampleRates = sampleRates.isEmpty() ? null : sampleRates;
    }

    /**
     * @return the sampleFormats
     */
    public List<String> getSampleFormats() {
        return sampleFormats;
    }

    /**
     * @param sampleFormats the sampleFormats to set
     */
    public void setSampleFormats(List<String> sampleFormats) {
        this.sampleFormats = sampleFormats.isEmpty() ? null : sampleFormats;
    }

    /**
     * @return the channelLayouts
     */
    public List<String> getChannelLayouts() {
        return channelLayouts;
    }

    /**
     * @param channelLayouts the channelLayouts to set
     */
    public void setChannelLayouts(List<String> channelLayouts) {
        this.channelLayouts = channelLayouts.isEmpty() ? null : channelLayouts;
    }

    /**
     * @return the parameters
     */
    public List<ParameterWrapper> getParameters() {
        return parameters;
    }

    /**
     * @param parameters the parameters to set
     */
    public void setParameters(List<ParameterWrapper> parameters) {
        this.parameters = parameters.isEmpty() ? null : parameters;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        final int maxLen = 10;
        StringBuilder builder = new StringBuilder();
        builder.append("EncoderWrapper [");
        if (name != null) {
            builder.append("name=");
            builder.append(name);
            builder.append(", ");
        }
        if (pixelFormats != null) {
            builder.append("pixelFormats=");
            builder.append(pixelFormats.subList(0, Math.min(pixelFormats.size(), maxLen)));
            builder.append(", ");
        }
        if (sampleRates != null) {
            builder.append("sampleRates=");
            builder.append(sampleRates.subList(0, Math.min(sampleRates.size(), maxLen)));
            builder.append(", ");
        }
        if (sampleFormats != null) {
            builder.append("sampleFormats=");
            builder.append(sampleFormats.subList(0, Math.min(sampleFormats.size(), maxLen)));
            builder.append(", ");
        }
        if (channelLayouts != null) {
            builder.append("channelLayouts=");
            builder.append(channelLayouts.subList(0, Math.min(channelLayouts.size(), maxLen)));
            builder.append(", ");
        }
        if (parameters != null) {
            builder.append("parameters=");
            builder.append(parameters.subList(0, Math.min(parameters.size(), maxLen)));
        }
        builder.append("]");
        return builder.toString();
    }
}
