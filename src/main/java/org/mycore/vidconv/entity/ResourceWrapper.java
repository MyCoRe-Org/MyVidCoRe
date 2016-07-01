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
package org.mycore.vidconv.entity;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.security.NoSuchAlgorithmException;

import javax.activation.MimetypesFileTypeMap;

import org.mycore.vidconv.util.Hash;

/**
 * @author Ren\u00E9 Adler (eagle)
 *
 */
public class ResourceWrapper implements Serializable {

    private static final long serialVersionUID = 1L;

    private String fileName;

    private String mimeType;

    private String etag;

    private byte[] content;

    public ResourceWrapper(final String fileName, final InputStream is) {
        this(fileName, null, is);
    }

    public ResourceWrapper(final String fileName, final String mimeType, final InputStream is) {
        try {
            this.fileName = fileName;
            this.mimeType = mimeType != null ? mimeType : detectMimeType(fileName);
            this.content = toByteArray(is);
            this.etag = Hash.getMD5String(new String(this.content));
        } catch (IOException | NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * @return the fileName
     */
    public String getFileName() {
        return fileName;
    }

    /**
     * @return the mimeType
     */
    public String getMimeType() {
        return mimeType;
    }

    /**
     * @return the etag
     */
    public String getETag() {
        return this.etag;
    }

    /**
     * @return the content
     */
    public byte[] getContent() {
        return content;
    }

    static String detectMimeType(final String fileName) throws IOException {
        // Missing mime type should be add to META-INF/mime.types
        MimetypesFileTypeMap mimeTypesMap = new MimetypesFileTypeMap();
        return mimeTypesMap.getContentType(fileName);
    }

    static byte[] toByteArray(InputStream input) throws IOException {
        byte[] buffer = new byte[8192];
        int bytesRead;
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        while ((bytesRead = input.read(buffer)) != -1) {
            output.write(buffer, 0, bytesRead);
        }
        return output.toByteArray();
    }
}