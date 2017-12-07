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
package org.mycore.vidconv.common.util;

import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.Consumer;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * Matcher stream util class.
 * 
 * @author Ren\u00E9 Adler (eagle)
 *
 */
public class MatcherStream {

    /**
     * Find with {@ink Pattern} within input and return result as {@link String} stream.
     *
     * @param pattern the pattern
     * @param input the input
     * @return the stream
     */
    public static Stream<String> find(Pattern pattern, CharSequence input) {
        return findMatches(pattern, input).map(MatchResult::group);
    }

    /**
     * Find with {@ink Pattern} within input and return as {@ink MatchResult} stream.
     *
     * @param pattern the pattern
     * @param input the input
     * @return the stream
     */
    public static Stream<MatchResult> findMatches(Pattern pattern, CharSequence input) {
        Matcher matcher = pattern.matcher(input);

        Spliterator<MatchResult> spliterator = new Spliterators.AbstractSpliterator<MatchResult>(
            Long.MAX_VALUE, Spliterator.ORDERED | Spliterator.NONNULL) {
            @Override
            public boolean tryAdvance(Consumer<? super MatchResult> action) {
                if (!matcher.find())
                    return false;
                action.accept(matcher.toMatchResult());
                return true;
            }
        };

        return StreamSupport.stream(spliterator, false);
    }
}
