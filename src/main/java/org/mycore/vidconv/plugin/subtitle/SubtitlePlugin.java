package org.mycore.vidconv.plugin.subtitle;
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

import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.vidconv.backend.service.ConverterService;
import org.mycore.vidconv.backend.service.ConverterService.ConverterJob;
import org.mycore.vidconv.common.event.Event;
import org.mycore.vidconv.common.util.Executable;
import org.mycore.vidconv.plugin.ListenerPlugin;
import org.mycore.vidconv.plugin.annotation.Plugin;
import org.mycore.vidconv.plugin.subtitle.VoskExtractor.VoskResult;
import org.mycore.vidconv.plugin.subtitle.VoskExtractor.VoskResult.Word;

import fr.noop.subtitle.util.SubtitlePlainText;
import fr.noop.subtitle.util.SubtitleTimeCode;
import fr.noop.subtitle.vtt.VttCue;
import fr.noop.subtitle.vtt.VttLine;
import fr.noop.subtitle.vtt.VttObject;
import fr.noop.subtitle.vtt.VttWriter;

/**
 * @author Ren\u00E9 Adler (eagle)
 *
 */
@Plugin(name = "Subtitle Plugin", description = "Generates WebVTT Subtitle from Video.")
public class SubtitlePlugin extends ListenerPlugin {

    private static final Logger LOGGER = LogManager.getLogger();

    private static final long PROCESS_TIMEOUT_VALUE = 1;

    private static final TimeUnit PROCESS_TIMEOUT_UNIT = TimeUnit.HOURS;

    private static final float SAMPLE_RATE = 16000;

    private static final int SUBTITLE_LINE_WORDS = 7;

    private static <T> Stream<List<T>> batches(List<T> source, int length) {
        if (length <= 0) {
            throw new IllegalArgumentException("length = " + length);
        }

        int size = source.size();
        if (size <= 0) {
            return Stream.empty();
        }

        int fullChunks = (size - 1) / length;
        return IntStream.range(0, fullChunks + 1).mapToObj(
                n -> source.subList(n * length, n == fullChunks ? size : (n + 1) * length));
    }

    private static String formatFileName(Path outputPath, Path inputFile, String ext) {
        String fName = inputFile.getFileName().toString();
        return outputPath
                .resolve(fName.substring(0, fName.lastIndexOf(".")) + ext)
                .toAbsolutePath()
                .toString();
    }

    private static VttObject buildSubtitle(List<VoskResult> results) {
        VttObject subtitle = new VttObject();

        results.stream()
                .filter(r -> r != null && r.getText() != null && !r.getText().isEmpty() && r.getResult() != null)
                .forEach(r -> {
                    batches(r.getResult(), SUBTITLE_LINE_WORDS).forEach(ws -> {
                        VttLine line = new VttLine();
                        line.addText(
                                new SubtitlePlainText(ws.stream().map(Word::getWord).collect(Collectors.joining(" "))));

                        VttCue cue = new VttCue();

                        cue.addLine(line);
                        cue.setStartTime(new SubtitleTimeCode((long) (ws.get(0).getStart() * 1000)));
                        cue.setEndTime(
                                new SubtitleTimeCode((long) (ws.get(ws.size() - 1).getEnd() * 1000)));

                        subtitle.addCue(cue);
                    });
                });

        return subtitle;
    }

    private static String buildAudioExtractCommand(String inputFileName, String outputFileName, float sampleRate) {
        return "ffmpeg -y -i \"" + inputFileName + "\" -q:a 0 -ac 1 -ar "
                + Float.toString(sampleRate)
                + " -acodec pcm_s16le \"" + outputFileName + "\"";
    }

    private static int runCommand(String cmd) throws InterruptedException, ExecutionException {
        LOGGER.debug(cmd);

        int res = -1;
        Executable exec = new Executable(cmd);
        res = exec.runAndWait(PROCESS_TIMEOUT_VALUE, PROCESS_TIMEOUT_UNIT);

        return res;
    }

    /* (non-Javadoc)
     * @see org.mycore.vidconv.common.event.Listener#handleEvent(org.mycore.vidconv.common.event.Event)
     */
    @Override
    public void handleEvent(Event<?> event) throws Exception {
        if (ConverterService.EVENT_CONVERT_DONE.equals(event.getType())
                && event.getSource().equals(ConverterService.class)) {
            final ConverterJob job = (ConverterJob) event.getObject();

            if (job.exitValue() == 0) {
                VoskExtractor extractor = new VoskExtractor(SAMPLE_RATE);

                if (extractor.getModelPath() == null) {
                    LOGGER.warn("no model path provided, disable plugin");
                    disable();
                    return;
                }

                Path audioFilePath = Paths
                        .get(formatFileName(job.inputPath().getParent(), job.inputPath(), ".wav"));

                LOGGER.info("extract audio from {}...", job.inputPath());
                String cmd = buildAudioExtractCommand(job.inputPath().toString(), audioFilePath.toString(),
                        SAMPLE_RATE);
                int ret = runCommand(cmd);

                if (ret == 0) {
                    try {
                        LOGGER.info("guess language of {}...", audioFilePath);
                        String lang = extractor.guessLanguage(audioFilePath);

                        if (lang != null) {
                            LOGGER.info("...set language to \"{}\" for {}", lang, audioFilePath);
                            extractor.setModelLang(lang);
                        }

                        LOGGER.info("extract text from {}...", audioFilePath);
                        List<VoskResult> results = extractor.extract(audioFilePath);

                        if (results != null && !results.isEmpty()) {
                            Path vttFilePath = Paths
                                    .get(formatFileName(job.outputPath(), job.inputPath(), ".vtt"));
                            VttObject vtt = buildSubtitle(results);

                            LOGGER.info("write WebVTT to {}...", vttFilePath);
                            try (OutputStream os = Files.newOutputStream(vttFilePath)) {
                                VttWriter vttWriter = new VttWriter(StandardCharsets.UTF_8.name());
                                vttWriter.write(vtt, os);
                            }
                            LOGGER.info("...done.");
                        } else {
                            LOGGER.warn("...no text extracted.");
                        }
                    } finally {
                        Files.deleteIfExists(audioFilePath);
                    }
                } else if (ret == Executable.PROCESS_TIMEOUT) {
                    LOGGER.warn("...timeout reached!");
                }
            }
        }
    }

}
