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

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.Normalizer;
import java.text.Normalizer.Form;
import java.util.Locale;

import jakarta.xml.bind.DatatypeConverter;

/**
 * @author Ren\u00E9 Adler (eagle)
 *
 */
public class Hash {

	public static String getMD5String(String text) throws NoSuchAlgorithmException, UnsupportedEncodingException {
		return getHash(1, null, text, "md5");
	}

	private static String getHash(int iterations, byte[] salt, final String text, final String algorithm)
			throws NoSuchAlgorithmException, UnsupportedEncodingException {
		int it = iterations;
		MessageDigest digest;
		if (--it < 0) {
			it = 0;
		}
		byte[] data;

		digest = MessageDigest.getInstance(algorithm);
		String t = Normalizer.normalize(text, Form.NFC);
		if (salt != null) {
			digest.update(salt);
		}
		data = digest.digest(t.getBytes("UTF-8"));
		for (int i = 0; i < it; i++) {
			data = digest.digest(data);
		}

		return Hash.toHexString(data);
	}

	public static String toHexString(byte[] data) {
		return DatatypeConverter.printHexBinary(data).toLowerCase(Locale.ROOT);
	}

}
