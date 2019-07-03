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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import org.mycore.vidconv.frontend.entity.adapter.NVCoderAdapter;

/**
 * @author Ren\u00E9 Adler (eagle)
 *
 */
@XmlRootElement(name = "deviceSpec")
@XmlType(name = "nvidiaSpec")
public class HWAccelNvidiaSpec extends HWAccelDeviceSpec {

    private String name;

    private String family;

    private String chip;

    private int numChips;

    private int numEncoder;

    private int numDecoder;

    private Map<String, Boolean> encoders;

    private Map<String, Boolean> decoders;

    private final List<String> encProcessIds;

    private final List<String> decProcessIds;

    public HWAccelNvidiaSpec() {
        encProcessIds = Collections.synchronizedList(new ArrayList<>());
        decProcessIds = Collections.synchronizedList(new ArrayList<>());
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
     * @return the family
     */
    public String getFamily() {
        return family;
    }

    /**
     * @param family the family to set
     */
    public void setFamily(String family) {
        this.family = family;
    }

    /**
     * @return the chip
     */
    public String getChip() {
        return chip;
    }

    /**
     * @param chip the chip to set
     */
    public void setChip(String chip) {
        this.chip = chip;
    }

    /**
     * @return the numChips
     */
    public int getNumChips() {
        return numChips;
    }

    /**
     * @param numChips the numChips to set
     */
    public void setNumChips(int numChips) {
        this.numChips = numChips;
    }

    /**
     * @return the numEncoder
     */
    public int getNumEncoder() {
        return numEncoder;
    }

    /**
     * @param numEncoder the numEncoder to set
     */
    public void setNumEncoder(int numEncoder) {
        this.numEncoder = numEncoder;
    }

    /**
     * @return the numDecoder
     */
    public int getNumDecoder() {
        return numDecoder;
    }

    /**
     * @param numDecoder the numDecoder to set
     */
    public void setNumDecoder(int numDecoder) {
        this.numDecoder = numDecoder;
    }

    /**
     * @return the encoders
     */
    @XmlJavaTypeAdapter(NVCoderAdapter.class)
    public Map<String, Boolean> getEncoders() {
        return encoders;
    }

    /**
     * @param encoders the encoders to set
     */
    public void setEncoders(Map<String, Boolean> encoders) {
        this.encoders = encoders;
    }

    /**
     * @return the decoders
     */
    @XmlJavaTypeAdapter(NVCoderAdapter.class)
    public Map<String, Boolean> getDecoders() {
        return decoders;
    }

    /**
     * @param decoders the decoders to set
     */
    public void setDecoders(Map<String, Boolean> decoders) {
        this.decoders = decoders;
    }

    /**
     * @return the encProcessIds
     */
    public synchronized List<String> getEncProcessIds() {
        return encProcessIds;
    }

    /**
     * @return the decProcessIds
     */
    public synchronized List<String> getDecProcessIds() {
        return decProcessIds;
    }

    public synchronized boolean canUseEncoder() {
        return numEncoder > 0 && numEncoder > encProcessIds.size();
    }

    public synchronized boolean canUseEncoder(final String processId) {
        return encProcessIds.contains(processId);
    }

    public synchronized boolean canUseDecoder() {
        return numDecoder > 0 && numDecoder > decProcessIds.size();
    }

    public synchronized boolean canUseDecoder(final String processId) {
        return decProcessIds.contains(processId);
    }

    /* (non-Javadoc)
     * @see org.mycore.vidconv.frontend.entity.HWAccelDeviceSpec#numConcurrentProcesses()
     */
    @Override
    public int numConcurrentProcesses() {
        return Integer.max(numDecoder, numEncoder);
    }

    /* (non-Javadoc)
     * @see org.mycore.vidconv.frontend.entity.HWAccelDeviceSpec#registerProcessId(java.lang.String)
     */
    @Override
    public synchronized void registerProcessId(String processId) {
        registerEncoderProcessId(processId);
        registerDecoderProcessId(processId);
    }

    public synchronized void registerEncoderProcessId(String processId) {
        if (canUseEncoder() && !encProcessIds.contains(processId)) {
            encProcessIds.add(processId);
        }
    }

    public synchronized void registerDecoderProcessId(String processId) {
        if (canUseDecoder() && !decProcessIds.contains(processId)) {
            decProcessIds.add(processId);
        }
    }

    /* (non-Javadoc)
     * @see org.mycore.vidconv.frontend.entity.HWAccelDeviceSpec#unregisterProcessId(java.lang.String)
     */
    @Override
    public synchronized void unregisterProcessId(String processId) {
        encProcessIds.remove(processId);
        decProcessIds.remove(processId);
    }

    /* (non-Javadoc)
     * @see java.lang.Comparable#compareTo(java.lang.Object)
     */
    @Override
    public int compareTo(HWAccelDeviceSpec o) {
        if (o == null || !(o instanceof HWAccelNvidiaSpec)) {
            return -1;
        }

        HWAccelNvidiaSpec os = (HWAccelNvidiaSpec) o;

        if (decProcessIds.size() < os.decProcessIds.size()) {
            return -1;
        } else if (decProcessIds.size() > os.decProcessIds.size()) {
            return 1;
        } else if (encProcessIds.size() < os.encProcessIds.size()) {
            return -1;
        } else if (encProcessIds.size() > os.encProcessIds.size()) {
            return 1;
        }

        return 0;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        final int maxLen = 10;
        StringBuilder builder = new StringBuilder();
        builder.append("HWAccelNvidiaSpec [");
        if (name != null) {
            builder.append("name=");
            builder.append(name);
            builder.append(", ");
        }
        if (family != null) {
            builder.append("family=");
            builder.append(family);
            builder.append(", ");
        }
        if (chip != null) {
            builder.append("chip=");
            builder.append(chip);
            builder.append(", ");
        }
        builder.append("numChips=");
        builder.append(numChips);
        builder.append(", numEncoder=");
        builder.append(numEncoder);
        builder.append(", numDecoder=");
        builder.append(numDecoder);
        builder.append(", ");
        if (encoders != null) {
            builder.append("encoders=");
            builder.append(toString(encoders.entrySet(), maxLen));
            builder.append(", ");
        }
        if (decoders != null) {
            builder.append("decoders=");
            builder.append(toString(decoders.entrySet(), maxLen));
        }
        builder.append("]");
        return builder.toString();
    }

    private String toString(Collection<?> collection, int maxLen) {
        StringBuilder builder = new StringBuilder();
        builder.append("[");
        int i = 0;
        for (Iterator<?> iterator = collection.iterator(); iterator.hasNext() && i < maxLen; i++) {
            if (i > 0)
                builder.append(", ");
            builder.append(iterator.next());
        }
        builder.append("]");
        return builder.toString();
    }

    /* (non-Javadoc)
     * @see java.lang.Object#clone()
     */
    @Override
    protected HWAccelNvidiaSpec clone() {
        HWAccelNvidiaSpec copy = new HWAccelNvidiaSpec();

        copy.setName(getName());
        copy.setFamily(getFamily());
        copy.setChip(getChip());
        copy.setNumChips(getNumChips());
        copy.setEncoders(getEncoders());
        copy.setNumEncoder(getNumEncoder());
        copy.setDecoders(getDecoders());
        copy.setNumDecoder(getNumDecoder());

        return copy;
    }

    /* (non-Javadoc)
     * @see org.mycore.vidconv.frontend.entity.HWAccelDeviceSpec#basicCopy()
     */
    @SuppressWarnings("unchecked")
    @Override
    public HWAccelNvidiaSpec basicCopy() {
        HWAccelNvidiaSpec copy = new HWAccelNvidiaSpec();

        copy.setName(getName());
        copy.setFamily(getFamily());
        copy.setChip(getChip());
        copy.setNumChips(getNumChips());
        copy.setNumEncoder(getNumEncoder());
        copy.setNumDecoder(getNumDecoder());

        return copy;
    }

}
