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
package org.mycore.vidconv.frontend;

import static org.junit.Assert.assertNotNull;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;
import org.mycore.vidconv.common.config.Settings;
import org.openqa.selenium.By;

/**
 * @author Ren\u00E9 Adler (eagle)
 *
 */
public class TestSettings extends TestSharedHelpers {

    @SuppressWarnings("serial")
    private List<Map<String, String>> OUTPUT_SETTINGS = Arrays.asList(
            new HashMap<String, String>() {
                {
                    put("format", "mp4");
                    put("fileappendix", "-2160p");

                    put("video-codec", "libx264");
                    put("video-profile", "high");
                    put("video-level", "5.1");
                    put("video-preset", "fast");
                    put("video-tune", "film");
                    put("video-pixelformat", "yuv420p");
                    put("video-scale", "-2:2160");
                    put("video-framerate", "30");
                    put("video-forceKeyFrames", "2");
                    put("video-bitrate", "15000");
                    put("video-maxrate", "15000");
                    put("video-bufsize", "30000");

                    put("audio-codec", "aac");
                    put("audio-samplerate", "48000");
                    put("audio-bitrate", "192");
                }
            },
            new HashMap<String, String>() {
                {
                    put("format", "mp4");
                    put("fileappendix", "-1080p");

                    put("video-codec", "libx264");
                    put("video-profile", "main");
                    put("video-level", "4.1");
                    put("video-preset", "fast");
                    put("video-tune", "film");
                    put("video-pixelformat", "yuv420p");
                    put("video-scale", "-2:1080");
                    put("video-framerate", "25");
                    put("video-forceKeyFrames", "2");
                    put("video-bitrate", "8000");
                    put("video-maxrate", "16000");
                    put("video-bufsize", "16000");

                    put("audio-codec", "aac");
                    put("audio-samplerate", "48000");
                    put("audio-bitrate", "192");
                }
            },
            new HashMap<String, String>() {
                {
                    put("format", "mp4");
                    put("fileappendix", "-720p");

                    put("video-codec", "libx264");
                    put("video-profile", "main");
                    put("video-level", "4.0");
                    put("video-preset", "fast");
                    put("video-tune", "film");
                    put("video-pixelformat", "yuv420p");
                    put("video-scale", "-2:720");
                    put("video-framerate", "25");
                    put("video-forceKeyFrames", "2");
                    put("video-bitrate", "5000");
                    put("video-maxrate", "10000");
                    put("video-bufsize", "10000");

                    put("audio-codec", "aac");
                    put("audio-samplerate", "44100");
                    put("audio-bitrate", "128");
                }
            },
            new HashMap<String, String>() {
                {
                    put("format", "mp4");
                    put("fileappendix", "-480p");

                    put("video-codec", "libx264");
                    put("video-profile", "main");
                    put("video-level", "3.1");
                    put("video-preset", "fast");
                    put("video-tune", "film");
                    put("video-pixelformat", "yuv420p");
                    put("video-scale", "-2:480");
                    put("video-framerate", "25");
                    put("video-forceKeyFrames", "2");
                    put("video-bitrate", "2500");
                    put("video-maxrate", "5000");
                    put("video-bufsize", "5000");

                    put("audio-codec", "aac");
                    put("audio-samplerate", "44100");
                    put("audio-bitrate", "128");
                }
            },
            new HashMap<String, String>() {
                {
                    put("format", "mp4");
                    put("fileappendix", "-360p");

                    put("video-codec", "libx264");
                    put("video-profile", "main");
                    put("video-level", "3.0");
                    put("video-preset", "fast");
                    put("video-tune", "film");
                    put("video-pixelformat", "yuv420p");
                    put("video-scale", "-2:360");
                    put("video-framerate", "25");
                    put("video-forceKeyFrames", "2");
                    put("video-bitrate", "700");
                    put("video-maxrate", "1400");
                    put("video-bufsize", "1400");

                    put("audio-codec", "aac");
                    put("audio-samplerate", "44100");
                    put("audio-bitrate", "96");
                }
            });

    @Override
    public void setUp() throws Exception {
        super.setUp();
        Settings.instance().clearSettings();
    }

    @Test
    public void testSettings() {
        switchNavLink("settings");

        waitForInvisibleElement(By.id("spinner"), 3);
        assertNotNull(waitForElement(By.id("settings")));

        AtomicInteger tabIndex = new AtomicInteger(0);

        do {
            Map<String, String> os = OUTPUT_SETTINGS.get(tabIndex.get());

            assertNotNull(reloadRetry(() -> {
                return waitAndClick(By.id("tab-output-" + tabIndex.get()));
            }));

            waitAndSelectByValue(By.id("output-format-" + tabIndex), os.get("format"));
            waitForElement(By.id("output-fileappendix-" + tabIndex)).sendKeys(os.get("fileappendix"));

            fillVideoTab(tabIndex.get());
            fillAudioTab(tabIndex.get());

            tabIndex.incrementAndGet();

            if (tabIndex.get() < OUTPUT_SETTINGS.size()) {
                waitAndClick(By.id("tab-output-add"));
            }
        } while (tabIndex.get() < OUTPUT_SETTINGS.size());

        waitAndClick(By.cssSelector("*[type = 'submit']"));

        waitForInvisibleElement(By.id("spinner"), 3);
        assertNotNull(waitForElement(By.id("settings")));

        assertNotNull(waitForElement(By.cssSelector("#toast-container > div.toast-success")));
    }

    private void fillVideoTab(int tabIndex) {
        Map<String, String> os = OUTPUT_SETTINGS.get(tabIndex);

        // change to video
        waitAndClick(By.id("tab-output-" + tabIndex + "-video"));

        waitAndSelectByValue(By.id("output-" + tabIndex + "-video-codec"), os.get("video-codec"));
        waitAndSelectByValue(By.id("output-" + tabIndex + "-video-profile"), os.get("video-profile"));
        waitAndSelectByValue(By.id("output-" + tabIndex + "-video-level"), os.get("video-level"));
        waitAndSelectByValue(By.id("output-" + tabIndex + "-video-preset"), os.get("video-preset"));
        waitAndSelectByValue(By.id("output-" + tabIndex + "-video-tune"), os.get("video-tune"));
        waitAndSelectByValue(By.id("output-" + tabIndex + "-video-pixelformat"), os.get("video-pixelformat"));
        waitAndSelectByValue(By.id("output-" + tabIndex + "-video-scale"), os.get("video-scale"));
        waitAndSelectByValue(By.id("output-" + tabIndex + "-video-framerate"), os.get("video-framerate"));

        waitAndClick(By.cssSelector("label[for='output-" + tabIndex + "-video-framerateType-vfr']"));
        waitForElement(By.id("output-" + tabIndex + "-video-forceKeyFrames")).sendKeys(os.get("video-forceKeyFrames"));

        waitAndClick(By.cssSelector("label[for='output-" + tabIndex + "-video-qType-abr']"));
        waitForElement(By.id("output-" + tabIndex + "-video-bitrate")).sendKeys(os.get("video-bitrate"));
        waitForElement(By.id("output-" + tabIndex + "-video-maxrate")).sendKeys(os.get("video-maxrate"));
        waitForElement(By.id("output-" + tabIndex + "-video-bufsize")).sendKeys(os.get("video-bufsize"));
    }

    private void fillAudioTab(int tabIndex) {
        Map<String, String> os = OUTPUT_SETTINGS.get(tabIndex);

        // change to audio tab
        waitAndClick(By.id("tab-output-" + tabIndex + "-audio"));

        waitAndSelectByValue(By.id("output-" + tabIndex + "-audio-codec"), os.get("audio-codec"));
        waitAndSelectByValue(By.id("output-" + tabIndex + "-audio-samplerate"), os.get("audio-samplerate"));
        waitAndSelectByValue(By.id("output-" + tabIndex + "-audio-bitrate"), os.get("audio-bitrate"));
    }

}
