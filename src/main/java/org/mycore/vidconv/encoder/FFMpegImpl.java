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
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.mycore.vidconv.entity.CodecWrapper;
import org.mycore.vidconv.entity.CodecsWrapper;
import org.mycore.vidconv.entity.FormatWrapper;
import org.mycore.vidconv.entity.FormatsWrapper;
import org.mycore.vidconv.entity.SettingsWrapper;
import org.mycore.vidconv.entity.SettingsWrapper.Audio;
import org.mycore.vidconv.entity.SettingsWrapper.Video;
import org.mycore.vidconv.util.StreamConsumer;

/**
 * @author Ren\u00E9 Adler (eagle)
 *
 */
public class FFMpegImpl {

    private static final Pattern PATTERN_LIB = Pattern.compile("--enable-([^\\s]+)");

    private static final Pattern PATTERN_CODECS = Pattern
            .compile("\\s(D|\\.|\\s)(E|\\.|\\s)(V|A|S|\\s)(I|\\.|\\s)(L|\\.|\\s)(S|\\.|\\s)\\s([^=\\s\\t]+)([^\\n]+)");

    private static final Pattern PATTERN_ENCODER_LIB = Pattern.compile("\\(encoders:\\s([^\\)]+)\\)");

    private static final Pattern PATTERN_DECODER_LIB = Pattern.compile("\\(decoders:\\s([^\\)]+)\\)");

    private static CodecsWrapper supportedCodecs;

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

        final List<String> libs = new ArrayList<>();
        final String errorStream = errorConsumer.getStreamOutput();
        if (errorStream != null) {
            final Matcher m = PATTERN_LIB.matcher(errorStream);
            while (m.find()) {
                libs.add(m.group(1));
            }
        }

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
                        codec.setEncoderLib(
                                Arrays.stream(em.group(1).trim().split("\\s")).collect(Collectors.toList()));
                    }
                    final Matcher dm = PATTERN_DECODER_LIB.matcher(desc);
                    while (dm.find()) {
                        codec.setDecoderLib(
                                Arrays.stream(dm.group(1).trim().split("\\s")).collect(Collectors.toList()));
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

    public static String command(final SettingsWrapper settings) throws IOException, InterruptedException {
        final StringBuffer cmd = new StringBuffer();

        cmd.append("ffmpeg -i {0} -stats -threads 1 -y");

        Video video = settings.getVideo();

        System.out.println(codecs().getByName(video.getCodec()).get(0));

        cmd.append(" -codec:v lib" + video.getCodec());
        cmd.append(" -profile:v " + video.getProfile());
        cmd.append(" -level " + video.getLevel());
        cmd.append(" -pix_fmt yuv420p");

        if (video.getFramerate() != null) {
            if (video.getFramerateType() != null) {
                cmd.append(" -vsync " + (video.getFramerateType().equals("CFR") ? "1"
                        : video.getFramerateType().equals("VFR") ? "2" : "0"));
            }
            cmd.append(" -r " + video.getFramerate());
        }

        if (video.getQuality().getType() != null && video.getQuality().getType().equals("CRF")) {
            cmd.append(" -crf " + video.getQuality().getRateFactor());
        } else if (video.getQuality().getType() != null && video.getQuality().getType().equals("ABR")) {
            cmd.append(" -b:v " + video.getQuality().getBitrate() + "k");
        }

        Audio audio = settings.getAudio();

        switch (audio.getCodec()) {
        case "aac":
            cmd.append(" -codec:a libfdk_aac");
            break;
        case "he-aac":
            cmd.append(" -codec:a libfdk_aac -profile:a aac_he");
            break;
        case "mp3":
            cmd.append(" -codec:a libmp3lame");
            break;
        case "vorbis":
            cmd.append(" -codec:a libvorbis");
            break;
        }

        cmd.append(" -b:a " + audio.getBitrate() + "k");

        if (audio.getSamplerate() != null)
            cmd.append(" -ar " + audio.getSamplerate());

        cmd.append(" {1}");

        System.out.println(cmd.toString());
        return cmd.toString();
    }

    private static final Pattern PATTERN_DURATION = Pattern.compile("Duration: (.*), start:");

    private static final Pattern PATTERN_CURRENT = Pattern.compile(".*time=([0-9:\\.]+) bitrate.*$");

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
        String[] tp = time.split("[:\\.]");

        millis += Integer.parseInt(tp[0]) * 3600000;
        millis += Integer.parseInt(tp[1]) * 60000;
        millis += Integer.parseInt(tp[2]) * 1000;
        millis += Integer.parseInt(tp[3]);

        return millis;
    }
}
