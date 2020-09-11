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
package org.mycore.vidconv.backend.encoder;

import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.stream.StreamSource;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.ehcache.Cache;
import org.ehcache.CacheManager;
import org.ehcache.config.builders.CacheConfigurationBuilder;
import org.ehcache.config.builders.CacheManagerBuilder;
import org.ehcache.config.builders.ResourcePoolsBuilder;
import org.mycore.vidconv.common.config.ConfigurationDir;
import org.mycore.vidconv.common.event.annotation.AutoExecutable;
import org.mycore.vidconv.common.event.annotation.Startup;
import org.mycore.vidconv.common.util.Executable;
import org.mycore.vidconv.common.util.JsonUtils;
import org.mycore.vidconv.common.util.MatcherStream;
import org.mycore.vidconv.frontend.entity.CodecWrapper;
import org.mycore.vidconv.frontend.entity.CodecsWrapper;
import org.mycore.vidconv.frontend.entity.DecoderWrapper;
import org.mycore.vidconv.frontend.entity.DecodersWrapper;
import org.mycore.vidconv.frontend.entity.EncoderWrapper;
import org.mycore.vidconv.frontend.entity.EncodersWrapper;
import org.mycore.vidconv.frontend.entity.FilterWrapper;
import org.mycore.vidconv.frontend.entity.FiltersWrapper;
import org.mycore.vidconv.frontend.entity.FormatWrapper;
import org.mycore.vidconv.frontend.entity.FormatsWrapper;
import org.mycore.vidconv.frontend.entity.HWAccelDeviceSpec;
import org.mycore.vidconv.frontend.entity.HWAccelNvidiaSpec;
import org.mycore.vidconv.frontend.entity.HWAccelWrapper;
import org.mycore.vidconv.frontend.entity.HWAccelWrapper.HWAccelType;
import org.mycore.vidconv.frontend.entity.HWAccelsWrapper;
import org.mycore.vidconv.frontend.entity.MuxerWrapper;
import org.mycore.vidconv.frontend.entity.ParameterWrapper;
import org.mycore.vidconv.frontend.entity.ParameterWrapper.ParameterValue;
import org.mycore.vidconv.frontend.entity.SettingsWrapper.Audio;
import org.mycore.vidconv.frontend.entity.SettingsWrapper.Output;
import org.mycore.vidconv.frontend.entity.SettingsWrapper.Video;
import org.mycore.vidconv.frontend.entity.probe.ProbeWrapper;
import org.mycore.vidconv.frontend.entity.probe.StreamWrapper;

/**
 * The Class FFMpegImpl.
 *
 * @author Ren\u00E9 Adler (eagle)
 */
@AutoExecutable(name = "FFMpeg Init", priority = 1000)
public class FFMpegImpl {

    /** The Constant LOGGER. */
    private static final Logger LOGGER = LogManager.getLogger();

    /** The Constant CACHE_MGR. */
    private static final CacheManager CACHE_MGR = CacheManagerBuilder.newCacheManagerBuilder()
            .withCache("probe",
                    CacheConfigurationBuilder.newCacheConfigurationBuilder(Path.class, ProbeWrapper.class,
                            ResourcePoolsBuilder.heap(100))
                            .build())
            .build(true);

    private static final long PROCESS_TIMEOUT_VALUE = 5;

    private static final TimeUnit PROCESS_TIMEOUT_UNIT = TimeUnit.MINUTES;

    /**
     * Inits the.
     */
    @Startup
    protected static void init() {
        LOGGER.info("parse codecs...");
        LOGGER.info("...found {}.", Optional.ofNullable(codecs()).map(c -> c.getCodecs().size()).orElse(0));

        LOGGER.info("parse filters...");
        LOGGER.info("...found {}.", Optional.ofNullable(filters()).map(f -> f.getFilters().size()).orElse(0));

        LOGGER.info("parse formats...");
        LOGGER.info("...found {}.", Optional.ofNullable(formats()).map(f -> f.getFormats().size()).orElse(0));

        LOGGER.info("detect hw accelerators...");
        HWAccelsWrapper hwaccels = detectHWAccels();
        if (hwaccels != null && !hwaccels.getHWAccels().isEmpty()) {
            hwaccels.getHWAccels().forEach(outputHWAccelInfo());
        } else {
            LOGGER.info("...none found.");
        }
    }

    private static Consumer<HWAccelWrapper<? extends HWAccelDeviceSpec>> outputHWAccelInfo() {
        return hw -> {
            if (hw.getDeviceSpec() == null) {
                LOGGER.warn("...found {} {} ({}) - but missing in support matrix.", hw.getIndex(), hw.getName(),
                        hw.getType());
            } else {
                LOGGER.info("...found {} {} ({}).", hw.getIndex(), hw.getName(), hw.getType());
            }
        };
    }

    /** The Constant PATTERN_ENTRY_SPLIT. */
    private static final Pattern PATTERN_ENTRY_SPLIT = Pattern.compile("\\n\\n");

    /** The Constant PATTERN_CODECS. */
    private static final Pattern PATTERN_CODECS = Pattern
            .compile("\\s(D|\\.|\\s)(E|\\.|\\s)(V|A|S|\\s)(I|\\.|\\s)(L|\\.|\\s)(S|\\.|\\s)\\s([^=\\s\\t]+)([^\\n]+)");

    /** The Constant PATTERN_ENCODER_LIB. */
    private static final Pattern PATTERN_ENCODER_LIB = Pattern.compile("\\(encoders:\\s([^\\)]+)\\)");

    /** The Constant PATTERN_DECODER_LIB. */
    private static final Pattern PATTERN_DECODER_LIB = Pattern.compile("\\(decoders:\\s([^\\)]+)\\)");

    /** The supported codecs. */
    private static CodecsWrapper supportedCodecs;

    /**
     * Returns all supported codecs.
     *
     * @return the supported codecs
     */
    public static CodecsWrapper codecs() {
        if (supportedCodecs != null) {
            return supportedCodecs;
        }

        final Executable exec = new Executable("ffmpeg", "-codecs");

        try {
            if (exec.runAndWait(PROCESS_TIMEOUT_VALUE, PROCESS_TIMEOUT_UNIT) == 0) {
                final String outputStream = exec.output();

                if (outputStream != null && !outputStream.isEmpty()) {
                    supportedCodecs = new CodecsWrapper()
                            .setCodecs(MatcherStream.findMatches(PATTERN_CODECS, outputStream).map(mr -> {
                                final CodecWrapper codec = new CodecWrapper();

                                if ("A".equals(mr.group(3))) {
                                    codec.setType(CodecWrapper.Type.AUDIO);
                                } else if ("V".equals(mr.group(3))) {
                                    codec.setType(CodecWrapper.Type.VIDEO);
                                } else if ("S".equals(mr.group(3))) {
                                    codec.setType(CodecWrapper.Type.SUBTITLE);
                                } else {
                                    return null;
                                }

                                codec.setLossy(mr.group(5).equalsIgnoreCase("L"));
                                codec.setLossless(mr.group(6).equalsIgnoreCase("S"));

                                codec.setName(mr.group(7));
                                String desc = mr.group(8).trim();
                                if (desc != null) {
                                    MatcherStream.findMatches(PATTERN_ENCODER_LIB, desc).findFirst()
                                            .map(m -> splitString(m.group(1)).collect(Collectors.toList()))
                                            .ifPresent(codec::setEncoderLib);

                                    MatcherStream.findMatches(PATTERN_DECODER_LIB, desc).findFirst()
                                            .map(m -> splitString(m.group(1)).collect(Collectors.toList()))
                                            .ifPresent(codec::setDecoderLib);

                                    desc = desc.replaceAll(PATTERN_DECODER_LIB.pattern(), "");
                                    desc = desc.replaceAll(PATTERN_ENCODER_LIB.pattern(), "");
                                }
                                codec.setDescription(desc.trim());

                                return codec;
                            }).filter(Objects::nonNull).collect(Collectors.toList()));
                    return supportedCodecs;
                }
            }
        } catch (InterruptedException | ExecutionException e) {
            LOGGER.error(e);
        }

        return null;
    }

    /** The Constant PATTERN_FILTERS. */
    private static final Pattern PATTERN_FILTERS = Pattern
            .compile("\\s(T|\\.|\\s)(S|\\.|\\s)(C|\\.|\\s)\\s([^=\\s\\t]+)([^-]+->[^\\s\\t]+)([^\\n]+)");

    /** The supported filters. */
    private static FiltersWrapper supportedFilters;

    /**
     * Returns all supported filters.
     *
     * @return the supported formats
     */
    public static FiltersWrapper filters() {
        if (supportedFilters != null) {
            return supportedFilters;
        }

        final Executable exec = new Executable("ffmpeg", "-filters");

        try {
            if (exec.runAndWait(PROCESS_TIMEOUT_VALUE, PROCESS_TIMEOUT_UNIT) == 0) {
                final String outputStream = exec.output();

                if (outputStream != null && !outputStream.isEmpty()) {
                    supportedFilters = new FiltersWrapper()
                            .setFilters(MatcherStream.findMatches(PATTERN_FILTERS, outputStream).map(mr -> {
                                final FilterWrapper filter = new FilterWrapper();

                                filter.setTimelineSupport(mr.group(1).equalsIgnoreCase("T"));
                                filter.setSliceSupport(mr.group(2).equalsIgnoreCase("S"));
                                filter.setCommandSupport(mr.group(3).equalsIgnoreCase("C"));
                                filter.setName(mr.group(4).trim());
                                filter.setIoSupport(mr.group(5).trim());
                                filter.setDescription(mr.group(6).trim());

                                return filter;
                            }).collect(Collectors.toList()));
                    return supportedFilters;
                }
            }
        } catch (InterruptedException | ExecutionException e) {
            LOGGER.error(e);
        }

        return null;
    }

    /** The Constant PATTERN_FORMATS. */
    private static final Pattern PATTERN_FORMATS = Pattern
            .compile("\\s(D|\\.|\\s)(E|\\.|\\s)\\s([^=\\s\\t]+)([^\\n]+)");

    /** The supported formats. */
    private static FormatsWrapper supportedFormats;

    /**
     * Returns all supported formats.
     *
     * @return the supported formats
     */
    public static FormatsWrapper formats() {
        if (supportedFormats != null) {
            return supportedFormats;
        }

        final Executable exec = new Executable("ffmpeg", "-formats");

        try {
            if (exec.runAndWait(PROCESS_TIMEOUT_VALUE, PROCESS_TIMEOUT_UNIT) == 0) {
                final String outputStream = exec.output();

                if (outputStream != null && !outputStream.isEmpty()) {
                    supportedFormats = new FormatsWrapper()
                            .setFormats(MatcherStream.findMatches(PATTERN_FORMATS, outputStream).map(mr -> {
                                final FormatWrapper format = new FormatWrapper();

                                format.setDemuxer(mr.group(1).equalsIgnoreCase("D"));
                                format.setMuxer(mr.group(2).equalsIgnoreCase("E"));
                                format.setName(mr.group(3).trim());
                                format.setDescription(mr.group(4).trim());

                                return format;
                            }).collect(Collectors.toList()));
                    return supportedFormats;
                }
            }
        } catch (InterruptedException | ExecutionException e) {
            LOGGER.error(e);
        }

        return null;
    }

    /** The Constant PATTERN_DECODER. */
    private static final Pattern PATTERN_DECODER = Pattern
            .compile("^Decoder\\s([^\\s]+)\\s\\[([^\\]]+)\\]:\\n([\\S\\s]+)$");

    /** The Constant PATTERN_PARAMS. */
    private static final Pattern PATTERN_PARAMS = Pattern
            .compile("\\s+-([^\\s]+)\\s+<([^>]+)>\\s+(?:[^\\s]+)\\s([^\\n]+)([\\S\\s]+?(?=\\s+-))?");

    /** The Constant PATTERN_PARAM_VALUES. */
    private static final Pattern PATTERN_PARAM_VALUES = Pattern
            .compile("\\s+([^\\s]+)\\s+(?:\\d+\\s+)?(?:[^\\s]+)([^\\n]*)");

    /** The Constant PATTERN_PARAM_FROM_TO. */
    private static final Pattern PATTERN_PARAM_FROM_TO = Pattern
            .compile("\\(from\\s([^\\s]+)\\sto\\s([^\\s]+)\\)");

    /** The Constant PATTERN_PARAM_DEFAULT. */
    private static final Pattern PATTERN_PARAM_DEFAULT = Pattern
            .compile("\\(default\\s([^\\)]+)\\)");

    /**
     * Parses the parameters.
     *
     * @param output the output
     * @return the list
     */
    private static List<ParameterWrapper> parseParameters(final String output) {
        return MatcherStream.findMatches(PATTERN_PARAMS, output).map(mr -> {
            final ParameterWrapper param = new ParameterWrapper();

            param.setName(mr.group(1));
            param.setType(mr.group(2));
            param.setDescription(mr.group(3));

            Optional.ofNullable(getPatternGroup(PATTERN_PARAM_FROM_TO, mr.group(3), 1))
                    .ifPresent(v -> param.setFromValue(v));
            Optional.ofNullable(getPatternGroup(PATTERN_PARAM_FROM_TO, mr.group(3), 2))
                    .ifPresent(v -> param.setToValue(v));
            Optional.ofNullable(getPatternGroup(PATTERN_PARAM_DEFAULT, mr.group(3), 1))
                    .ifPresent(v -> param.setDefaultValue(v));

            Optional.ofNullable(mr.group(4)).filter(vs -> !vs.isEmpty()).ifPresent(vs -> {
                final List<ParameterValue> values = MatcherStream.findMatches(PATTERN_PARAM_VALUES, vs).map(pv -> {
                    final ParameterValue value = new ParameterValue();

                    value.setName(pv.group(1));
                    Optional.ofNullable(pv.group(2)).map(String::trim).filter(v -> !v.isEmpty())
                            .ifPresent(v -> value.setDescription(v));

                    return value;
                }).collect(Collectors.toList());

                if (!values.isEmpty())
                    param.setValues(values);
            });

            return param;
        }).collect(Collectors.toList());
    }

    /** The supported decoders. */
    private static Map<String, DecodersWrapper> supportedDecoders = new ConcurrentHashMap<>();

    /**
     * Returns informations for given decoder.
     *
     * @param name the name
     * @return the decoders wrapper
     * @throws NumberFormatException the number format exception
     */
    public static DecodersWrapper decoder(final String name)
            throws NumberFormatException {
        if (supportedDecoders.containsKey(name)) {
            return supportedDecoders.get(name);
        }

        final Executable exec = new Executable("ffmpeg", "-h", "decoder=" + name);

        try {
            if (exec.runAndWait(PROCESS_TIMEOUT_VALUE, PROCESS_TIMEOUT_UNIT) == 0) {
                final String outputStream = exec.output();

                if (outputStream != null && !outputStream.isEmpty()) {
                    supportedDecoders.put(name,
                            new DecodersWrapper().setDecoders(PATTERN_ENTRY_SPLIT.splitAsStream(outputStream)
                                    .filter(os -> !os.isEmpty())
                                    .map(os -> MatcherStream.findMatches(PATTERN_DECODER, os).map(mr -> {
                                        final DecoderWrapper decoder = new DecoderWrapper();

                                        decoder.setName(mr.group(1));
                                        decoder.setDescription(mr.group(2));
                                        decoder.setParameters(parseParameters(mr.group(3)));

                                        return decoder;
                                    })).flatMap(ds -> ds).collect(Collectors.toList())));
                    return supportedDecoders.get(name);
                }
            }
        } catch (InterruptedException | ExecutionException e) {
            LOGGER.error(e);
        }

        return null;
    }

    /** The Constant PATTERN_ENCODER. */
    private static final Pattern PATTERN_ENCODER = Pattern
            .compile("^Encoder\\s([^\\s]+)\\s\\[([^\\]]+)\\]:\\n([\\S\\s]+)$");

    /** The Constant PATTERN_PIX_FMT. */
    private static final Pattern PATTERN_PIX_FMT = Pattern.compile("pixel formats:(.*)");

    /** The Constant PATTERN_FRM_RATES. */
    private static final Pattern PATTERN_FRM_RATES = Pattern.compile("framerates:(.*)");

    /** The Constant PATTERN_SMP_RATES. */
    private static final Pattern PATTERN_SMP_RATES = Pattern.compile("sample rates:(.*)");

    /** The Constant PATTERN_SMP_FROMATS. */
    private static final Pattern PATTERN_SMP_FROMATS = Pattern.compile("sample formats:(.*)");

    /** The Constant PATTERN_CH_LAYOUTS. */
    private static final Pattern PATTERN_CH_LAYOUTS = Pattern.compile("channel layouts:(.*)");

    /** The supported encoders. */
    private static Map<String, EncodersWrapper> supportedEncoders = new ConcurrentHashMap<>();

    /**
     * Returns informations for given encoder.
     *
     * @param name the encoder name
     * @return the encoders wrapper
     * @throws NumberFormatException the number format exception
     */
    public static EncodersWrapper encoder(final String name) throws NumberFormatException {
        if (supportedEncoders.containsKey(name)) {
            return supportedEncoders.get(name);
        }

        final Executable exec = new Executable("ffmpeg", "-h", "encoder=" + name);

        try {
            if (exec.runAndWait(PROCESS_TIMEOUT_VALUE, PROCESS_TIMEOUT_UNIT) == 0) {
                final String outputStream = exec.output();

                if (outputStream != null && !outputStream.isEmpty()) {
                    supportedEncoders.put(name,
                            new EncodersWrapper().setEncoders(PATTERN_ENTRY_SPLIT.splitAsStream(outputStream)
                                    .filter(os -> !os.isEmpty())
                                    .map(os -> MatcherStream.findMatches(PATTERN_ENCODER, os).map(mr -> {
                                        final EncoderWrapper encoder = new EncoderWrapper();

                                        encoder.setName(mr.group(1));
                                        encoder.setDescription(mr.group(2));

                                        encoder.setPixelFormats(MatcherStream.findMatches(PATTERN_PIX_FMT, mr.group(3))
                                                .flatMap(ma -> splitString(ma.group(1))).collect(Collectors.toList()));

                                        encoder.setFrameRates(MatcherStream.findMatches(PATTERN_FRM_RATES, mr.group(3))
                                                .flatMap(ma -> splitString(ma.group(1))).collect(Collectors.toList()));

                                        encoder.setSampleFormats(MatcherStream
                                                .findMatches(PATTERN_SMP_FROMATS, mr.group(3))
                                                .flatMap(ma -> splitString(ma.group(1))).collect(Collectors.toList()));

                                        encoder.setSampleRates(MatcherStream.findMatches(PATTERN_SMP_RATES, mr.group(3))
                                                .flatMap(ma -> splitString(ma.group(1))).map(s -> new Integer(s))
                                                .collect(Collectors.toList()));

                                        encoder.setChannelLayouts(MatcherStream
                                                .findMatches(PATTERN_CH_LAYOUTS, mr.group(3))
                                                .flatMap(ma -> splitString(ma.group(1))).collect(Collectors.toList()));

                                        encoder.setParameters(parseParameters(mr.group(3)));

                                        return encoder;
                                    })).flatMap(es -> es).collect(Collectors.toList())));
                    return supportedEncoders.get(name);
                }
            }
        } catch (InterruptedException | ExecutionException e) {
            LOGGER.error(e);
        }

        return null;
    }

    /** The Constant PATTERN_MUXER. */
    private static final Pattern PATTERN_MUXER = Pattern
            .compile("^Muxer\\s([^\\s]+)\\s\\[(?:[^\\]]+)\\]:\\n([\\S\\s]+)$");

    /** The Constant PATTERN_EXTENSION. */
    private static final Pattern PATTERN_EXTENSION = Pattern.compile("Common extensions:(.*)\\.");

    /** The Constant PATTERN_MIME_TYPE. */
    private static final Pattern PATTERN_MIME_TYPE = Pattern.compile("Mime type:(.*)\\.");

    /** The Constant PATTERN_AUDIO_CODEC. */
    private static final Pattern PATTERN_AUDIO_CODEC = Pattern.compile("audio codec:(.*)\\.");

    /** The Constant PATTERN_VIDEO_CODEC. */
    private static final Pattern PATTERN_VIDEO_CODEC = Pattern.compile("video codec:(.*)\\.");

    /** The Constant PATTERN_SUBTITLE_CODEC. */
    private static final Pattern PATTERN_SUBTITLE_CODEC = Pattern.compile("video codec:(.*)\\.");

    /** The supported muxers. */
    private static Map<String, MuxerWrapper> supportedMuxers = new ConcurrentHashMap<>();

    /**
     * Muxer.
     *
     * @param name the name
     * @return the muxer wrapper
     */
    public static MuxerWrapper muxer(final String name) {
        if (supportedMuxers.containsKey(name)) {
            return supportedMuxers.get(name);
        }

        final Executable exec = new Executable("ffmpeg", "-h", "muxer=" + name);

        try {
            if (exec.runAndWait(PROCESS_TIMEOUT_VALUE, PROCESS_TIMEOUT_UNIT) == 0) {
                final String outputStream = exec.output();

                if (outputStream != null && !outputStream.isEmpty()) {
                    MatcherStream.findMatches(PATTERN_MUXER, outputStream).findFirst().map(mr -> {
                        final MuxerWrapper muxer = new MuxerWrapper();

                        muxer.setName(mr.group(1));

                        muxer.setExtension(getPatternGroup(PATTERN_EXTENSION, mr.group(2), 1));
                        muxer.setMimeType(getPatternGroup(PATTERN_MIME_TYPE, mr.group(2), 1));
                        muxer.setAudioCodec(getPatternGroup(PATTERN_AUDIO_CODEC, mr.group(2), 1));
                        muxer.setVideoCodec(getPatternGroup(PATTERN_VIDEO_CODEC, mr.group(2), 1));
                        muxer.setSubtitleCodec(getPatternGroup(PATTERN_SUBTITLE_CODEC, mr.group(2), 1));

                        return muxer;
                    }).ifPresent(muxer -> supportedMuxers.put(name, muxer));

                    return supportedMuxers.get(name);
                }
            }
        } catch (InterruptedException | ExecutionException e) {
            LOGGER.error(e);
        }

        return null;
    }

    /** The Constant PATTERN_NVENC_GPU. */
    private static final Pattern PATTERN_NVENC_GPU = Pattern.compile("GPU\\s#(\\d+).*<([^>]+)(?:.*has\\s([^\\]]+))?");

    /** The detected HW accels. */
    private static HWAccelsWrapper detectedHWAccels;

    /**
     * Detect HW accels.
     *
     * @return the HW accels wrapper
     */
    public static HWAccelsWrapper detectHWAccels() {
        if (detectedHWAccels != null) {
            return detectedHWAccels;
        }

        final Executable exec = new Executable("ffmpeg -f lavfi -i nullsrc -c:v h264_nvenc -gpu list -f null -");

        try {
            if (exec.runAndWait(PROCESS_TIMEOUT_VALUE, PROCESS_TIMEOUT_UNIT) >= 0) {
                final String outputStream = exec.error();
                if (outputStream != null && !outputStream.isEmpty()) {
                    @SuppressWarnings("unchecked")
                    final List<HWAccelNvidiaSpec> specs = (List<HWAccelNvidiaSpec>) JsonUtils.loadJSON(
                            ConfigurationDir.getConfigResource("nvidia-matrix.json"),
                            HWAccelNvidiaSpec.class);

                    detectedHWAccels = new HWAccelsWrapper()
                            .setHWAccels(MatcherStream.findMatches(PATTERN_NVENC_GPU, outputStream).map(mr -> {
                                HWAccelWrapper<HWAccelNvidiaSpec> hwaccel = new HWAccelWrapper<>();

                                hwaccel.setType(HWAccelType.NVIDIA);
                                hwaccel.setIndex(Integer.parseInt(mr.group(1)));
                                hwaccel.setName(mr.group(2).trim());

                                specs.stream().filter(nv -> hwaccel.getName().contains(nv.getName())).findFirst()
                                        .ifPresent(nv -> hwaccel.setDeviceSpec(nv));

                                return hwaccel;
                            }).collect(Collectors.toList()));
                    return detectedHWAccels;
                }
            }
        } catch (InterruptedException | ExecutionException | JAXBException | IOException ex) {
            LOGGER.warn(ex.getMessage(), ex);
        }

        return null;
    }

    /** The Constant PATTERN_DURATION. */
    private static final Pattern PATTERN_DURATION = Pattern.compile("Duration: (.*), start:");

    /** The Constant PATTERN_CURRENT. */
    private static final Pattern PATTERN_CURRENT = Pattern.compile("time=([\\d:\\.]+)(?!.*time=[\\d:\\.]+)");

    /**
     * Returns the current progress value in percent.
     * 
     * @param stream the stream
     * @return the progress value
     */
    public static Integer progress(final String stream) {
        return Optional.ofNullable(stream).map(s -> {
            OptionalLong duration = MatcherStream.findMatches(PATTERN_DURATION, s)
                    .mapToLong(d -> parseMillis(d.group(1))).findFirst();
            OptionalLong current = MatcherStream.findMatches(PATTERN_CURRENT, s)
                    .mapToLong(c -> parseMillis(c.group(1))).max();

            return current.isPresent() && duration.isPresent()
                    ? (int) ((float) current.getAsLong() / (float) duration.getAsLong() * 100.0)
                    : null;
        }).orElse(null);
    }

    /**
     * Parses the millis.
     *
     * @param time the time
     * @return the long
     */
    private static long parseMillis(final String time) {
        long millis = 0;
        if (time != null && !time.isEmpty()) {
            final String[] tp = time.split("[:\\.]");

            if (tp.length == 4) {
                millis += Integer.parseInt(tp[0]) * 3600000;
                millis += Integer.parseInt(tp[1]) * 60000;
                millis += Integer.parseInt(tp[2]) * 1000;
                millis += Integer.parseInt(tp[3]);
            }
        }

        return millis;
    }

    /**
     * Checks if encoding supported for given input file.
     *  
     *
     * @param inputFile the input file
     * @return <code>true</code> if supported or <code>false</code> if not
     * @throws InterruptedException the interrupted exception
     * @throws JAXBException the JAXB exception
     * @throws ExecutionException the execution exception
     */
    public static boolean isEncodingSupported(final Path inputFile)
            throws InterruptedException, JAXBException, ExecutionException {
        final ProbeWrapper probe = probe(inputFile);

        if (probe != null && probe.getFormat() != null && probe.getStreams() != null) {
            return probe.getStreams().stream().filter(
                    s -> s.getCodecType().equalsIgnoreCase("audio") || s.getCodecType().equalsIgnoreCase("video"))
                    .map(s -> {
                        try {
                            return !codecs().getByName(s.getCodecName()).isEmpty();
                        } catch (Exception ex) {
                            return false;
                        }
                    }).filter(rt -> !rt).count() == 0;
        }

        return false;
    }

    /**
     * Checks if given scale factor against media files width and height if upscaling.
     *
     * @param inputFile the input file
     * @param scale the scale factor, in format <code>-1:720</code>
     * @return <code>true</code> if media file less than scale factor or <code>false</code> if not
     * @throws InterruptedException the interrupted exception
     * @throws JAXBException the JAXB exception
     * @throws ExecutionException the execution exception
     */
    public static boolean isUpscaling(final Path inputFile, final String scale)
            throws InterruptedException, JAXBException, ExecutionException {
        final ProbeWrapper probe = probe(inputFile);
        final Integer[] sc = Arrays.stream(scale.split(":")).map(Integer::new).toArray(Integer[]::new);

        if (probe != null && probe.getStreams() != null) {
            return probe.getStreams().stream().filter(s -> s.getCodecType().equalsIgnoreCase("video")).map(s -> {
                try {
                    return (sc[0] < 0 || (sc[0] > 0 && sc[0] <= s.getWidth()))
                            && (sc[1] < 0 || (sc[1] > 0 && sc[1] <= s.getHeight()));
                } catch (Exception ex) {
                    return true;
                }
            }).filter(rt -> rt).count() == 0;
        }

        return true;
    }

    /**
     * Can HW accelerate.
     *
     * @param outputs the outputs
     * @param hwAccel the hw accel
     * @return true, if successful
     */
    public static boolean canHWAccelerate(final List<Output> outputs,
            final HWAccelWrapper<? extends HWAccelDeviceSpec> hwAccel) {
        return hwAccel.getType() == HWAccelType.NVIDIA
                && Optional.ofNullable((HWAccelNvidiaSpec) hwAccel.getDeviceSpec()).map(ds -> ds.canUseEncoder())
                        .orElse(false)
                && outputs.stream().allMatch(o -> o.getVideo().getCodec().toLowerCase(Locale.ROOT)
                        .contains("nvenc"));
    }

    /**
     * Build the command line for given input path, a list of {@link Output} and a optional hw accelerator.
     *
     * @param processId the process Id
     * @param input the inut file
     * @param outputs the outputs
     * @param hwAccel the optional hw accelerator
     * @return the command
     * @throws InterruptedException the interrupted exception
     * @throws JAXBException the JAXB exception
     * @throws ExecutionException the execution exception
     */
    public static String command(final String processId, final Path input, final List<Output> outputs,
            final Optional<HWAccelWrapper<? extends HWAccelDeviceSpec>> hwAccel)
            throws InterruptedException, JAXBException, ExecutionException {
        final StringBuffer cmd = new StringBuffer();

        ProbeWrapper inInfo = probe(input);

        cmd.append("ffmpeg");

        buildInputStreamCommand(cmd, inInfo, processId, hwAccel);

        cmd.append(" -i \"" + input.toFile().getAbsolutePath() + "\" -stats -y");

        outputs.forEach(output -> {
            final Set<String> ambiguousParams = checkForAmbiguousParameters(output, hwAccel);
            buildVideoStreamCommand(cmd, inInfo, processId, output, hwAccel, ambiguousParams);
            buildAudioStreamCommand(cmd, inInfo, output, ambiguousParams);

            // Workaround to fix Too many packets buffered for output stream
            cmd.append(" -max_muxing_queue_size 512");

            cmd.append(" \"" + output.getOutputPath().toFile().getAbsolutePath() + "\"");
        });

        return cmd.toString();
    }

    /**
     * Builds the input stream command.
     *
     * @param cmd the cmd
     * @param inInfo the in info
     * @param processId the process id
     * @param hwAccel the hw accel
     */
    private static void buildInputStreamCommand(StringBuffer cmd, final ProbeWrapper inInfo, final String processId,
            final Optional<HWAccelWrapper<? extends HWAccelDeviceSpec>> hwAccel) {
        hwAccel.ifPresent(hw -> {
            CodecWrapper videoCodec = inInfo.getStreams().stream()
                    .filter(s -> s.getCodecType().equalsIgnoreCase("video"))
                    .findFirst()
                    .map(s -> {
                        return codecs().getByName(s.getCodecName()).stream().findFirst().orElse(null);
                    }).orElse(null);

            if (HWAccelType.NVIDIA == hw.getType()) {
                HWAccelNvidiaSpec devSpec = (HWAccelNvidiaSpec) hw.getDeviceSpec();
                if (videoCodec != null && devSpec.canUseDecoder()
                        && devSpec.getDecoders().entrySet().stream()
                                .anyMatch(e -> e.getKey().equalsIgnoreCase(videoCodec.getName()) && e.getValue())) {
                    videoCodec.getDecoderLib().stream().filter(s -> s.contains("cuvid")).findFirst().ifPresent(
                            dec -> {
                                devSpec.registerDecoderProcessId(processId);
                                cmd.append(" -hwaccel_device " + hw.getIndex() + " -hwaccel cuvid -c:v " + dec);
                            });
                }
            } else {
                hw.getDeviceSpec().registerProcessId(processId);
            }
        });
    }

    private static String getInputPixelFormat(ProbeWrapper pw) {
        return pw.getStreams().stream()
                .filter(s -> s.getCodecType().equalsIgnoreCase("video"))
                .findFirst()
                .map(StreamWrapper::getPixFmt).orElse(null);
    }

    /**
     * Builds the video stream command.
     *
     * @param cmd the cmd
     * @param processId the process id
     * @param output the output
     * @param hwAccel the hw accel
     * @param ambiguousParams the ambiguous params
     */
    private static void buildVideoStreamCommand(StringBuffer cmd, final ProbeWrapper inInfo, final String processId,
            final Output output,
            final Optional<HWAccelWrapper<? extends HWAccelDeviceSpec>> hwAccel, final Set<String> ambiguousParams) {
        Video video = hwAccel.isPresent() ? output.getVideo()
                : Optional.ofNullable(output.getVideoFallback()).orElse(output.getVideo());

        if (hwAccel.isPresent()) {
            buildHWAccelVideoStreamCommand(cmd, inInfo, processId, video, hwAccel);
        } else {
            Optional.ofNullable(video.getScale()).ifPresent(v -> cmd.append(" -filter:v 'scale=" + v + "'"));
            Optional.ofNullable(video.getPixelFormat()).filter(v -> !v.equalsIgnoreCase(getInputPixelFormat(inInfo)))
                    .ifPresent(v -> cmd.append(" -pix_fmt " + (!v.isEmpty() ? v : "yuv420p")));
        }

        cmd.append(" -codec:v " + video.getCodec());

        cmd.append(buildParameters(video.getParameters(), video.getCodec(), ambiguousParams, "v"));

        Optional.ofNullable(video.getFramerate()).ifPresent(v -> {
            cmd.append(" -r " + v);
            Optional.ofNullable(video.getFramerateType())
                    .ifPresent(t -> cmd.append(" -vsync " + ("CFR".equals(t) ? "1"
                            : "VFR".equals(t) ? "2" : "0")));
        });

        Optional.ofNullable(video.getForceKeyFrames())
                .ifPresent(v -> cmd.append(" -force_key_frames 'expr:gte(t,n_forced*" + v + ")'"));

        Optional.ofNullable(video.getQuality()).ifPresent(quality -> {
            switch (quality.getType()) {
            case "CRF":
                Optional.ofNullable(quality.getRateFactor()).ifPresent(v -> cmd.append(" -crf " + v));
                break;
            case "CQ":
                Optional.ofNullable(quality.getScale()).ifPresent(v -> cmd.append(" -qscale:v " + v));
                break;
            case "ABR":
                Optional.ofNullable(quality.getBitrate()).ifPresent(v -> cmd.append(" -b:v " + v + "k"));
                break;
            default:
                break;
            }

            Optional.ofNullable(quality.getMinrate()).ifPresent(v -> cmd.append(" -minrate " + v + "k"));
            Optional.ofNullable(quality.getMaxrate()).ifPresent(v -> cmd.append(" -maxrate " + v + "k"));
            Optional.ofNullable(quality.getBufsize()).ifPresent(v -> cmd.append(" -bufsize " + v + "k"));
        });
    }

    /**
     * Builds the HW accel video stream command.
     *
     * @param cmd the cmd
     * @param processId the process id
     * @param video the video
     * @param hwAccel the hw accel
     */
    private static void buildHWAccelVideoStreamCommand(StringBuffer cmd, final ProbeWrapper inInfo,
            final String processId, final Video video,
            final Optional<HWAccelWrapper<? extends HWAccelDeviceSpec>> hwAccel) {

        HWAccelWrapper<? extends HWAccelDeviceSpec> hw = hwAccel.get();
        if (HWAccelType.NVIDIA == hw.getType()) {
            HWAccelNvidiaSpec devSpec = (HWAccelNvidiaSpec) hw.getDeviceSpec();
            if (devSpec != null && (devSpec.canUseEncoder() || devSpec.canUseEncoder(processId))) {
                devSpec.registerEncoderProcessId(processId);
                cmd.append(" -gpu " + hw.getIndex());

                if (devSpec.canUseDecoder(processId)) {
                    if (filters().getFilters().stream()
                            .anyMatch(f -> "scale_npp".equalsIgnoreCase(f.getName()))) {
                        String pxf = Optional.ofNullable(video.getPixelFormat())
                                .filter(v -> !v.equalsIgnoreCase(getInputPixelFormat(inInfo)))
                                .map(v -> ":format=" + (!v.isEmpty() ? v : "same")).orElse("");

                        Optional.ofNullable(video.getScale())
                                .ifPresent(v -> cmd.append(
                                        " -filter:v 'scale_npp=" + v + pxf + ":interp_algo=lanczos'"));
                    } else {
                        LOGGER.warn(
                                "Couldn't use \"scale_npp\", ignore scale settings now. Compile FFMpeg with --enable-libnpp.");
                    }
                } else {
                    Optional.ofNullable(video.getScale())
                            .ifPresent(v -> cmd.append(" -filter:v 'scale=" + v + "'"));
                    Optional.ofNullable(video.getPixelFormat())
                            .filter(v -> !v.equalsIgnoreCase(getInputPixelFormat(inInfo)))
                            .ifPresent(v -> cmd.append(" -pix_fmt " + (!v.isEmpty() ? v : "yuv420p")));
                }
            }
        }
    }

    /**
     * Builds the audio stream command.
     *
     * @param cmd the cmd
     * @param inInfo the in info
     * @param output the output
     * @param ambiguousParams the ambiguous params
     */
    private static void buildAudioStreamCommand(StringBuffer cmd, final ProbeWrapper inInfo, final Output output,
            final Set<String> ambiguousParams) {
        Audio audio = output.getAudio();

        if (inInfo.getStreams().stream().anyMatch(s -> s.getCodecType().equalsIgnoreCase("audio"))) {
            cmd.append(" -codec:a " + audio.getCodec());

            cmd.append(buildParameters(audio.getParameters(), audio.getCodec(), ambiguousParams, "a"));

            Optional.ofNullable(audio.getBitrate()).ifPresent(v -> cmd.append(" -b:a " + v + "k"));
            Optional.ofNullable(audio.getSamplerate()).ifPresent(v -> cmd.append(" -ar " + v));
            cmd.append(" -ac 2");
        }
    }

    /**
     * Check for ambiguous parameters.
     *
     * @param output the output
     * @param hwAccel the hw accel
     * @return the list
     */
    private static Set<String> checkForAmbiguousParameters(final Output output,
            final Optional<HWAccelWrapper<? extends HWAccelDeviceSpec>> hwAccel) {
        Video video = hwAccel.isPresent() ? output.getVideo()
                : Optional.ofNullable(output.getVideoFallback()).orElse(output.getVideo());
        Audio audio = output.getAudio();

        EncodersWrapper vencs;
        try {
            vencs = encoder(video.getCodec());
        } catch (NumberFormatException e) {
            vencs = null;
        }

        EncodersWrapper aencs;
        try {
            aencs = encoder(audio.getCodec());
        } catch (NumberFormatException e) {
            aencs = null;
        }

        List<String> knownAmbiguous = Arrays.asList("b", "profile");

        List<String> vparams = Stream.concat(knownAmbiguous.stream(), Optional.ofNullable(vencs.getEncoders())
                .map(encs -> encs.stream().filter(e -> e.getName().equalsIgnoreCase(video.getCodec())).findFirst()
                        .orElse(null))
                .map(e -> e.getParameters().stream().map(p -> p.getName()).sorted())
                .orElse(Stream.empty())).collect(Collectors.toList());

        List<String> aparams = Stream.concat(knownAmbiguous.stream(), Optional.ofNullable(aencs.getEncoders())
                .map(encs -> encs.stream().filter(e -> e.getName().equalsIgnoreCase(audio.getCodec())).findFirst()
                        .orElse(null))
                .map(e -> e.getParameters().stream().map(p -> p.getName()).sorted())
                .orElse(Stream.empty())).collect(Collectors.toList());

        return vparams.stream().filter(p -> aparams.contains(p)).collect(Collectors.toSet());
    }

    /**
     * Builds the parameters.
     *
     * @param parameters the parameters
     * @param codec the codec
     * @param ambiguousParams the ambiguous params
     * @param paramSuffix the param suffix
     * @return the string
     */
    private static String buildParameters(Map<String, String> parameters, String codec, Set<String> ambiguousParams,
            String paramSuffix) {
        StringBuffer cmd = new StringBuffer();

        Optional.ofNullable(parameters).ifPresent(params -> {
            EncodersWrapper encoders;
            try {
                encoders = encoder(codec);
            } catch (NumberFormatException e) {
                encoders = null;
            }

            EncoderWrapper enc = Optional.ofNullable(encoders.getEncoders())
                    .map(encs -> encs.stream().filter(e -> e.getName().equalsIgnoreCase(codec)).findFirst()
                            .orElse(null))
                    .orElse(null);

            params.entrySet().stream()
                    .filter(entry -> enc != null && enc.getParameters().stream()
                            .filter(p -> p.getName().equals(entry.getKey()) && (p.getDefaultValue() == null
                                    || !p.getDefaultValue().equals(entry.getValue())))
                            .findAny().isPresent())
                    .forEach(
                            entry -> cmd.append(
                                    " -" + entry.getKey()
                                            + (ambiguousParams.contains(entry.getKey()) ? ":" + paramSuffix : "")
                                            + " "
                                            + ("true".equalsIgnoreCase(entry.getValue()) ? "1"
                                                    : "false".equalsIgnoreCase(entry.getValue()) ? "0"
                                                            : entry.getValue())));
        });

        return cmd.toString();
    }

    /**
     * Builds filename for given format.
     *
     * @param format the output format
     * @param fileName the input file name
     * @param appendix the filename appendix
     * @return the string
     */
    public static String filename(final String format, final String fileName, final String appendix) {
        final String extension = muxer(format).getExtension();
        return fileName.substring(0, fileName.lastIndexOf('.'))
                + Optional.ofNullable(appendix).orElse("") + "." + extension;
    }

    /**
     * Returns file informations.
     *
     * @param inputFile the input file
     * @return the informations as {@link ProbeWrapper}
     * @throws InterruptedException the interrupted exception
     * @throws JAXBException the JAXB exception
     * @throws ExecutionException the execution exception
     */
    public static ProbeWrapper probe(final Path inputFile)
            throws InterruptedException, JAXBException, ExecutionException {
        final Cache<Path, ProbeWrapper> cache = CACHE_MGR.getCache("probe", Path.class,
                ProbeWrapper.class);

        if (cache.containsKey(inputFile)) {
            return cache.get(inputFile);
        }

        final Executable exec = new Executable("ffprobe", "-v", "quiet", "-print_format", "xml", "-show_format",
                "-show_streams",
                inputFile.toFile().getAbsolutePath());

        if (exec.runAndWait(PROCESS_TIMEOUT_VALUE, PROCESS_TIMEOUT_UNIT) == 0) {
            final String outputStream = exec.output();

            if (outputStream != null && !outputStream.isEmpty()) {
                final JAXBContext jc = JAXBContext.newInstance(ProbeWrapper.class);
                final Unmarshaller unmarshaller = jc.createUnmarshaller();

                try (final StringReader sr = new StringReader(outputStream)) {
                    final ProbeWrapper pw = unmarshaller
                            .unmarshal(new StreamSource(sr), ProbeWrapper.class)
                            .getValue();

                    cache.put(inputFile, pw);
                    return pw;
                }
            }
        }

        return null;
    }

    /**
     * Split string.
     *
     * @param str the str
     * @return the stream
     */
    private static Stream<String> splitString(final String str) {
        return Arrays.stream(str.split("\\s")).filter(s -> !s.isEmpty());
    }

    /**
     * Gets the pattern group.
     *
     * @param pattern the pattern
     * @param str the str
     * @param group the group
     * @return the pattern group
     */
    private static String getPatternGroup(final Pattern pattern, final String str, int group) {
        return MatcherStream.findMatches(pattern, str).findFirst().map(m -> m.group(group).trim()).orElse(null);
    }
}
