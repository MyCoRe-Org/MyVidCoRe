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

import org.ehcache.Cache;
import org.ehcache.CacheManager;
import org.ehcache.config.builders.CacheConfigurationBuilder;
import org.ehcache.config.builders.CacheManagerBuilder;
import org.ehcache.config.builders.ResourcePoolsBuilder;
import org.mycore.vidconv.common.util.Executable;
import org.mycore.vidconv.frontend.entity.CodecWrapper;
import org.mycore.vidconv.frontend.entity.CodecsWrapper;
import org.mycore.vidconv.frontend.entity.EncoderWrapper;
import org.mycore.vidconv.frontend.entity.EncodersWrapper;
import org.mycore.vidconv.frontend.entity.FormatWrapper;
import org.mycore.vidconv.frontend.entity.FormatsWrapper;
import org.mycore.vidconv.frontend.entity.MuxerWrapper;
import org.mycore.vidconv.frontend.entity.ParameterWrapper;
import org.mycore.vidconv.frontend.entity.SettingsWrapper;
import org.mycore.vidconv.frontend.entity.ParameterWrapper.ParameterValue;
import org.mycore.vidconv.frontend.entity.SettingsWrapper.Audio;
import org.mycore.vidconv.frontend.entity.SettingsWrapper.Output;
import org.mycore.vidconv.frontend.entity.SettingsWrapper.Video;
import org.mycore.vidconv.frontend.entity.probe.ProbeWrapper;

/**
 * @author Ren\u00E9 Adler (eagle)
 *
 */
public class FFMpegImpl {
    private static final CacheManager CACHE_MGR = CacheManagerBuilder.newCacheManagerBuilder()
        .withCache("probe",
            CacheConfigurationBuilder.newCacheConfigurationBuilder(Path.class, ProbeWrapper.class,
                ResourcePoolsBuilder.heap(100))
                .build())
        .build(true);

    private static final Pattern PATTERN_ENTRY_SPLIT = Pattern.compile("\\n\\n");

    private static final Pattern PATTERN_CODECS = Pattern
        .compile("\\s(D|\\.|\\s)(E|\\.|\\s)(V|A|S|\\s)(I|\\.|\\s)(L|\\.|\\s)(S|\\.|\\s)\\s([^=\\s\\t]+)([^\\n]+)");

    private static final Pattern PATTERN_ENCODER_LIB = Pattern.compile("\\(encoders:\\s([^\\)]+)\\)");

    private static final Pattern PATTERN_DECODER_LIB = Pattern.compile("\\(decoders:\\s([^\\)]+)\\)");

    private static CodecsWrapper supportedCodecs;

    /**
     * Returns all supported codecs.
     * 
     * @return the supported codecs
     * @throws IOException
     * @throws InterruptedException
     * @throws ExecutionException 
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

    private static final Pattern PATTERN_FORMATS = Pattern
        .compile("\\s(D|\\.|\\s)(E|\\.|\\s)\\s([^=\\s\\t]+)([^\\n]+)");

    private static FormatsWrapper supportedFormats;

    /**
     * Returns all supported formats.
     * 
     * @return the supported formats
     * @throws IOException
     * @throws InterruptedException
     * @throws ExecutionException 
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

    private static final Pattern PATTERN_ENCODER = Pattern
        .compile("^Encoder\\s([^\\s]+)\\s\\[([^\\]]+)\\]:\\n([\\S\\s]+)$");

    private static final Pattern PATTERN_PIX_FMT = Pattern.compile("pixel formats:(.*)");

    private static final Pattern PATTERN_FRM_RATES = Pattern.compile("framerates:(.*)");

    private static final Pattern PATTERN_SMP_RATES = Pattern.compile("sample rates:(.*)");

    private static final Pattern PATTERN_SMP_FROMATS = Pattern.compile("sample formats:(.*)");

    private static final Pattern PATTERN_CH_LAYOUTS = Pattern.compile("channel layouts:(.*)");

    private static final Pattern PATTERN_PARAMS = Pattern
        .compile("\\s+-([^\\s]+)\\s+<([^>]+)>\\s+(?:[^\\s]+)\\s([^\\n]+)([\\S\\s]+?(?=\\s+-))?");

    private static final Pattern PATTERN_PARAM_VALUES = Pattern.compile("\\s+([^\\s]+)\\s+(?:[^\\s]+)([^\\n]*)");

    private static final Pattern PATTERN_PARAM_FROM_TO = Pattern
        .compile("\\(from\\s([^\\s]+)\\sto\\s([^\\s]+)\\)");

    private static final Pattern PATTERN_PARAM_DEFAULT = Pattern
        .compile("\\(default\\s([^\\)]+)\\)");

    private static Map<String, EncodersWrapper> supportedEncoders = new ConcurrentHashMap<>();

    /**
     * Returns informations for given encoder.
     * 
     * @param name the encoder name
     * @throws IOException
     * @throws InterruptedException
     * @throws ExecutionException 
     * @throws NumberFormatException 
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
                        while (m.find()) {
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
                            if (pm.find()) {
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

    private static final Pattern PATTERN_MUXER = Pattern
        .compile("^Muxer\\s([^\\s]+)\\s\\[(?:[^\\]]+)\\]:\\n([\\S\\s]+)$");

    private static final Pattern PATTERN_EXTENSION = Pattern.compile("Common extensions:(.*)\\.");

    private static final Pattern PATTERN_MIME_TYPE = Pattern.compile("Mime type:(.*)\\.");

    private static final Pattern PATTERN_AUDIO_CODEC = Pattern.compile("audio codec:(.*)\\.");

    private static final Pattern PATTERN_VIDEO_CODEC = Pattern.compile("video codec:(.*)\\.");

    private static final Pattern PATTERN_SUBTITLE_CODEC = Pattern.compile("video codec:(.*)\\.");

    private static Map<String, MuxerWrapper> supportedMuxers = new ConcurrentHashMap<>();

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

    private static final Pattern PATTERN_DURATION = Pattern.compile("Duration: (.*), start:");

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
     * @param inputFile the input file
     * @return <code>true</code> if supported or <code>false</code> if not
     * @throws IOException
     * @throws InterruptedException
     * @throws JAXBException
     * @throws ExecutionException 
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
     * @throws InterruptedException
     * @throws JAXBException
     * @throws ExecutionException
     */
    public static boolean isUpscaling(final Path inputFile, final String scale)
        throws InterruptedException, JAXBException, ExecutionException {
        final ProbeWrapper probe = probe(inputFile);
        final Integer[] sc = Arrays.stream(scale.split(":")).map(Integer::new).toArray(Integer[]::new);

        if (probe != null && probe.getStreams() != null) {
            return probe.getStreams().stream().filter(s -> s.getCodecType().equalsIgnoreCase("video")).map(s -> {
                try {
                    return (sc[0] < 0 || sc[0] < s.getWidth())
                        && (sc[1] < 0 || sc[1] < s.getHeight());
                } catch (Exception ex) {
                    return true;
                }
            }).filter(rt -> rt).count() == 0;
        }

        return true;
    }

    /**
     * Build the command line for given {@link SettingsWrapper}.
     *  
     * @param output the settings
     * @return the command
     * @throws IOException
     * @throws InterruptedException
     */
    public static String command(final Output output)
        throws InterruptedException {
        final StringBuffer cmd = new StringBuffer();

        cmd.append("ffmpeg -i {0} -stats -y");

        Video video = output.getVideo();

        cmd.append(" -codec:v " + video.getCodec());

        Optional.ofNullable(video.getAdvancedOptions()).ifPresent(ao -> {
            cmd.append(" " + ao);
        });

        Optional.ofNullable(video.getPreset()).ifPresent(v -> cmd.append(" -preset " + v));
        Optional.ofNullable(video.getTune()).ifPresent(v -> cmd.append(" -tune " + v));
        Optional.ofNullable(video.getProfile()).ifPresent(v -> cmd.append(" -profile:v " + v));
        Optional.ofNullable(video.getLevel()).ifPresent(v -> cmd.append(" -level " + v));
        Optional.ofNullable(video.getPixelFormat())
            .ifPresent(v -> cmd.append(" -pix_fmt " + (!v.isEmpty() ? v : "yuv420p")));

        Optional.ofNullable(video.getScale()).ifPresent(v -> cmd.append(" -vf 'scale=" + v + "'"));

        Optional.ofNullable(video.getFramerate()).ifPresent(v -> {
            cmd.append(" -r " + v);
            Optional.ofNullable(video.getFramerateType()).ifPresent(t -> cmd.append(" -vsync " + ("CFR".equals(t) ? "1"
                : "VFR".equals(t) ? "2" : "0")));
        });

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
        });

        Audio audio = output.getAudio();

        cmd.append(" -codec:a " + audio.getCodec());
        cmd.append(" -ac 2");

        Optional.ofNullable(audio.getBitrate()).ifPresent(v -> cmd.append(" -b:a " + v + "k"));
        Optional.ofNullable(audio.getSamplerate()).ifPresent(v -> cmd.append(" -ar " + v));

        cmd.append(" {1}");

        return cmd.toString();
    }

    /**
     * Builds filename for given format.
     * 
     * @param format the output format
     * @param fileName the input file name
     * @param appendix the filename appendix
     * @return
     * @throws ExecutionException 
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
     * @throws IOException
     * @throws InterruptedException
     * @throws JAXBException
     * @throws ExecutionException 
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

    private static Stream<String> splitString(final String str) {
        return Arrays.stream(str.split("\\s")).filter(s -> !s.isEmpty());
    }

    private static String getPatternGroup(final Pattern pattern, final String str, int group) {
        final Matcher m = pattern.matcher(str);
        if (m.find()) {
            return m.group(group).trim();
        }

        return null;
    }
}
