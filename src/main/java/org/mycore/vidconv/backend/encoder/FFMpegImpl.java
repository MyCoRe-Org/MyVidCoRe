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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.regex.Matcher;
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

    /**
     * Inits the.
     */
    @Startup
    protected static void init() {
        try {
            LOGGER.info("parse codecs...");
            LOGGER.info("...found {}.", codecs().getCodecs().size());

            LOGGER.info("parse filters...");
            LOGGER.info("...found {}.", filters().getFilters().size());

            LOGGER.info("parse formats...");
            LOGGER.info("...found {}.", formats().getFormats().size());

            LOGGER.info("detect hw accelerators...");
            HWAccelsWrapper hwaccels = detectHWAccels();
            if (hwaccels != null && !hwaccels.getHWAccels().isEmpty()) {
                hwaccels.getHWAccels()
                    .forEach(hw -> LOGGER.info("...found {} {} ({}).", hw.getIndex(), hw.getName(), hw.getType()));
            } else {
                LOGGER.info("...none found.");
            }
        } catch (InterruptedException | ExecutionException e) {
            LOGGER.error(e.getMessage(), e);
        }
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
     * @throws InterruptedException the interrupted exception
     * @throws ExecutionException the execution exception
     */
    public static CodecsWrapper codecs() throws InterruptedException, ExecutionException {
        if (supportedCodecs != null) {
            return supportedCodecs;
        }

        final Executable exec = new Executable("ffmpeg", "-codecs");

        if (exec.runAndWait() == 0) {
            final String outputStream = exec.output();

            if (outputStream != null && !outputStream.isEmpty()) {
                final List<CodecWrapper> codecs = new ArrayList<>();
                final Matcher m = PATTERN_CODECS.matcher(outputStream);

                while (m.find()) {
                    final CodecWrapper codec = new CodecWrapper();

                    switch (m.group(3)) {
                        case "A":
                            codec.setType(CodecWrapper.Type.AUDIO);
                            break;
                        case "V":
                            codec.setType(CodecWrapper.Type.VIDEO);
                            break;
                        case "S":
                            codec.setType(CodecWrapper.Type.SUBTITLE);
                            break;
                        default:
                            continue;
                    }

                    codec.setLossy(m.group(5).equalsIgnoreCase("L"));
                    codec.setLossless(m.group(6).equalsIgnoreCase("S"));

                    codec.setName(m.group(7));
                    String desc = m.group(8).trim();
                    if (desc != null) {
                        final Matcher em = PATTERN_ENCODER_LIB.matcher(desc);
                        while (em.find()) {
                            codec.setEncoderLib(splitString(em.group(1)).collect(Collectors.toList()));
                        }
                        final Matcher dm = PATTERN_DECODER_LIB.matcher(desc);
                        while (dm.find()) {
                            codec.setDecoderLib(splitString(dm.group(1)).collect(Collectors.toList()));
                        }
                        desc = desc.replaceAll(PATTERN_DECODER_LIB.pattern(), "");
                        desc = desc.replaceAll(PATTERN_ENCODER_LIB.pattern(), "");
                    }
                    codec.setDescription(desc.trim());
                    codecs.add(codec);
                }

                supportedCodecs = new CodecsWrapper().setCodecs(codecs);
                return supportedCodecs;
            }
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
     * @throws InterruptedException the interrupted exception
     * @throws ExecutionException the execution exception
     */
    public static FiltersWrapper filters() throws InterruptedException, ExecutionException {
        if (supportedFilters != null) {
            return supportedFilters;
        }

        final Executable exec = new Executable("ffmpeg", "-filters");

        if (exec.runAndWait() == 0) {
            final String outputStream = exec.output();

            if (outputStream != null && !outputStream.isEmpty()) {
                final List<FilterWrapper> filters = new ArrayList<>();
                final Matcher m = PATTERN_FILTERS.matcher(outputStream);

                while (m.find()) {
                    final FilterWrapper filter = new FilterWrapper();

                    filter.setTimelineSupport(m.group(1).equalsIgnoreCase("T"));
                    filter.setSliceSupport(m.group(2).equalsIgnoreCase("S"));
                    filter.setCommandSupport(m.group(3).equalsIgnoreCase("C"));
                    filter.setName(m.group(4).trim());
                    filter.setIoSupport(m.group(5).trim());
                    filter.setDescription(m.group(6).trim());

                    filters.add(filter);
                }

                supportedFilters = new FiltersWrapper().setFilters(filters);
                return supportedFilters;
            }
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
     * @throws InterruptedException the interrupted exception
     * @throws ExecutionException the execution exception
     */
    public static FormatsWrapper formats() throws InterruptedException, ExecutionException {
        if (supportedFormats != null) {
            return supportedFormats;
        }

        final Executable exec = new Executable("ffmpeg", "-formats");

        if (exec.runAndWait() == 0) {
            final String outputStream = exec.output();

            if (outputStream != null && !outputStream.isEmpty()) {
                final List<FormatWrapper> formats = new ArrayList<>();
                final Matcher m = PATTERN_FORMATS.matcher(outputStream);

                while (m.find()) {
                    final FormatWrapper format = new FormatWrapper();

                    format.setDemuxer(m.group(1).equalsIgnoreCase("D"));
                    format.setMuxer(m.group(2).equalsIgnoreCase("E"));
                    format.setName(m.group(3).trim());
                    format.setDescription(m.group(4).trim());

                    formats.add(format);
                }

                supportedFormats = new FormatsWrapper().setFormats(formats);
                return supportedFormats;
            }
        }

        return null;
    }

    /** The Constant PATTERN_DECODER. */
    private static final Pattern PATTERN_DECODER = Pattern
        .compile("^Decoder\\s([^\\s]+)\\s\\[([^\\]]+)\\]:\\n([\\S\\s]+)$");

    /** The supported decoders. */
    private static Map<String, DecodersWrapper> supportedDecoders = new ConcurrentHashMap<>();

    /**
     * Returns informations for given decoder.
     *
     * @param name the name
     * @return the decoders wrapper
     * @throws InterruptedException the interrupted exception
     * @throws NumberFormatException the number format exception
     * @throws ExecutionException the execution exception
     */
    public static DecodersWrapper decoder(final String name)
        throws InterruptedException, NumberFormatException, ExecutionException {
        if (supportedDecoders.containsKey(name)) {
            return supportedDecoders.get(name);
        }

        final Executable exec = new Executable("ffmpeg", "-h", "decoder=" + name);

        if (exec.runAndWait() == 0) {
            final String outputStream = exec.output();

            if (outputStream != null && !outputStream.isEmpty()) {
                final List<DecoderWrapper> decoders = PATTERN_ENTRY_SPLIT.splitAsStream(outputStream)
                    .filter(os -> !os.isEmpty())
                    .map(os -> {
                        final Matcher m = PATTERN_DECODER.matcher(os);
                        if (m.find()) {
                            final DecoderWrapper decoder = new DecoderWrapper();

                            decoder.setName(m.group(1));
                            decoder.setDescription(m.group(2));

                            final List<ParameterWrapper> parameters = new ArrayList<>();
                            final Matcher pm = PATTERN_PARAMS.matcher(m.group(3));
                            while (pm.find()) {
                                final ParameterWrapper param = new ParameterWrapper();
                                param.setName(pm.group(1));
                                param.setType(pm.group(2));
                                param.setDescription(pm.group(3));

                                Optional.ofNullable(getPatternGroup(PATTERN_PARAM_FROM_TO, pm.group(3), 1))
                                    .ifPresent(v -> param.setFromValue(v));
                                Optional.ofNullable(getPatternGroup(PATTERN_PARAM_FROM_TO, pm.group(3), 2))
                                    .ifPresent(v -> param.setToValue(v));
                                Optional.ofNullable(getPatternGroup(PATTERN_PARAM_DEFAULT, pm.group(3), 1))
                                    .ifPresent(v -> param.setDefaultValue(v));

                                Optional.ofNullable(pm.group(4)).ifPresent(vs -> {
                                    if (!vs.isEmpty()) {
                                        final List<ParameterValue> values = new ArrayList<>();
                                        final Matcher pv = PATTERN_PARAM_VALUES.matcher(vs);
                                        while (pv.find()) {
                                            final ParameterValue value = new ParameterValue();
                                            value.setName(pv.group(1));
                                            Optional.ofNullable(pv.group(2)).ifPresent(v -> {
                                                v = v.trim();
                                                if (!v.isEmpty())
                                                    value.setDescription(v);
                                            });
                                            values.add(value);
                                        }
                                        if (!values.isEmpty())
                                            param.setValues(values);
                                    }
                                });

                                parameters.add(param);
                            }
                            decoder.setParameters(parameters);

                            return decoder;
                        }

                        return null;
                    }).filter(e -> e != null).collect(Collectors.toList());

                supportedDecoders.put(name, new DecodersWrapper().setDecoders(decoders));
                return supportedDecoders.get(name);
            }
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

    /** The Constant PATTERN_PARAMS. */
    private static final Pattern PATTERN_PARAMS = Pattern
        .compile("\\s+-([^\\s]+)\\s+<([^>]+)>\\s+(?:[^\\s]+)\\s([^\\n]+)([\\S\\s]+?(?=\\s+-))?");

    /** The Constant PATTERN_PARAM_VALUES. */
    private static final Pattern PATTERN_PARAM_VALUES = Pattern.compile("\\s+([^\\s]+)\\s+(?:[^\\s]+)([^\\n]*)");

    /** The Constant PATTERN_PARAM_FROM_TO. */
    private static final Pattern PATTERN_PARAM_FROM_TO = Pattern
        .compile("\\(from\\s([^\\s]+)\\sto\\s([^\\s]+)\\)");

    /** The Constant PATTERN_PARAM_DEFAULT. */
    private static final Pattern PATTERN_PARAM_DEFAULT = Pattern
        .compile("\\(default\\s([^\\)]+)\\)");

    /** The supported encoders. */
    private static Map<String, EncodersWrapper> supportedEncoders = new ConcurrentHashMap<>();

    /**
     * Returns informations for given encoder.
     *
     * @param name the encoder name
     * @return the encoders wrapper
     * @throws InterruptedException the interrupted exception
     * @throws NumberFormatException the number format exception
     * @throws ExecutionException the execution exception
     */
    public static EncodersWrapper encoder(final String name)
        throws InterruptedException, NumberFormatException, ExecutionException {
        if (supportedEncoders.containsKey(name)) {
            return supportedEncoders.get(name);
        }

        final Executable exec = new Executable("ffmpeg", "-h", "encoder=" + name);

        if (exec.runAndWait() == 0) {
            final String outputStream = exec.output();

            if (outputStream != null && !outputStream.isEmpty()) {
                final List<EncoderWrapper> encoders = PATTERN_ENTRY_SPLIT.splitAsStream(outputStream)
                    .filter(os -> !os.isEmpty())
                    .map(os -> {
                        final Matcher m = PATTERN_ENCODER.matcher(os);
                        if (m.find()) {
                            final EncoderWrapper encoder = new EncoderWrapper();

                            encoder.setName(m.group(1));
                            encoder.setDescription(m.group(2));

                            encoder.setPixelFormats(
                                Stream.of(m.group(3)).map(s -> PATTERN_PIX_FMT.matcher(s))
                                    .filter(ma -> ma.find())
                                    .flatMap(ma -> splitString(ma.group(1)))
                                    .collect(Collectors.toList()));

                            encoder.setFrameRates(
                                Stream.of(m.group(3)).map(s -> PATTERN_FRM_RATES.matcher(s))
                                    .filter(ma -> ma.find())
                                    .flatMap(ma -> splitString(ma.group(1)))
                                    .collect(Collectors.toList()));

                            encoder.setSampleFormats(
                                Stream.of(m.group(3)).map(s -> PATTERN_SMP_FROMATS.matcher(s))
                                    .filter(ma -> ma.find())
                                    .flatMap(ma -> splitString(ma.group(1)))
                                    .collect(Collectors.toList()));

                            encoder.setSampleRates(
                                Stream.of(m.group(3)).map(s -> PATTERN_SMP_RATES.matcher(s))
                                    .filter(ma -> ma.find())
                                    .flatMap(ma -> splitString(ma.group(1))).map(s -> new Integer(s))
                                    .collect(Collectors.toList()));

                            encoder.setChannelLayouts(
                                Stream.of(m.group(3)).map(s -> PATTERN_CH_LAYOUTS.matcher(s))
                                    .filter(ma -> ma.find())
                                    .flatMap(ma -> splitString(ma.group(1)))
                                    .collect(Collectors.toList()));

                            final List<ParameterWrapper> parameters = new ArrayList<>();
                            final Matcher pm = PATTERN_PARAMS.matcher(m.group(3));
                            while (pm.find()) {
                                final ParameterWrapper param = new ParameterWrapper();
                                param.setName(pm.group(1));
                                param.setType(pm.group(2));
                                param.setDescription(pm.group(3));

                                Optional.ofNullable(getPatternGroup(PATTERN_PARAM_FROM_TO, pm.group(3), 1))
                                    .ifPresent(v -> param.setFromValue(v));
                                Optional.ofNullable(getPatternGroup(PATTERN_PARAM_FROM_TO, pm.group(3), 2))
                                    .ifPresent(v -> param.setToValue(v));
                                Optional.ofNullable(getPatternGroup(PATTERN_PARAM_DEFAULT, pm.group(3), 1))
                                    .ifPresent(v -> param.setDefaultValue(v));

                                Optional.ofNullable(pm.group(4)).ifPresent(vs -> {
                                    if (!vs.isEmpty()) {
                                        final List<ParameterValue> values = new ArrayList<>();
                                        final Matcher pv = PATTERN_PARAM_VALUES.matcher(vs);
                                        while (pv.find()) {
                                            final ParameterValue value = new ParameterValue();
                                            value.setName(pv.group(1));
                                            Optional.ofNullable(pv.group(2)).ifPresent(v -> {
                                                v = v.trim();
                                                if (!v.isEmpty())
                                                    value.setDescription(v);
                                            });
                                            values.add(value);
                                        }
                                        if (!values.isEmpty())
                                            param.setValues(values);
                                    }
                                });

                                parameters.add(param);
                            }
                            encoder.setParameters(parameters);

                            return encoder;
                        }

                        return null;
                    }).filter(e -> e != null).collect(Collectors.toList());

                supportedEncoders.put(name, new EncodersWrapper().setEncoders(encoders));
                return supportedEncoders.get(name);
            }
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
     * @throws InterruptedException the interrupted exception
     * @throws ExecutionException the execution exception
     */
    public static MuxerWrapper muxer(final String name) throws InterruptedException, ExecutionException {
        if (supportedMuxers.containsKey(name)) {
            return supportedMuxers.get(name);
        }

        final Executable exec = new Executable("ffmpeg", "-h", "muxer=" + name);

        if (exec.runAndWait() == 0) {
            final String outputStream = exec.output();

            if (outputStream != null && !outputStream.isEmpty()) {
                final Matcher m = PATTERN_MUXER.matcher(outputStream);
                while (m.find()) {
                    final MuxerWrapper muxer = new MuxerWrapper();

                    muxer.setName(m.group(1));

                    muxer.setExtension(getPatternGroup(PATTERN_EXTENSION, m.group(2), 1));
                    muxer.setMimeType(getPatternGroup(PATTERN_MIME_TYPE, m.group(2), 1));
                    muxer.setAudioCodec(getPatternGroup(PATTERN_AUDIO_CODEC, m.group(2), 1));
                    muxer.setVideoCodec(getPatternGroup(PATTERN_VIDEO_CODEC, m.group(2), 1));
                    muxer.setSubtitleCodec(getPatternGroup(PATTERN_SUBTITLE_CODEC, m.group(2), 1));

                    supportedMuxers.put(name, muxer);
                }

                return supportedMuxers.get(name);
            }
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
            if (exec.runAndWait() >= 0) {
                final String outputStream = exec.error();
                if (outputStream != null && !outputStream.isEmpty()) {
                    final List<HWAccelWrapper<? extends HWAccelDeviceSpec>> hwaccels = new ArrayList<>();
                    final Matcher m = PATTERN_NVENC_GPU.matcher(outputStream);

                    @SuppressWarnings("unchecked")
                    final List<HWAccelNvidiaSpec> specs = (List<HWAccelNvidiaSpec>) JsonUtils.loadJSON(
                        ConfigurationDir.getConfigResource("nvidia-matrix.json"),
                        HWAccelNvidiaSpec.class);

                    while (m.find()) {
                        HWAccelWrapper<HWAccelNvidiaSpec> hwaccel = new HWAccelWrapper<>();

                        hwaccel.setType(HWAccelType.NVIDIA);
                        hwaccel.setIndex(Integer.parseInt(m.group(1)));
                        hwaccel.setName(m.group(2).trim());

                        specs.stream().filter(nv -> hwaccel.getName().contains(nv.getName())).findFirst()
                            .ifPresent(nv -> hwaccel.setDeviceSpec(nv));

                        hwaccels.add(hwaccel);
                    }

                    detectedHWAccels = new HWAccelsWrapper().setHWAccels(hwaccels);
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
        if (stream != null) {
            final Matcher dm = PATTERN_DURATION.matcher(stream);
            final Matcher cm = PATTERN_CURRENT.matcher(stream);

            if (dm.find() && cm.find()) {
                long duration = parseMillis(dm.group(1));

                String last = "";
                while (cm.find()) {
                    last = cm.group(1);
                }
                long current = parseMillis(last);

                return (int) ((float) current / (float) duration * 100.0);
            }
        }

        return null;
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
        return hwAccel.getType() == HWAccelType.NVIDIA && ((HWAccelNvidiaSpec) hwAccel.getDeviceSpec()).canUseEncoder()
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
     */
    public static String command(final String processId, final Path input, final List<Output> outputs,
        final Optional<HWAccelWrapper<? extends HWAccelDeviceSpec>> hwAccel) {
        final StringBuffer cmd = new StringBuffer();

        cmd.append("ffmpeg");

        buildInputStreamCommand(cmd, processId, input, hwAccel);

        cmd.append(" -i \"" + input.toFile().getAbsolutePath() + "\" -stats -y");

        outputs.forEach(output -> {
            buildVideoStreamCommand(cmd, processId, output, hwAccel);
            buildAudioStreamCommand(cmd, output);
        });

        return cmd.toString();
    }

    private static void buildInputStreamCommand(StringBuffer cmd, final String processId, final Path input,
        final Optional<HWAccelWrapper<? extends HWAccelDeviceSpec>> hwAccel) {
        hwAccel.ifPresent(hw -> {
            try {
                ProbeWrapper inInfo = probe(input);
                CodecWrapper videoCodec = inInfo.getStreams().stream()
                    .filter(s -> s.getCodecType().equalsIgnoreCase("video"))
                    .findFirst()
                    .map(s -> {
                        try {
                            return codecs().getByName(s.getCodecName()).stream().findFirst().orElse(null);
                        } catch (InterruptedException | ExecutionException e) {
                            return null;
                        }
                    }).orElse(null);

                if (HWAccelType.NVIDIA == hw.getType()) {
                    HWAccelNvidiaSpec devSpec = (HWAccelNvidiaSpec) hw.getDeviceSpec();
                    if (videoCodec != null && devSpec.canUseDecoder(processId)
                        && devSpec.getDecoders().entrySet().stream()
                            .anyMatch(e -> e.getKey().equalsIgnoreCase(videoCodec.getName()) && e.getValue())) {
                        videoCodec.getDecoderLib().stream().filter(s -> s.contains("cuvid")).findFirst().ifPresent(
                            dec -> cmd.append(" -hwaccel_device " + hw.getIndex() + " -hwaccel cuvid -c:v " + dec));
                    }
                }
            } catch (InterruptedException | JAXBException | ExecutionException e) {
                LOGGER.warn("Couldn't get media informations for file {}", input);
            }
        });
    }

    private static void buildVideoStreamCommand(StringBuffer cmd, final String processId, final Output output,
        final Optional<HWAccelWrapper<? extends HWAccelDeviceSpec>> hwAccel) {
        Video video = output.getVideo();

        if (hwAccel.isPresent()) {
            buildHWAccelVideoStreamCommand(cmd, processId, output, hwAccel);
        } else {
            Optional.ofNullable(video.getScale()).ifPresent(v -> cmd.append(" -vf 'scale=" + v + "'"));
        }

        cmd.append(" -codec:v " + video.getCodec());

        cmd.append(buildParameters(video.getParameters(), video.getCodec()));

        Optional.ofNullable(video.getPixelFormat())
            .ifPresent(v -> cmd.append(" -pix_fmt " + (!v.isEmpty() ? v : "yuv420p")));

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

    private static void buildHWAccelVideoStreamCommand(StringBuffer cmd, final String processId, final Output output,
        final Optional<HWAccelWrapper<? extends HWAccelDeviceSpec>> hwAccel) {
        Video video = output.getVideo();

        HWAccelWrapper<? extends HWAccelDeviceSpec> hw = hwAccel.get();
        if (HWAccelType.NVIDIA == hw.getType()) {
            HWAccelNvidiaSpec devSpec = (HWAccelNvidiaSpec) hw.getDeviceSpec();
            if (devSpec.canUseEncoder(processId)) {
                cmd.append(" -gpu " + hw.getIndex());

                if (devSpec.canUseDecoder(processId)) {
                    if (supportedFilters.getFilters().stream()
                        .anyMatch(f -> "scale_npp".equalsIgnoreCase(f.getName()))) {
                        Optional.ofNullable(video.getScale()).ifPresent(v -> cmd.append(" -vf 'scale_npp=" + v + "'"));
                    } else {
                        LOGGER.warn(
                            "Couldn't use \"scale_npp\", ignore scale settings now. Compile FFMpeg with --enable-libnpp.");
                    }
                } else {
                    Optional.ofNullable(video.getScale()).ifPresent(v -> cmd.append(" -vf 'scale=" + v + "'"));
                }
            }
        }
    }

    private static void buildAudioStreamCommand(StringBuffer cmd, final Output output) {
        Audio audio = output.getAudio();

        cmd.append(" -codec:a " + audio.getCodec());

        cmd.append(buildParameters(audio.getParameters(), audio.getCodec()));

        Optional.ofNullable(audio.getBitrate()).ifPresent(v -> cmd.append(" -b:a " + v + "k"));
        Optional.ofNullable(audio.getSamplerate()).ifPresent(v -> cmd.append(" -ar " + v));
        cmd.append(" -ac 2");

        cmd.append(" \"" + output.getOutputPath().toFile().getAbsolutePath() + "\"");
    }

    /**
     * Builds the parameters.
     *
     * @param parameters the parameters
     * @param codec the codec
     * @return the string
     */
    private static String buildParameters(Map<String, String> parameters, String codec) {
        StringBuffer cmd = new StringBuffer();

        Optional.ofNullable(parameters).ifPresent(params -> {
            EncodersWrapper encoders;
            try {
                encoders = encoder(codec);
            } catch (NumberFormatException | InterruptedException | ExecutionException e1) {
                encoders = null;
            }

            EncoderWrapper enc = Optional.ofNullable(encoders.getEncoders())
                .map(encs -> encs.stream().filter(e -> e.getName().equalsIgnoreCase(codec)).findFirst()
                    .orElse(null))
                .orElse(null);

            params.entrySet().stream().filter(
                entry -> enc == null || enc.getParameters() != null
                    && enc.getParameters().stream().noneMatch(p -> p.getName().equals(entry.getKey())
                        && (p.getDefaultValue() != null && p.getDefaultValue().equals(entry.getValue()))))
                .forEach(
                    entry -> cmd.append(" -" + entry.getKey() + " " + ("true".equalsIgnoreCase(entry.getValue()) ? "1"
                        : "false".equalsIgnoreCase(entry.getValue()) ? "0" : entry.getValue())));
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
     * @throws ExecutionException the execution exception
     */
    public static String filename(final String format, final String fileName, final String appendix)
        throws ExecutionException {
        try {
            final String extension = muxer(format).getExtension();
            return fileName.substring(0, fileName.lastIndexOf('.'))
                + Optional.ofNullable(appendix).orElse("") + "." + extension;
        } catch (InterruptedException e) {
            return null;
        }
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

        if (exec.runAndWait() == 0) {
            final String outputStream = exec.output();

            if (outputStream != null && !outputStream.isEmpty()) {
                final JAXBContext jc = JAXBContext.newInstance(ProbeWrapper.class);
                final Unmarshaller unmarshaller = jc.createUnmarshaller();

                final ProbeWrapper pw = unmarshaller
                    .unmarshal(new StreamSource(new StringReader(outputStream)), ProbeWrapper.class)
                    .getValue();

                cache.put(inputFile, pw);
                return pw;
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
        final Matcher m = pattern.matcher(str);
        if (m.find()) {
            return m.group(group).trim();
        }

        return null;
    }
}
