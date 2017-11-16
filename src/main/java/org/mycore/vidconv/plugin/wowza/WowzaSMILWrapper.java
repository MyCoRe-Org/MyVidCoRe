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
package org.mycore.vidconv.plugin.wowza;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import org.mycore.vidconv.frontend.entity.probe.ProbeWrapper;
import org.mycore.vidconv.frontend.entity.probe.StreamWrapper;

/**
 * @author Ren\u00E9 Adler (eagle)
 * @param <T>
 *
 */
@XmlRootElement(name = "smil")
public class WowzaSMILWrapper {

    private String title;

    private Body body;

    public static WowzaSMILWrapper build(final List<ProbeWrapper> probes) {
        final WowzaSMILWrapper smil = new WowzaSMILWrapper();

        final List<Video> videos = new ArrayList<>();

        probes.forEach(p -> {
            if (p.getFormat() != null && p.getStreams() != null) {
                final StreamWrapper vs = p.getStreams().stream().filter(s -> s.getCodecType().equalsIgnoreCase("video"))
                    .findFirst().orElse(null);
                final Optional<StreamWrapper> as = p.getStreams().stream()
                    .filter(s -> s.getCodecType().equalsIgnoreCase("audio")).findFirst();
                if (vs != null) {
                    final Video video = new Video();

                    video.setSrc(Paths.get(p.getFormat().getFilename()).getFileName().toString());
                    video.setSystemLanguage("eng");
                    video.setHeight(vs.getHeight());
                    video.setWidth(vs.getWidth());

                    video.withParam(new Param<Integer>("videoBitrate", vs.getBitRate(), "data"));
                    as.ifPresent(s -> video.withParam(new Param<Integer>("audioBitrate", s.getBitRate(), "data")));

                    videos.add(video);
                }
            }
        });

        smil.setBody(new Body(videos));

        return smil;
    }

    public static void saveTo(final Path outputPath, final List<ProbeWrapper> probes) throws JAXBException {
        final JAXBContext jc = JAXBContext.newInstance(WowzaSMILWrapper.class);
        final Marshaller marshaller = jc.createMarshaller();
        marshaller.marshal(WowzaSMILWrapper.build(probes), outputPath.toFile());
    }

    /**
     * @return the title
     */
    @XmlAttribute(name = "title")
    public String getTitle() {
        return title;
    }

    /**
     * @param title the title to set
     */
    public void setTitle(String title) {
        this.title = title;
    }

    /**
     * @return the body
     */
    @XmlElement(name = "body")
    public Body getBody() {
        return body;
    }

    /**
     * @param body the body to set
     */
    public void setBody(Body body) {
        this.body = body;
    }

    @XmlRootElement(name = "body")
    @XmlType(name = "SMIL.Body")
    public static class Body {

        private List<Video> switchBody;

        protected Body() {
            super();
        }

        public Body(List<Video> switchBody) {
            this.switchBody = switchBody;
        }

        /**
         * @return the switchBody
         */
        @XmlElementWrapper(name = "switch")
        @XmlElement(name = "video")
        public List<Video> getSwitchBody() {
            return switchBody;
        }

        /**
         * @param switchBody the switchBody to set
         */
        public void setSwitchBody(List<Video> switchBody) {
            this.switchBody = switchBody;
        }
    }

    @XmlRootElement(name = "video")
    @XmlType(name = "SMIL.Video")
    public static class Video {
        private String src;

        private String systemLanguage;

        private Integer height;

        private Integer width;

        private List<Param<?>> params;

        /**
         * @return the src
         */
        @XmlAttribute(name = "src")
        public String getSrc() {
            return src;
        }

        /**
         * @param src the src to set
         */
        public void setSrc(String src) {
            this.src = src;
        }

        /**
         * @return the systemLanguage
         */
        @XmlAttribute(name = "systemLanguage")
        public String getSystemLanguage() {
            return systemLanguage;
        }

        /**
         * @param systemLanguage the systemLanguage to set
         */
        public void setSystemLanguage(String systemLanguage) {
            this.systemLanguage = systemLanguage;
        }

        /**
         * @return the height
         */
        @XmlAttribute(name = "height")
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
         * @return the width
         */
        @XmlAttribute(name = "width")
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
         * @return the params
         */
        @XmlElement(name = "param")
        public List<Param<?>> getParams() {
            return params;
        }

        /**
         * @param params the params to set
         */
        public void setParams(List<Param<?>> params) {
            this.params = params;
        }

        public Video withParam(Param<?> param) {
            if (this.params == null)
                this.params = new ArrayList<>();

            this.params.add(param);
            return this;
        }

    }

    @XmlRootElement(name = "param")
    @XmlType(name = "SMIL.Param")
    public static class Param<T> {
        private String name;

        private T value;

        private String valueType;

        protected Param() {
            super();
        }

        /**
         * @param name
         * @param value
         * @param valueType
         */
        public Param(String name, T value, String valueType) {
            super();
            this.name = name;
            this.value = value;
            this.valueType = valueType;
        }

        /**
         * @return the name
         */
        @XmlAttribute(name = "name")
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
         * @return the value
         */
        @XmlAttribute(name = "value")
        public T getValue() {
            return value;
        }

        /**
         * @param value the value to set
         */
        public void setValue(T value) {
            this.value = value;
        }

        /**
         * @return the valueType
         */
        @XmlAttribute(name = "valuetype")
        public String getValueType() {
            return valueType;
        }

        /**
         * @param valueType the valueType to set
         */
        public void setValueType(String valueType) {
            this.valueType = valueType;
        }
    }
}
