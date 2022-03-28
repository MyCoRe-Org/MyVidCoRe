/*
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
package org.mycore.vidconv.plugin.subtitle;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;
import javax.xml.bind.JAXBException;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import org.mycore.vidconv.common.config.Configuration;
import org.mycore.vidconv.common.util.JsonUtils;
import org.vosk.LibVosk;
import org.vosk.LogLevel;
import org.vosk.Model;
import org.vosk.Recognizer;

/**
 * @author Ren\u00E9 Adler (eagle)
 *
 */
public class VoskExtractor {

    private static final float DEFAULT_SAMPLE_RATE = 16000;

    private final String modelPath;

    private final float sampleRate;

    public VoskExtractor() {
        this(null, DEFAULT_SAMPLE_RATE);
    }

    public VoskExtractor(float sampleRate) {
        this(null, sampleRate);
    }

    public VoskExtractor(String modelPath, float sampleRate) {
        this.modelPath = Optional.ofNullable(modelPath)
                .orElseGet(() -> Configuration.instance().getString("VoskExtractor.modelPath"));
        this.sampleRate = sampleRate;
    }

    public List<VoskResult> extract(Path input) throws IOException, UnsupportedAudioFileException {
        LibVosk.setLogLevel(LogLevel.WARNINGS);

        List<VoskResult> results = new ArrayList<>();

        try (Model model = new Model(modelPath);
                InputStream is = Files.newInputStream(input);
                InputStream ais = AudioSystem.getAudioInputStream(new BufferedInputStream(is));
                Recognizer recognizer = new Recognizer(model, sampleRate)) {
            recognizer.setWords(true);

            int nbytes;
            byte[] b = new byte[(int) (sampleRate / 2)];
            while ((nbytes = ais.read(b)) >= 0) {
                if (recognizer.acceptWaveForm(b, nbytes)) {
                    results.add(unmarshall(recognizer.getResult()));
                }
            }

            results.add(unmarshall(recognizer.getFinalResult()));
        } catch (JAXBException e) {
            return null;
        }

        return results;
    }

    /**
     * Fixes JSON with wrong double values
     * 
     * @param input
     * @return the fixed json
     */
    private String fixDoubleValues(String input) {
        return input.replaceAll("(\\d+),(\\d+)", "$1.$2");
    }

    private VoskResult unmarshall(String input) throws JAXBException, IOException {
        return JsonUtils.fromJSON(fixDoubleValues(input), VoskResult.class, false);
    }

    @XmlRootElement(name = "voskResult")
    @XmlType(name = "VoskResult")
    @XmlAccessorType(XmlAccessType.NONE)
    public static class VoskResult {

        private List<Word> result;

        private String text;

        /**
         * @return the result
         */
        @XmlElement(name = "result")
        public List<Word> getResult() {
            return result;
        }

        /**
         * @param result the result to set
         */
        public void setResult(List<Word> result) {
            this.result = result;
        }

        /**
         * @return the text
         */
        @XmlElement(name = "text")
        public String getText() {
            return text;
        }

        /**
         * @param text the text to set
         */
        public void setText(String text) {
            this.text = text;
        }

        @XmlRootElement(name = "voskWord")
        @XmlType(name = "VoskResult.Word")
        @XmlAccessorType(XmlAccessType.NONE)
        public static class Word {

            private double conf;

            private double end;

            private double start;

            private String word;

            /**
             * @return the conf
             */
            @XmlElement(name = "conf")
            public double getConf() {
                return conf;
            }

            /**
             * @param conf the conf to set
             */
            public void setConf(double conf) {
                this.conf = conf;
            }

            /**
             * @return the end
             */
            @XmlElement(name = "end")
            public double getEnd() {
                return end;
            }

            /**
             * @param end the end to set
             */
            public void setEnd(double end) {
                this.end = end;
            }

            /**
             * @return the start
             */
            @XmlElement(name = "start")
            public double getStart() {
                return start;
            }

            /**
             * @param start the start to set
             */
            public void setStart(double start) {
                this.start = start;
            }

            /**
             * @return the word
             */
            @XmlElement(name = "word")
            public String getWord() {
                return word;
            }

            /**
             * @param word the word to set
             */
            public void setWord(String word) {
                this.word = word;
            }

        }
    }

}
