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
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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

    public static final String CONFIG_PREFIX_MODEL = CONFIG_PREFIX + "model.";

    public static final String DE = "de";

    public static final String EN = "en";

    public static final String RU = "ru";

    public static final String IT = "it";

    private static final Logger LOGGER = LogManager.getLogger();

    private static final float DEFAULT_SAMPLE_RATE = 16000;

    private static final int DEFAULT_GUESS_MAX_WORDS = 200;

    private static final double DEFAULT_GUESS_MIN_WORD_CONF = 0.9;

    private static final Map<String, List<String>> DEFAULT_GUESS_MAP;

    private final Configuration config;

    private final String modelPath;

    private final float sampleRate;

    private final Map<String, Path> modelPaths;

    private final Map<String, List<String>> guessMap;

    private String modelLang;

    static {
        DEFAULT_GUESS_MAP = new HashMap<>();

        DEFAULT_GUESS_MAP.put(DE,
                Arrays.asList("aber", "acht", "alle", "am", "auch", "aus", "bei", "bis", "dann", "das", "dem", "den",
                        "denn", "der", "die", "drei", "du", "ein", "eine", "eins", "elf", "fünf", "für", "gut", "haben",
                        "heute", "ich", "ihn", "ihnen", "ihr", "im", "ist", "ja", "jetzt", "kann", "können", "mehr",
                        "mit", "nein", "neun", "null", "nun", "oder", "sechs", "sein", "sich", "sie", "sieben", "sind",
                        "und", "uns", "vier", "vom", "von", "vor", "werden", "wie", "wir", "wird", "zu", "zum", "zwei",
                        "zwölf"));
        DEFAULT_GUESS_MAP.put(EN,
                Arrays.asList("after", "all", "and", "are", "at", "be", "before", "but", "by", "can", "eight", "eleven",
                        "five", "for", "four", "have", "he", "her", "his", "how", "is", "it", "me", "nine", "no", "now",
                        "of", "off", "on", "one", "or", "seven", "shall", "she", "six", "that", "the", "there", "this",
                        "three", "to", "twelve", "two", "us", "will", "with", "yes", "you", "your", "zero"));
        DEFAULT_GUESS_MAP.put(RU,
                Arrays.asList("более", "буду", "в", "вам", "вас", "восемь", "вот", "все", "вы", "да", "два", "девять",
                        "для", "ее", "ему", "и", "или", "иметь", "к", "как", "который", "нас", "не", "нет", "но",
                        "нуль", "один", "он", "она", "от", "очень", "погода", "пять", "с", "с участием", "сейчас",
                        "семь", "так", "три", "ты", "у", "четыре", "что", "чтобы", "шесть", "это", "я"));
        DEFAULT_GUESS_MAP.put(IT,
                Arrays.asList("adesso", "al", "alcune", "anche", "avere", "avete", "bambini", "bene", "buongiorno",
                        "ce", "che", "cinque", "cioè", "come", "con", "cui", "dal", "davanti", "dei", "del", "della",
                        "di", "dodici", "due", "durante", "il", "io", "la", "lo", "loro", "lui", "ma", "ne", "nella",
                        "noi", "non", "nove", "o", "otto", "per", "poi", "potere", "potete", "quattro", "quello",
                        "quindi", "se", "sei", "sette", "si", "sono", "sua", "tre", "tutti", "un", "una", "undici",
                        "uno", "voi", "volere", "zero"));
    }

    public VoskExtractor() {
        this(null, null, DEFAULT_SAMPLE_RATE);
    }

    public VoskExtractor(float sampleRate) {
        this(null, null, sampleRate);
    }

    public VoskExtractor(String modelPath, String modelLang, float sampleRate) {
        this.config = Configuration.instance();
        this.modelPath = Optional.ofNullable(modelPath)
                .orElseGet(() -> config.getString(CONFIG_PREFIX + "modelPath", null));
        this.modelLang = Optional.ofNullable(modelLang).orElse(DE);
        this.sampleRate = sampleRate;
        this.modelPaths = getConfiguredModels();
        this.guessMap = getConfiguredGuessMap();
    }

    private Map<String, Path> getConfiguredModels() {
        return config.getPropertiesMap(VoskExtractor.CONFIG_PREFIX_MODEL).entrySet().stream()
                .filter(p -> Files.exists(Paths.get(modelPath).resolve(p.getValue())))
                .collect(Collectors.toMap(p -> p.getKey().substring(VoskExtractor.CONFIG_PREFIX_MODEL.length()),
                        p -> Paths.get(modelPath).resolve(p.getValue())));
    }

    private Map<String, List<String>> getConfiguredGuessMap() {
        return getConfiguredModels().keySet().stream()
                .collect(Collectors.toMap(lng -> lng,
                        lng -> config.getStrings(CONFIG_PREFIX_MODEL + lng + ".guessMap", DEFAULT_GUESS_MAP.get(lng))));
    }

    /**
     * @return the modelPath
     */
    public String getModelPath() {
        return getModelPath(modelLang);
    }

    private String getModelPath(String modelLang) {
        return Optional.ofNullable(modelPaths.get(modelLang))
                .map(mlp -> mlp.toAbsolutePath().toString()).orElse(modelPath);
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

        int guessMaxWords = config.getInt(CONFIG_PREFIX + "guessMaxWords", DEFAULT_GUESS_MAX_WORDS);
        double guessMinWordConf = config.getDouble(CONFIG_PREFIX + "guessMinWordConf", DEFAULT_GUESS_MIN_WORD_CONF);
        boolean guessConcurrent = config.getBoolean(CONFIG_PREFIX + "guessConcurrent", true);

        Set<String> langs = guessMap.keySet();
        Stream<String> stream = guessConcurrent ? langs.parallelStream() : langs.stream();

        return stream.map(lang -> {
            Map<String, Integer> gr = new HashMap<>();

            try (Model model = new Model(getModelPath(lang));
                    InputStream is = Files.newInputStream(input);
                    InputStream ais = AudioSystem.getAudioInputStream(new BufferedInputStream(is));
                    Recognizer recognizer = new Recognizer(model, sampleRate)) {
                recognizer.setWords(true);

                int wordCount = 0;
                int nbytes;
                byte[] b = new byte[(int) (sampleRate / 2)];
                List<String> allWords = new ArrayList<>();
                while ((nbytes = ais.read(b)) >= 0) {
                    if (recognizer.acceptWaveForm(b, nbytes)) {
                        VoskResult result = unmarshall(recognizer.getResult());
                        if (result.getResult() != null && !result.getResult().isEmpty()) {
                            List<String> words = result.getResult().stream()
                                    .filter(w -> w.getConf() >= guessMinWordConf &&
                                            (allWords.isEmpty() || !w.getWord()
                                                    .equalsIgnoreCase(allWords.get(allWords.size() - 1))))
                                    .map(w -> w.getWord().toLowerCase(Locale.ROOT))
                                    .peek(allWords::add)
                                    .collect(Collectors.toList());

                            Map<String, Integer> gres = guessMap.entrySet().stream()
                                    .filter(e -> e.getValue() != null)
                                    .collect(Collectors.toMap(e -> (String) e.getKey(),
                                            e -> Long.valueOf(
                                                    e.getValue().stream()
                                                            .map(gw -> gw.toLowerCase(Locale.ROOT))
                                                            .filter(words::contains).count())
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

                    if (wordCount > guessMaxWords) {
                        break;
                    }
                }

                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("{}\n{}", String.join(" ", allWords), countOccurrences(allWords, 3));
                }

                return gr;
            } catch (Exception e) {
                LOGGER.error(e.getMessage(), e);
                return null;
            }
        }).filter(m -> m != null)
                .map(gr -> gr.entrySet()
                        .stream()
                        .max(sortGuess)
                        .orElse(null))
                .filter(e -> e != null)
                .max(sortGuess)
                .map(e -> e.getValue() == 0 ? null : e.getKey())
                .orElse(null);
    }

    private Comparator<Entry<String, Integer>> sortGuess = (e1, e2) -> {
        return e1.getValue().compareTo(e2.getValue());
    };

    private Map<String, Long> countOccurrences(List<String> sentence, int minOccurrence) {
        return sentence.stream()
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting())).entrySet().stream()
                .filter(e -> e.getValue() > 3)
                .collect(Collectors.toMap(e -> e.getKey(), e -> e.getValue()));
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
