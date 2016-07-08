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
package org.mycore.vidconv.encoder;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.mycore.vidconv.entity.CodecWrapper;
import org.mycore.vidconv.entity.CodecsWrapper;
import org.mycore.vidconv.entity.EncoderWrapper;
import org.mycore.vidconv.entity.EncodersWrapper;
import org.mycore.vidconv.entity.FormatWrapper;
import org.mycore.vidconv.entity.FormatsWrapper;
import org.mycore.vidconv.entity.MuxerWrapper;
import org.mycore.vidconv.entity.ParameterWrapper;
import org.mycore.vidconv.entity.ParameterWrapper.ParameterValue;
import org.mycore.vidconv.entity.SettingsWrapper;
import org.mycore.vidconv.entity.SettingsWrapper.Audio;
import org.mycore.vidconv.entity.SettingsWrapper.Video;
import org.mycore.vidconv.util.StreamConsumer;

/**
 * @author Ren\u00E9 Adler (eagle)
 *
 */
public class FFMpegImpl {

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
     */
    public static CodecsWrapper codecs() throws IOException, InterruptedException {
        if (supportedCodecs != null) {
            return supportedCodecs;
        }
        final Process p = Runtime.getRuntime().exec(new String[] { "ffmpeg", "-codecs" });

        StreamConsumer outputConsumer = new StreamConsumer(p.getInputStream());
        StreamConsumer errorConsumer = new StreamConsumer(p.getErrorStream());

        new Thread(outputConsumer).start();
        new Thread(errorConsumer).start();

        p.waitFor();

        final List<CodecWrapper> codecs = new ArrayList<>();
        final String outputStream = outputConsumer.getStreamOutput();
        if (outputStream != null) {
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
        }

        supportedCodecs = new CodecsWrapper().setCodecs(codecs);
        return supportedCodecs;
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
     */
    public static FormatsWrapper formats() throws IOException, InterruptedException {
        if (supportedFormats != null) {
            return supportedFormats;
        }

        final Process p = Runtime.getRuntime().exec(new String[] { "ffmpeg", "-formats" });

        StreamConsumer outputConsumer = new StreamConsumer(p.getInputStream());
        StreamConsumer errorConsumer = new StreamConsumer(p.getErrorStream());

        new Thread(outputConsumer).start();
        new Thread(errorConsumer).start();

        p.waitFor();

        final List<FormatWrapper> formats = new ArrayList<>();
        final String outputStream = outputConsumer.getStreamOutput();
        if (outputStream != null) {
            final Matcher m = PATTERN_FORMATS.matcher(outputStream);
            while (m.find()) {
                final FormatWrapper format = new FormatWrapper();

                format.setDemuxer(m.group(1).equalsIgnoreCase("D"));
                format.setMuxer(m.group(2).equalsIgnoreCase("E"));
                format.setName(m.group(3).trim());
                format.setDescription(m.group(4).trim());

                formats.add(format);
            }
        }

        supportedFormats = new FormatsWrapper().setFormats(formats);
        return supportedFormats;
    }

    private static final Pattern PATTERN_ENCODER = Pattern
            .compile("^Encoder\\s([^\\s]+)\\s\\[(?:[^\\]]+)\\]:\\n([\\S\\s]+)$");

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

    private static Map<String, EncodersWrapper> supportedEncoders = Collections.synchronizedMap(new HashMap<>());

    /**
     * Returns informations for given encoder.
     * 
     * @param name the encoder name
     * @throws IOException
     * @throws InterruptedException
     */
    public static EncodersWrapper encoder(final String name) throws IOException, InterruptedException {
        if (supportedEncoders.containsKey(name)) {
            return supportedEncoders.get(name);
        }

        final Process p = Runtime.getRuntime().exec(new String[] { "ffmpeg", "-h", "encoder=" + name });

        StreamConsumer outputConsumer = new StreamConsumer(p.getInputStream());
        StreamConsumer errorConsumer = new StreamConsumer(p.getErrorStream());

        new Thread(outputConsumer).start();
        new Thread(errorConsumer).start();

        p.waitFor();

        final String outputStream = outputConsumer.getStreamOutput();
        if (outputStream != null) {
            final List<EncoderWrapper> encoders = PATTERN_ENTRY_SPLIT.splitAsStream(outputStream)
                    .filter(os -> !os.isEmpty())
                    .map(os -> {
                        final Matcher m = PATTERN_ENCODER.matcher(os);
                        while (m.find()) {
                            final EncoderWrapper encoder = new EncoderWrapper();

                            encoder.setName(m.group(1));

                            encoder.setPixelFormats(
                                    Stream.of(m.group(2)).map(s -> PATTERN_PIX_FMT.matcher(s)).filter(ma -> ma.find())
                                            .flatMap(ma -> splitString(ma.group(1)))
                                            .collect(Collectors.toList()));

                            encoder.setFrameRates(
                                    Stream.of(m.group(2)).map(s -> PATTERN_FRM_RATES.matcher(s))
                                            .filter(ma -> ma.find())
                                            .flatMap(ma -> splitString(ma.group(1)))
                                            .collect(Collectors.toList()));

                            encoder.setSampleFormats(
                                    Stream.of(m.group(2)).map(s -> PATTERN_SMP_FROMATS.matcher(s))
                                            .filter(ma -> ma.find())
                                            .flatMap(ma -> splitString(ma.group(1)))
                                            .collect(Collectors.toList()));

                            encoder.setSampleRates(
                                    Stream.of(m.group(2)).map(s -> PATTERN_SMP_RATES.matcher(s)).filter(ma -> ma.find())
                                            .flatMap(ma -> splitString(ma.group(1))).map(s -> new Integer(s))
                                            .collect(Collectors.toList()));

                            encoder.setChannelLayouts(
                                    Stream.of(m.group(2)).map(s -> PATTERN_CH_LAYOUTS.matcher(s))
                                            .filter(ma -> ma.find())
                                            .flatMap(ma -> splitString(ma.group(1)))
                                            .collect(Collectors.toList()));

                            final List<ParameterWrapper> parameters = new ArrayList<>();
                            final Matcher pm = PATTERN_PARAMS.matcher(m.group(2));
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

        return null;
    }

    private static final Pattern PATTERN_MUXER = Pattern
            .compile("^Muxer\\s([^\\s]+)\\s\\[(?:[^\\]]+)\\]:\\n([\\S\\s]+)$");

    private static final Pattern PATTERN_EXTENSION = Pattern.compile("Common extensions:(.*)\\.");

    private static final Pattern PATTERN_MIME_TYPE = Pattern.compile("Mime type:(.*)\\.");

    private static final Pattern PATTERN_AUDIO_CODEC = Pattern.compile("audio codec:(.*)\\.");

    private static final Pattern PATTERN_VIDEO_CODEC = Pattern.compile("video codec:(.*)\\.");

    private static final Pattern PATTERN_SUBTITLE_CODEC = Pattern.compile("video codec:(.*)\\.");

    private static Map<String, MuxerWrapper> supportedMuxers = Collections.synchronizedMap(new HashMap<>());

    public static MuxerWrapper muxer(final String name) throws IOException, InterruptedException {
        if (supportedMuxers.containsKey(name)) {
            return supportedMuxers.get(name);
        }

        final Process p = Runtime.getRuntime().exec(new String[] { "ffmpeg", "-h", "muxer=" + name });

        StreamConsumer outputConsumer = new StreamConsumer(p.getInputStream());
        StreamConsumer errorConsumer = new StreamConsumer(p.getErrorStream());

        new Thread(outputConsumer).start();
        new Thread(errorConsumer).start();

        p.waitFor();

        final String outputStream = outputConsumer.getStreamOutput();
        if (outputStream != null) {
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
                return supportedMuxers.get(name);
            }
        }

        return null;
    }

    private static final Pattern PATTERN_DURATION = Pattern.compile("Duration: (.*), start:");

    private static final Pattern PATTERN_CURRENT = Pattern.compile(".*time=([0-9:\\.]+) bitrate.*$");

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
                long current = parseMillis(cm.group(1));

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
     * Build the command line for given {@link SettingsWrapper}.
     *  
     * @param settings the settings
     * @return the command
     * @throws IOException
     * @throws InterruptedException
     */
    public static String command(final SettingsWrapper settings) throws IOException, InterruptedException {
        final StringBuffer cmd = new StringBuffer();

        cmd.append("ffmpeg -i {0} -stats -threads 1 -y");

        Video video = settings.getVideo();

        cmd.append(" -codec:v " + video.getCodec());
        if (video.getProfile() != null)
            cmd.append(" -profile:v " + video.getProfile());
        if (video.getLevel() != null)
            cmd.append(" -level " + video.getLevel());
        cmd.append(" -pix_fmt " + (video.getPixelFormat() != null && !video.getPixelFormat().isEmpty()
                ? video.getPixelFormat() : "yuv420p"));

        if (video.getFramerate() != null) {
            if (video.getFramerateType() != null) {
                cmd.append(" -vsync " + (video.getFramerateType().equals("CFR") ? "1"
                        : video.getFramerateType().equals("VFR") ? "2" : "0"));
            }
            cmd.append(" -r " + video.getFramerate());
        }

        Video.Quality quality = video.getQuality();
        if (quality != null && quality.getType() != null) {
            if (quality.getType().equals("CRF") && quality.getRateFactor() != null) {
                cmd.append(" -crf " + quality.getRateFactor());
            } else if (quality.getType().equals("CQ") && quality.getScale() != null) {
                cmd.append(" -qscale:v " + quality.getScale());
            } else if (quality.getType().equals("ABR") && quality.getBitrate() != null) {
                cmd.append(" -b:v " + quality.getBitrate() + "k");
            }
        }

        Audio audio = settings.getAudio();

        cmd.append(" -codec:a " + audio.getCodec());

        if (audio.getBitrate() != null)
            cmd.append(" -b:a " + audio.getBitrate() + "k");
        if (audio.getSamplerate() != null)
            cmd.append(" -ar " + audio.getSamplerate());

        cmd.append(" {1}");

        return cmd.toString();
    }

    public static String filename(final SettingsWrapper settings, final String fileName) {
        try {
            final String extension = muxer(settings.getFormat()).getExtension();
            return fileName.substring(0, fileName.lastIndexOf('.')) + "." + extension;
        } catch (IOException | InterruptedException e) {
            return null;
        }
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
