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
package org.mycore.vidconv.common.util;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * @author Ren\u00E9 Adler (eagle)
 *
 */
public class StreamConsumer implements Runnable {

    private static final Logger LOGGER = LogManager.getLogger();

    private final StringBuffer sb;

    private final InputStream is;

    public StreamConsumer(final InputStream stream) {
        sb = new StringBuffer();
        is = stream;
    }

    @Override
    public void run() {
        byte[] buf = new byte[1024];
        int read;
        try {
            while ((read = is.read(buf)) != -1) {
                sb.append(new String(buf, 0, read, StandardCharsets.UTF_8));
            }
        } catch (IOException e) {
            LOGGER.error(e.getMessage(), e);
        }
    }

    public synchronized String getStreamOutput() {
        return sb.toString();
    }

    public synchronized void clear() {
        sb.delete(0, sb.length());
    }

    public synchronized void clear(int length) {
        sb.delete(0, length);
    }

}
