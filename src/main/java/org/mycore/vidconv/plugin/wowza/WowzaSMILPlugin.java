/*
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
package org.mycore.vidconv.plugin.wowza;

import java.nio.file.Path;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.vidconv.backend.encoder.FFMpegImpl;
import org.mycore.vidconv.backend.service.ConverterService;
import org.mycore.vidconv.backend.service.ConverterService.ConverterJob;
import org.mycore.vidconv.common.event.Event;
import org.mycore.vidconv.plugin.ListenerPlugin;
import org.mycore.vidconv.plugin.annotation.Plugin;

/**
 * @author Ren\u00E9 Adler (eagle)
 *
 */
@Plugin(name = "Wowza SMIL Plugin", description = "Generates SMIL-Files for Wowza.")
public class WowzaSMILPlugin extends ListenerPlugin {

    private static final Logger LOGGER = LogManager.getLogger();

    /* (non-Javadoc)
     * @see org.mycore.vidconv.common.event.Listener#handleEvent(org.mycore.vidconv.common.event.Event)
     */
    @Override
    public void handleEvent(Event<?> event) throws Exception {
        if (ConverterService.EVENT_CONVERT_DONE.equals(event.getType())
            && event.getSource().equals(ConverterService.class)) {

            final ConverterJob job = (ConverterJob) event.getObject();
            final String fileName = job.inputPath().getFileName().toString();
            final Path file = job.outputPath()
                .resolve(fileName.substring(0, fileName.lastIndexOf(".")) + ".smil");

            LOGGER.info("save to " + file);
            WowzaSMILWrapper.saveTo(file, job.outputs().stream().map(o -> {
                try {
                    return FFMpegImpl.probe(o.getOutputPath());
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }).collect(Collectors.toList()));
        }
    }

}
