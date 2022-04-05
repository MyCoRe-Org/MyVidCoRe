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
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.vidconv.common.config.Configuration;
import org.mycore.vidconv.common.util.JsonUtils;
import org.vosk.LibVosk;
import org.vosk.LogLevel;
import org.vosk.Model;
import org.vosk.Recognizer;

import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlType;

/**
 * Extracts Text from Audio file.
 * 
 * <p>
 * Linux needes <i>libatomic.so.1</i> to be installed.
 * </p>
 * 
 * @author Ren\u00E9 Adler (eagle)
 *
 */
public class VoskExtractor {

    public static final String CONFIG_PREFIX = "VoskExtractor.";

    public static final String DE = "de";

    public static final String EN = "en";

    private static final Logger LOGGER = LogManager.getLogger();

    private static final float DEFAULT_SAMPLE_RATE = 16000;

    private static final int DEFAULT_GUESS_MAX_WORDS = 200;

    private static final Map<String, List<String>> DEFAULT_GUESS_MAP;

    private final String modelPath;

    private final float sampleRate;

    private String modelLang;

    static {
        DEFAULT_GUESS_MAP = new HashMap<>();

        DEFAULT_GUESS_MAP.put(DE,
                Arrays.asList("der", "die", "das", "ich", "du", "wir", "uns", "ihr", "ihn", "ihnen", "sie", "es", "zu",
                        "zum", "und", "oder", "für", "mit", "im", "am", "dem", "den", "denn", "dann", "von", "vom",
                        "vor", "ist", "bis", "wie", "aber", "ja", "nein", "wird", "werden", "haben", "aus", "ein",
                        "mehr", "sein", "nun", "auch", "eins", "zwei", "drei", "vier", "fünf", "sechs", "sieben",
                        "acht", "neun", "null"));
        DEFAULT_GUESS_MAP.put(EN,
                Arrays.asList("the", "there", "this", "that", "his", "her", "me", "you", "he", "she", "us", "with",
                        "of", "are", "and", "or", "for", "at", "to", "by", "is", "it", "how", "be", "before", "after",
                        "but", "yes", "no", "will", "shall", "have", "on", "off", "now", "one", "two", "three", "four",
                        "five", "six", "seven", "eight", "nine", "zero"));
    }

    public VoskExtractor() {
        this(null, null, DEFAULT_SAMPLE_RATE);
    }

    public VoskExtractor(float sampleRate) {
        this(null, null, sampleRate);
    }

    public VoskExtractor(String modelPath, String modelLang, float sampleRate) {
        this.modelPath = Optional.ofNullable(modelPath)
                .orElseGet(() -> Configuration.instance().getString(CONFIG_PREFIX + "modelPath", null));
        this.modelLang = Optional.ofNullable(modelLang).orElse(DE);
        this.sampleRate = sampleRate;
    }

    /**
     * @return the modelPath
     */
    public String getModelPath() {
        return getModelPath(modelLang);
    }

    private String getModelPath(String modelLang) {
        return Optional.ofNullable(Configuration.instance().getString(CONFIG_PREFIX + "model." + modelLang, null))
                .map(mlp -> Paths.get(modelPath).resolve(mlp).toAbsolutePath().toString()).orElse(modelPath);
    }

    /**
     * @return the modelLang
     */
    public String getModelLang() {
        return modelLang;
    }

    /**
     * @param modelLang the modelLang to set
     */
    public void setModelLang(String modelLang) {
        this.modelLang = modelLang;
    }

    /**
     * Extract text from given audio file.
     * 
     * @param input the audio file
     * @return a {@link List} of {@link VoskResult}s 
     * @throws IOException
     * @throws UnsupportedAudioFileException
     */
    public List<VoskResult> extract(Path input) throws IOException, UnsupportedAudioFileException {
        if (this.modelPath == null) {
            return null;
        }

        LibVosk.setLogLevel(LogLevel.WARNINGS);

        List<VoskResult> results = new ArrayList<>();

        try (Model model = new Model(getModelPath());
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
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
            return null;
        }

        return results;
    }

    /**
     * Guess language of given audio file.
     * 
     * @param input the audio file
     * @return the langauage
     * @throws IOException
     * @throws UnsupportedAudioFileException
     */
    public String guessLanguage(Path input) throws IOException, UnsupportedAudioFileException {
        if (this.modelPath == null) {
            return null;
        }

        LibVosk.setLogLevel(LogLevel.WARNINGS);

        Map<String, Integer> guessResult = new HashMap<>();

        for (String lang : Arrays.asList(DE, EN)) {
            Map<String, Integer> gr = new HashMap<>();

            try (Model model = new Model(getModelPath(lang));
                    InputStream is = Files.newInputStream(input);
                    InputStream ais = AudioSystem.getAudioInputStream(new BufferedInputStream(is));
                    Recognizer recognizer = new Recognizer(model, sampleRate)) {
                recognizer.setWords(true);

                int wordCount = 0;
                int nbytes;
                byte[] b = new byte[(int) (sampleRate / 2)];
                while ((nbytes = ais.read(b)) >= 0) {
                    if (recognizer.acceptWaveForm(b, nbytes)) {
                        VoskResult result = unmarshall(recognizer.getResult());
                        if (result.getResult() != null && !result.getResult().isEmpty()) {
                            List<String> words = result.getResult().stream()
                                    .map(w -> w.getWord().toLowerCase(Locale.ROOT))
                                    .collect(Collectors.toList());

                            Map<String, Integer> gres = DEFAULT_GUESS_MAP.entrySet().stream()
                                    .collect(Collectors.toMap(e -> (String) e.getKey(),
                                            e -> Long.valueOf(e.getValue().stream().filter(words::contains).count())
                                                    .intValue()));

                            gres.entrySet().stream().forEach(e -> {
                                if (gr.containsKey(e.getKey())) {
                                    gr.compute(e.getKey(), (k, i) -> i + e.getValue());
                                } else {
                                    gr.put(e.getKey(), e.getValue());
                                }
                            });

                            wordCount += words.size();
                        }
                    }

                    if (wordCount > DEFAULT_GUESS_MAX_WORDS) {
                        break;
                    }
                }

                gr.entrySet()
                        .stream()
                        .sorted(sortGuess)
                        .findFirst().ifPresent(e -> guessResult.put(e.getKey(), e.getValue()));
            } catch (Exception e) {
                LOGGER.error(e.getMessage(), e);
                return null;
            }
        }

        return guessResult.entrySet()
                .stream()
                .sorted(sortGuess)
                .findFirst()
                .map(Entry::getKey)
                .orElse(null);
    }

    private Comparator<Entry<String, Integer>> sortGuess = (e1, e2) -> {
        int i = e1.getValue().compareTo(e2.getValue());
        return (i != 0) ? -i : 0;
    };

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
