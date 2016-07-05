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

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * @author Ren\u00E9 Adler (eagle)
 *
 */
@XmlRootElement(name = "settings")
public class SettingsWrapper {

    private String format;

    private Video video;

    private Audio audio;

    public SettingsWrapper() {
    }

    /**
     * @return the format
     */
    @XmlElement
    public String getFormat() {
        return format;
    }

    /**
     * @return the video
     */
    @XmlElement
    public Video getVideo() {
        return video;
    }

    /**
     * @return the audio
     */
    @XmlElement
    public Audio getAudio() {
        return audio;
    }

    /**
     * @param format the format to set
     */
    public void setFormat(String format) {
        this.format = format;
    }

    /**
     * @param video the video to set
     */
    public void setVideo(Video video) {
        this.video = video;
    }

    /**
     * @param audio the audio to set
     */
    public void setAudio(Audio audio) {
        this.audio = audio;
    }

    @XmlRootElement(name = "video")
    public static class Video {
        private String codec;

        private Float framerate;

        private String framerateType;

        private String profile;

        private String level;

        private String pixelFormat;

        private Quality quality;

        public Video() {
        }

        /**
         * @return the codec
         */
        public String getCodec() {
            return codec;
        }

        /**
         * @return the framerate
         */
        public Float getFramerate() {
            return framerate;
        }

        /**
         * @return the framerateType
         */
        public String getFramerateType() {
            return framerateType;
        }

        /**
         * @return the profile
         */
        public String getProfile() {
            return profile;
        }

        /**
         * @return the level
         */
        public String getLevel() {
            return level;
        }

        /**
         * @return the quality
         */
        public Quality getQuality() {
            return quality;
        }

        /**
         * @param codec the codec to set
         */
        public void setCodec(String codec) {
            this.codec = codec;
        }

        /**
         * @param framerate the framerate to set
         */
        public void setFramerate(Float framerate) {
            this.framerate = framerate;
        }

        /**
         * @param framerateType the framerateType to set
         */
        public void setFramerateType(String framerateType) {
            this.framerateType = framerateType;
        }

        /**
         * @param profile the profile to set
         */
        public void setProfile(String profile) {
            this.profile = profile;
        }

        /**
         * @param level the level to set
         */
        public void setLevel(String level) {
            this.level = level;
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
         * @param quality the quality to set
         */
        public void setQuality(Quality quality) {
            this.quality = quality;
        }

        @XmlRootElement(name = "quality")
        public static class Quality {
            private String type;

            private Float rateFactor;

            private Integer bitrate;

            public Quality() {
            }

            /**
             * @return the type
             */
            public String getType() {
                return type;
            }

            /**
             * @return the bitrate
             */
            public Integer getBitrate() {
                return bitrate;
            }

            /**
             * @param type the type to set
             */
            public void setType(String type) {
                this.type = type;
            }

            /**
             * @param bitrate the bitrate to set
             */
            public void setBitrate(Integer bitrate) {
                this.bitrate = bitrate;
            }

            /**
             * @return the rateFactor
             */
            public Float getRateFactor() {
                return rateFactor;
            }

            /**
             * @param rateFactor the rateFactor to set
             */
            public void setRateFactor(Float rateFactor) {
                this.rateFactor = rateFactor;
            }

        }
    }

    @XmlRootElement(name = "audio")
    public static class Audio {
        private String codec;

        private String mixdown;

        private Integer samplerate;

        private Integer bitrate;

        public Audio() {
        }

        /**
         * @return the codec
         */
        public String getCodec() {
            return codec;
        }

        /**
         * @return the mixdown
         */
        public String getMixdown() {
            return mixdown;
        }

        /**
         * @return the samplerate
         */
        public Integer getSamplerate() {
            return samplerate;
        }

        /**
         * @return the bitrate
         */
        public Integer getBitrate() {
            return bitrate;
        }

        /**
         * @param codec the codec to set
         */
        public void setCodec(String codec) {
            this.codec = codec;
        }

        /**
         * @param mixdown the mixdown to set
         */
        public void setMixdown(String mixdown) {
            this.mixdown = mixdown;
        }

        /**
         * @param samplerate the samplerate to set
         */
        public void setSamplerate(Integer samplerate) {
            this.samplerate = samplerate;
        }

        /**
         * @param bitrate the bitrate to set
         */
        public void setBitrate(Integer bitrate) {
            this.bitrate = bitrate;
        }

    }
}
