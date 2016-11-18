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
package org.mycore.vidconv.frontend.util;

import java.io.IOException;
import java.io.OutputStream;
import java.io.RandomAccessFile;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.StreamingOutput;

/**
 * @author Ren\u00E9 Adler (eagle)
 *
 */
public class RangeStreamingOutput implements StreamingOutput {

    private int length;

    private RandomAccessFile raf;

    private final byte[] buf = new byte[4096];

    public RangeStreamingOutput(int length, RandomAccessFile raf) {
        this.length = length;
        this.raf = raf;
    }

    @Override
    public void write(OutputStream outputStream) throws WebApplicationException {
        try {
            try {
                while (length != 0) {
                    int read = raf.read(buf, 0, buf.length > length ? length : buf.length);
                    outputStream.write(buf, 0, read);
                    length -= read;
                }
            } finally {
                raf.close();
            }
        } catch (IOException e) {
        }
    }

    public int getLenth() {
        return length;
    }
}
