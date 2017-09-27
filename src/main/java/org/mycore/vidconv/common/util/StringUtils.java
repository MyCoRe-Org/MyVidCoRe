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

import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.regex.Pattern;
import java.util.stream.IntStream;

/**
 * @author Ren\u00E9 Adler (eagle)
 *
 */
public class StringUtils {

    private final static char[] hexArray = "0123456789ABCDEF".toCharArray();

    public static boolean containsIgnoreCase(String haystack, String needle) {
        return haystack.toLowerCase(Locale.getDefault()).contains(needle.toLowerCase(Locale.getDefault()));
    }

    public static boolean filter(String haystack, String needle) {
        if (haystack == null || needle == null)
            return false;

        if (needle.contains("*")) {
            return Pattern.compile("^" + needle.replaceAll("\\*", ".*") + "$", Pattern.CASE_INSENSITIVE)
                .matcher(haystack).find();
        }

        return haystack.equalsIgnoreCase(needle);
    }

    public static String hexDump(byte[] bytes, int lineLen) {
        StringBuffer sb = new StringBuffer();

        IntStream.range(0, bytes.length / lineLen).forEach(l -> {
            int sOff = l * lineLen;
            int eOff = Integer.min((l + 1) * lineLen, bytes.length);
            sb.append(String.format(Locale.ROOT, "\n%08X  ", sOff));
            IntStream.range(sOff, eOff).forEach(j -> {
                int v = bytes[j] & 0xFF;
                sb.append(hexArray[v >>> 4]);
                sb.append(hexArray[v & 0x0F]);
                sb.append(" ");
            });
            sb.append(" ");
            sb.append(new String(bytes, sOff, lineLen, StandardCharsets.UTF_8).replaceAll("\n|\r|\t|\0", "."));
        });
        
        return sb.toString();
    }
}
