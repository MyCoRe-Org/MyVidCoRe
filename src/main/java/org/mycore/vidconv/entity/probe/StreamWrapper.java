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
package org.mycore.vidconv.entity.probe;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * @author Ren\u00E9 Adler (eagle)
 *
 */
@XmlRootElement(name = "stream")
@XmlAccessorType(XmlAccessType.FIELD)
public class StreamWrapper {

    @XmlAttribute(name = "color_space")
    private String colorSpace;

    @XmlAttribute(name = "pix_fmt")
    private String pixelFormat;

    @XmlAttribute(name = "chroma_location")
    private String chromaLocation;

    @XmlAttribute(name = "bits_per_raw_sample")
    private String bitsPerRawSample;

    @XmlAttribute(name = "codec_type")
    private String codecType;

    @XmlAttribute(name = "level")
    private String level;

    @XmlAttribute(name = "height")
    private String height;

    @XmlAttribute(name = "start_pts")
    private String startPts;

    @XmlAttribute(name = "color_transfer")
    private String colorTransfer;

    @XmlAttribute(name = "codec_time_base")
    private String codecTimeBase;

    @XmlAttribute(name = "color_range")
    private String colorRange;

    @XmlElement
    private DispositionWrapper disposition;

    @XmlAttribute(name = "profile")
    private String profile;

    @XmlAttribute(name = "color_primaries")
    private String colorPrimaries;

    @XmlAttribute(name = "index")
    private String index;

    @XmlAttribute(name = "codec_long_name")
    private String codecLongName;

    @XmlAttribute(name = "width")
    private String width;

    @XmlAttribute(name = "time_base")
    private String timeBase;

    @XmlAttribute(name = "coded_width")
    private String codedWidth;

    @XmlAttribute(name = "codec_name")
    private String codecName;

    @XmlAttribute(name = "sample_aspect_ratio")
    private String sampleAspectRatio;

    @XmlAttribute(name = "avg_frame_rate")
    private String avgFrameRate;

    @XmlAttribute(name = "is_avc")
    private String isAVC;

    @XmlAttribute(name = "codec_tag")
    private String codecTag;

    @XmlAttribute(name = "refs")
    private String refs;

    @XmlAttribute(name = "nal_length_size")
    private String nalLengthSize;

    @XmlAttribute(name = "start_time")
    private String startTime;

    @XmlAttribute(name = "display_aspect_ratio")
    private String displayAspectRatio;

    @XmlAttribute(name = "has_b_frames")
    private String hasBFrames;

    @XmlAttribute(name = "r_frame_rate")
    private String rFrameRate;

    @XmlAttribute(name = "codec_tag_string")
    private String codecTagString;

    @XmlAttribute(name = "coded_height")
    private String codedHeight;

    /**
     * @return the colorSpace
     */
    public String getColorSpace() {
        return colorSpace;
    }

    /**
     * @param colorSpace the colorSpace to set
     */
    public void setColorSpace(String colorSpace) {
        this.colorSpace = colorSpace;
    }

    /**
     * @return the pixelFormat
     */
    public String getPixelFormat() {
        return pixelFormat;
    }

    /**
     * @param pixelFormat the pixelFormat to set
     */
    public void setPixelFormat(String pixelFormat) {
        this.pixelFormat = pixelFormat;
    }

    /**
     * @return the chromaLocation
     */
    public String getChromaLocation() {
        return chromaLocation;
    }

    /**
     * @param chromaLocation the chromaLocation to set
     */
    public void setChromaLocation(String chromaLocation) {
        this.chromaLocation = chromaLocation;
    }

    /**
     * @return the bitsPerRawSample
     */
    public String getBitsPerRawSample() {
        return bitsPerRawSample;
    }

    /**
     * @param bitsPerRawSample the bitsPerRawSample to set
     */
    public void setBitsPerRawSample(String bitsPerRawSample) {
        this.bitsPerRawSample = bitsPerRawSample;
    }

    /**
     * @return the codecType
     */
    public String getCodecType() {
        return codecType;
    }

    /**
     * @param codecType the codecType to set
     */
    public void setCodecType(String codecType) {
        this.codecType = codecType;
    }

    /**
     * @return the level
     */
    public String getLevel() {
        return level;
    }

    /**
     * @param level the level to set
     */
    public void setLevel(String level) {
        this.level = level;
    }

    /**
     * @return the height
     */
    public String getHeight() {
        return height;
    }

    /**
     * @param height the height to set
     */
    public void setHeight(String height) {
        this.height = height;
    }

    /**
     * @return the startPts
     */
    public String getStartPts() {
        return startPts;
    }

    /**
     * @param startPts the startPts to set
     */
    public void setStartPts(String startPts) {
        this.startPts = startPts;
    }

    /**
     * @return the colorTransfer
     */
    public String getColorTransfer() {
        return colorTransfer;
    }

    /**
     * @param colorTransfer the colorTransfer to set
     */
    public void setColorTransfer(String colorTransfer) {
        this.colorTransfer = colorTransfer;
    }

    /**
     * @return the codecTimeBase
     */
    public String getCodecTimeBase() {
        return codecTimeBase;
    }

    /**
     * @param codecTimeBase the codecTimeBase to set
     */
    public void setCodecTimeBase(String codecTimeBase) {
        this.codecTimeBase = codecTimeBase;
    }

    /**
     * @return the colorRange
     */
    public String getColorRange() {
        return colorRange;
    }

    /**
     * @param colorRange the colorRange to set
     */
    public void setColorRange(String colorRange) {
        this.colorRange = colorRange;
    }

    /**
     * @return the disposition
     */
    public DispositionWrapper getDisposition() {
        return disposition;
    }

    /**
     * @param disposition the disposition to set
     */
    public void setDisposition(DispositionWrapper disposition) {
        this.disposition = disposition;
    }

    /**
     * @return the profile
     */
    public String getProfile() {
        return profile;
    }

    /**
     * @param profile the profile to set
     */
    public void setProfile(String profile) {
        this.profile = profile;
    }

    /**
     * @return the colorPrimaries
     */
    public String getColorPrimaries() {
        return colorPrimaries;
    }

    /**
     * @param colorPrimaries the colorPrimaries to set
     */
    public void setColorPrimaries(String colorPrimaries) {
        this.colorPrimaries = colorPrimaries;
    }

    /**
     * @return the index
     */
    public String getIndex() {
        return index;
    }

    /**
     * @param index the index to set
     */
    public void setIndex(String index) {
        this.index = index;
    }

    /**
     * @return the codecLongName
     */
    public String getCodecLongName() {
        return codecLongName;
    }

    /**
     * @param codecLongName the codecLongName to set
     */
    public void setCodecLongName(String codecLongName) {
        this.codecLongName = codecLongName;
    }

    /**
     * @return the width
     */
    public String getWidth() {
        return width;
    }

    /**
     * @param width the width to set
     */
    public void setWidth(String width) {
        this.width = width;
    }

    /**
     * @return the timeBase
     */
    public String getTimeBase() {
        return timeBase;
    }

    /**
     * @param timeBase the timeBase to set
     */
    public void setTimeBase(String timeBase) {
        this.timeBase = timeBase;
    }

    /**
     * @return the codedWidth
     */
    public String getCodedWidth() {
        return codedWidth;
    }

    /**
     * @param codedWidth the codedWidth to set
     */
    public void setCodedWidth(String codedWidth) {
        this.codedWidth = codedWidth;
    }

    /**
     * @return the codecName
     */
    public String getCodecName() {
        return codecName;
    }

    /**
     * @param codecName the codecName to set
     */
    public void setCodecName(String codecName) {
        this.codecName = codecName;
    }

    /**
     * @return the sampleAspectRatio
     */
    public String getSampleAspectRatio() {
        return sampleAspectRatio;
    }

    /**
     * @param sampleAspectRatio the sampleAspectRatio to set
     */
    public void setSampleAspectRatio(String sampleAspectRatio) {
        this.sampleAspectRatio = sampleAspectRatio;
    }

    /**
     * @return the avgFrameRate
     */
    public String getAvgFrameRate() {
        return avgFrameRate;
    }

    /**
     * @param avgFrameRate the avgFrameRate to set
     */
    public void setAvgFrameRate(String avgFrameRate) {
        this.avgFrameRate = avgFrameRate;
    }

    /**
     * @return the isAVC
     */
    public String getIsAVC() {
        return isAVC;
    }

    /**
     * @param isAVC the isAVC to set
     */
    public void setIsAVC(String isAVC) {
        this.isAVC = isAVC;
    }

    /**
     * @return the codecTag
     */
    public String getCodecTag() {
        return codecTag;
    }

    /**
     * @param codecTag the codecTag to set
     */
    public void setCodecTag(String codecTag) {
        this.codecTag = codecTag;
    }

    /**
     * @return the refs
     */
    public String getRefs() {
        return refs;
    }

    /**
     * @param refs the refs to set
     */
    public void setRefs(String refs) {
        this.refs = refs;
    }

    /**
     * @return the nalLengthSize
     */
    public String getNalLengthSize() {
        return nalLengthSize;
    }

    /**
     * @param nalLengthSize the nalLengthSize to set
     */
    public void setNalLengthSize(String nalLengthSize) {
        this.nalLengthSize = nalLengthSize;
    }

    /**
     * @return the startTime
     */
    public String getStartTime() {
        return startTime;
    }

    /**
     * @param startTime the startTime to set
     */
    public void setStartTime(String startTime) {
        this.startTime = startTime;
    }

    /**
     * @return the displayAspectRatio
     */
    public String getDisplayAspectRatio() {
        return displayAspectRatio;
    }

    /**
     * @param displayAspectRatio the displayAspectRatio to set
     */
    public void setDisplayAspectRatio(String displayAspectRatio) {
        this.displayAspectRatio = displayAspectRatio;
    }

    /**
     * @return the hasBFrames
     */
    public String getHasBFrames() {
        return hasBFrames;
    }

    /**
     * @param hasBFrames the hasBFrames to set
     */
    public void setHasBFrames(String hasBFrames) {
        this.hasBFrames = hasBFrames;
    }

    /**
     * @return the rFrameRate
     */
    public String getrFrameRate() {
        return rFrameRate;
    }

    /**
     * @param rFrameRate the rFrameRate to set
     */
    public void setrFrameRate(String rFrameRate) {
        this.rFrameRate = rFrameRate;
    }

    /**
     * @return the codecTagString
     */
    public String getCodecTagString() {
        return codecTagString;
    }

    /**
     * @param codecTagString the codecTagString to set
     */
    public void setCodecTagString(String codecTagString) {
        this.codecTagString = codecTagString;
    }

    /**
     * @return the codedHeight
     */
    public String getCodedHeight() {
        return codedHeight;
    }

    /**
     * @param codedHeight the codedHeight to set
     */
    public void setCodedHeight(String codedHeight) {
        this.codedHeight = codedHeight;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("StreamWrapper [");
        if (colorSpace != null) {
            builder.append("colorSpace=");
            builder.append(colorSpace);
            builder.append(", ");
        }
        if (pixelFormat != null) {
            builder.append("pixelFormat=");
            builder.append(pixelFormat);
            builder.append(", ");
        }
        if (chromaLocation != null) {
            builder.append("chromaLocation=");
            builder.append(chromaLocation);
            builder.append(", ");
        }
        if (bitsPerRawSample != null) {
            builder.append("bitsPerRawSample=");
            builder.append(bitsPerRawSample);
            builder.append(", ");
        }
        if (codecType != null) {
            builder.append("codecType=");
            builder.append(codecType);
            builder.append(", ");
        }
        if (level != null) {
            builder.append("level=");
            builder.append(level);
            builder.append(", ");
        }
        if (height != null) {
            builder.append("height=");
            builder.append(height);
            builder.append(", ");
        }
        if (startPts != null) {
            builder.append("startPts=");
            builder.append(startPts);
            builder.append(", ");
        }
        if (colorTransfer != null) {
            builder.append("colorTransfer=");
            builder.append(colorTransfer);
            builder.append(", ");
        }
        if (codecTimeBase != null) {
            builder.append("codecTimeBase=");
            builder.append(codecTimeBase);
            builder.append(", ");
        }
        if (colorRange != null) {
            builder.append("colorRange=");
            builder.append(colorRange);
            builder.append(", ");
        }
        if (disposition != null) {
            builder.append("disposition=");
            builder.append(disposition);
            builder.append(", ");
        }
        if (profile != null) {
            builder.append("profile=");
            builder.append(profile);
            builder.append(", ");
        }
        if (colorPrimaries != null) {
            builder.append("colorPrimaries=");
            builder.append(colorPrimaries);
            builder.append(", ");
        }
        if (index != null) {
            builder.append("index=");
            builder.append(index);
            builder.append(", ");
        }
        if (codecLongName != null) {
            builder.append("codecLongName=");
            builder.append(codecLongName);
            builder.append(", ");
        }
        if (width != null) {
            builder.append("width=");
            builder.append(width);
            builder.append(", ");
        }
        if (timeBase != null) {
            builder.append("timeBase=");
            builder.append(timeBase);
            builder.append(", ");
        }
        if (codedWidth != null) {
            builder.append("codedWidth=");
            builder.append(codedWidth);
            builder.append(", ");
        }
        if (codecName != null) {
            builder.append("codecName=");
            builder.append(codecName);
            builder.append(", ");
        }
        if (sampleAspectRatio != null) {
            builder.append("sampleAspectRatio=");
            builder.append(sampleAspectRatio);
            builder.append(", ");
        }
        if (avgFrameRate != null) {
            builder.append("avgFrameRate=");
            builder.append(avgFrameRate);
            builder.append(", ");
        }
        if (isAVC != null) {
            builder.append("isAVC=");
            builder.append(isAVC);
            builder.append(", ");
        }
        if (codecTag != null) {
            builder.append("codecTag=");
            builder.append(codecTag);
            builder.append(", ");
        }
        if (refs != null) {
            builder.append("refs=");
            builder.append(refs);
            builder.append(", ");
        }
        if (nalLengthSize != null) {
            builder.append("nalLengthSize=");
            builder.append(nalLengthSize);
            builder.append(", ");
        }
        if (startTime != null) {
            builder.append("startTime=");
            builder.append(startTime);
            builder.append(", ");
        }
        if (displayAspectRatio != null) {
            builder.append("displayAspectRatio=");
            builder.append(displayAspectRatio);
            builder.append(", ");
        }
        if (hasBFrames != null) {
            builder.append("hasBFrames=");
            builder.append(hasBFrames);
            builder.append(", ");
        }
        if (rFrameRate != null) {
            builder.append("rFrameRate=");
            builder.append(rFrameRate);
            builder.append(", ");
        }
        if (codecTagString != null) {
            builder.append("codecTagString=");
            builder.append(codecTagString);
            builder.append(", ");
        }
        if (codedHeight != null) {
            builder.append("codedHeight=");
            builder.append(codedHeight);
        }
        builder.append("]");
        return builder.toString();
    }

}
