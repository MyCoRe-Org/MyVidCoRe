/*
 * This file is part of ***  M y C o R e  ***
 * See http://www.mycore.de/ for details.
 *
 * This program is free software; you can use it, redistribute it
 * and / or modify it under the terms of the GNU General Public License
 * (GPL) as published by the Free Software Foundation; either version 2
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
package org.mycore.vidconv.frontend.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.Optional;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.StreamingOutput;

/**
 * @author Ren\u00E9 Adler (eagle)
 *
 */
public class ZipStreamingOutput implements StreamingOutput {

    private Path path;

    private Optional<Integer> level;

    public ZipStreamingOutput(Path path) {
        this.path = path;
    }

    public ZipStreamingOutput(Path path, int level) {
        this.path = path;
        this.level = Optional.of(level);
    }

    public Path path() {
        return path;
    }

    /* (non-Javadoc)
     * @see javax.ws.rs.core.StreamingOutput#write(java.io.OutputStream)
     */
    @Override
    public void write(OutputStream output) throws IOException, WebApplicationException {
        ZipOutputStream zipStream = new ZipOutputStream(output);
        try {
            level.ifPresent(zipStream::setLevel);

            if (!Files.isDirectory(path)) {
                addToZipStream(path, zipStream);
            } else {
                try (DirectoryStream<Path> dirStream = Files.newDirectoryStream(path)) {
                    dirStream.forEach(p -> {
                        try {
                            addToZipStream(p, zipStream);
                        } catch (IOException e) {
                            throw new UncheckedIOException(e);
                        }
                    });
                }
            }
        } finally {
            zipStream.close();
        }
    }

    private void addToZipStream(Path file, ZipOutputStream zipStream) throws FileNotFoundException, IOException {
        File inputFile = file.toFile();

        try (FileInputStream inputStream = new FileInputStream(inputFile)) {
            ZipEntry entry = new ZipEntry(inputFile.getName());
            entry.setCreationTime(FileTime.fromMillis(inputFile.lastModified()));
            zipStream.putNextEntry(entry);

            byte[] readBuffer = new byte[4096];
            int amountRead;

            while ((amountRead = inputStream.read(readBuffer)) > 0) {
                zipStream.write(readBuffer, 0, amountRead);
            }

            zipStream.closeEntry();
        }
    }

}
