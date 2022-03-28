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
package org.mycore.vidconv.frontend.entity.probe;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;

/**
 * @author Ren\u00E9 Adler (eagle)
 *
 */
@XmlRootElement(name = "stream")
@XmlAccessorType(XmlAccessType.FIELD)
public class StreamWrapper {

    @XmlAttribute(name = "avg_frame_rate")
    private String avgFrameRate;

    @XmlAttribute(name = "bit_rate")
    private Integer bitRate;

    @XmlAttribute(name = "bits_per_raw_sample")
    private Integer bitsPerRawSample;

    @XmlAttribute(name = "bits_per_sample")
    private Integer bitsPerSample;

    @XmlAttribute(name = "channel_layout")
    private String channelLayout;

    @XmlAttribute(name = "channels")
    private Integer channels;

    @XmlAttribute(name = "chroma_location")
    private String chromaLocation;

    @XmlAttribute(name = "codec_long_name")
    private String codecLongName;

    @XmlAttribute(name = "codec_name")
    private String codecName;

    @XmlAttribute(name = "codec_tag")
    private String codecTag;

    @XmlAttribute(name = "codec_tag_string")
    private String codecTagString;

    @XmlAttribute(name = "codec_time_base")
    private String codecTimeBase;

    @XmlAttribute(name = "codec_type")
    private String codecType;

    @XmlAttribute(name = "coded_height")
    private Integer codedHeight;

    @XmlAttribute(name = "coded_width")
    private Integer codedWidth;

    @XmlAttribute(name = "display_aspect_ratio")
    private String displayAspectRatio;

    @XmlAttribute(name = "duration")
    private String duration;

    @XmlAttribute(name = "duration_ts")
    private Integer durationTs;

    @XmlAttribute(name = "has_b_frames")
    private Integer hasBFrames;

    @XmlAttribute(name = "height")
    private Integer height;

    @XmlAttribute(name = "index")
    private Integer index;

    @XmlAttribute(name = "is_avc")
    private String isAvc;

    @XmlAttribute(name = "level")
    private Integer level;

    @XmlAttribute(name = "nal_length_size")
    private Integer nalLengthSize;

    @XmlAttribute(name = "nb_frames")
    private Integer nbFrames;

    @XmlAttribute(name = "pix_fmt")
    private String pixFmt;

    @XmlAttribute(name = "profile")
    private String profile;

    @XmlAttribute(name = "r_frame_rate")
    private String rFrameRate;

    @XmlAttribute(name = "refs")
    private Integer refs;

    @XmlAttribute(name = "sample_aspect_ratio")
    private String sampleAspectRatio;

    @XmlAttribute(name = "sample_fmt")
    private String sampleFmt;

    @XmlAttribute(name = "sample_rate")
    private Integer sampleRate;

    @XmlAttribute(name = "start_pts")
    private String startPts;

    @XmlAttribute(name = "start_time")
    private String startTime;

    @XmlAttribute(name = "time_base")
    private String timeBase;

    @XmlAttribute(name = "width")
    private Integer width;

    @XmlElement
    private DispositionWrapper disposition;

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
     * @return the bitRate
     */
    public Integer getBitRate() {
        return bitRate;
    }

    /**
     * @param bitRate the bitRate to set
     */
    public void setBitRate(Integer bitRate) {
        this.bitRate = bitRate;
    }

    /**
     * @return the bitsPerRawSample
     */
    public Integer getBitsPerRawSample() {
        return bitsPerRawSample;
    }

    /**
     * @param bitsPerRawSample the bitsPerRawSample to set
     */
    public void setBitsPerRawSample(Integer bitsPerRawSample) {
        this.bitsPerRawSample = bitsPerRawSample;
    }

    /**
     * @return the bitsPerSample
     */
    public Integer getBitsPerSample() {
        return bitsPerSample;
    }

    /**
     * @param bitsPerSample the bitsPerSample to set
     */
    public void setBitsPerSample(Integer bitsPerSample) {
        this.bitsPerSample = bitsPerSample;
    }

    /**
     * @return the channelLayout
     */
    public String getChannelLayout() {
        return channelLayout;
    }

    /**
     * @param channelLayout the channelLayout to set
     */
    public void setChannelLayout(String channelLayout) {
        this.channelLayout = channelLayout;
    }

    /**
     * @return the channels
     */
    public Integer getChannels() {
        return channels;
    }

    /**
     * @param channels the channels to set
     */
    public void setChannels(Integer channels) {
        this.channels = channels;
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
     * @return the codedHeight
     */
    public Integer getCodedHeight() {
        return codedHeight;
    }

    /**
     * @param codedHeight the codedHeight to set
     */
    public void setCodedHeight(Integer codedHeight) {
        this.codedHeight = codedHeight;
    }

    /**
     * @return the codedWidth
     */
    public Integer getCodedWidth() {
        return codedWidth;
    }

    /**
     * @param codedWidth the codedWidth to set
     */
    public void setCodedWidth(Integer codedWidth) {
        this.codedWidth = codedWidth;
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
     * @return the duration
     */
    public String getDuration() {
        return duration;
    }

    /**
     * @param duration the duration to set
     */
    public void setDuration(String duration) {
        this.duration = duration;
    }

    /**
     * @return the durationTs
     */
    public Integer getDurationTs() {
        return durationTs;
    }

    /**
     * @param durationTs the durationTs to set
     */
    public void setDurationTs(Integer durationTs) {
        this.durationTs = durationTs;
    }

    /**
     * @return the hasBFrames
     */
    public Integer getHasBFrames() {
        return hasBFrames;
    }

    /**
     * @param hasBFrames the hasBFrames to set
     */
    public void setHasBFrames(Integer hasBFrames) {
        this.hasBFrames = hasBFrames;
    }

    /**
     * @return the height
     */
    public Integer getHeight() {
        return height;
    }

    /**
     * @param height the height to set
     */
    public void setHeight(Integer height) {
        this.height = height;
    }

    /**
     * @return the index
     */
    public Integer getIndex() {
        return index;
    }

    /**
     * @param index the index to set
     */
    public void setIndex(Integer index) {
        this.index = index;
    }

    /**
     * @return the isAvc
     */
    public String getIsAvc() {
        return isAvc;
    }

    /**
     * @param isAvc the isAvc to set
     */
    public void setIsAvc(String isAvc) {
        this.isAvc = isAvc;
    }

    /**
     * @return the level
     */
    public Integer getLevel() {
        return level;
    }

    /**
     * @param level the level to set
     */
    public void setLevel(Integer level) {
        this.level = level;
    }

    /**
     * @return the nalLengthSize
     */
    public Integer getNalLengthSize() {
        return nalLengthSize;
    }

    /**
     * @param nalLengthSize the nalLengthSize to set
     */
    public void setNalLengthSize(Integer nalLengthSize) {
        this.nalLengthSize = nalLengthSize;
    }

    /**
     * @return the nbFrames
     */
    public Integer getNbFrames() {
        return nbFrames;
    }

    /**
     * @param nbFrames the nbFrames to set
     */
    public void setNbFrames(Integer nbFrames) {
        this.nbFrames = nbFrames;
    }

    /**
     * @return the pixFmt
     */
    public String getPixFmt() {
        return pixFmt;
    }

    /**
     * @param pixFmt the pixFmt to set
     */
    public void setPixFmt(String pixFmt) {
        this.pixFmt = pixFmt;
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
     * @return the refs
     */
    public Integer getRefs() {
        return refs;
    }

    /**
     * @param refs the refs to set
     */
    public void setRefs(Integer refs) {
        this.refs = refs;
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
     * @return the sampleFmt
     */
    public String getSampleFmt() {
        return sampleFmt;
    }

    /**
     * @param sampleFmt the sampleFmt to set
     */
    public void setSampleFmt(String sampleFmt) {
        this.sampleFmt = sampleFmt;
    }

    /**
     * @return the sampleRate
     */
    public Integer getSampleRate() {
        return sampleRate;
    }

    /**
     * @param sampleRate the sampleRate to set
     */
    public void setSampleRate(Integer sampleRate) {
        this.sampleRate = sampleRate;
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
     * @return the width
     */
    public Integer getWidth() {
        return width;
    }

    /**
     * @param width the width to set
     */
    public void setWidth(Integer width) {
        this.width = width;
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

}
