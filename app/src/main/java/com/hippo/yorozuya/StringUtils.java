/*
 * Copyright 2015 Hippo Seven
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.hippo.yorozuya;

import android.text.TextUtils;

import org.jsoup.parser.Parser;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class StringUtils {
    public static final String[] EMPTY_STRING_ARRAY = new String[0];
    public static final char[] WHITE_SPACE_ARRAY = {
            '\u0009', // TAB
            '\u0020', // SPACE
            '\u00A0', // NO-BREAK SPACE
            '\u3000', // IDEOGRAPHIC SPACE
    };

    private StringUtils() {
    }

    /**
     * Unescape xml. It do not work perfectly.
     */
    public static String unescapeXml(String str) {
        return Parser.unescapeEntities(str, true);
    }

    /**
     * <p>Replaces all occurrences of a String within another String.</p>
     *
     * <p>A {@code null} reference passed to this method is a no-op.</p>
     *
     * <pre>
     * StringUtils.replace(null, *, *)        = null
     * StringUtils.replace("", *, *)          = ""
     * StringUtils.replace("any", null, *)    = "any"
     * StringUtils.replace("any", *, null)    = "any"
     * StringUtils.replace("any", "", *)      = "any"
     * StringUtils.replace("aba", "a", null)  = "aba"
     * StringUtils.replace("aba", "a", "")    = "b"
     * StringUtils.replace("aba", "a", "z")   = "zbz"
     * </pre>
     *
     * @param text         text to search and replace in, may be null
     * @param searchString the String to search for, may be null
     * @param replacement  the String to replace it with, may be null
     * @return the text with any replacements processed,
     * {@code null} if null String input
     * @see #replace(String text, String searchString, String replacement, int max)
     */
    public static String replace(final String text, final String searchString, final String replacement) {
        return replace(text, searchString, replacement, -1);
    }

    /**
     * <p>Replaces a String with another String inside a larger String,
     * for the first {@code max} values of the search String.</p>
     *
     * <p>A {@code null} reference passed to this method is a no-op.</p>
     *
     * <pre>
     * StringUtils.replace(null, *, *, *)         = null
     * StringUtils.replace("", *, *, *)           = ""
     * StringUtils.replace("any", null, *, *)     = "any"
     * StringUtils.replace("any", *, null, *)     = "any"
     * StringUtils.replace("any", "", *, *)       = "any"
     * StringUtils.replace("any", *, *, 0)        = "any"
     * StringUtils.replace("abaa", "a", null, -1) = "abaa"
     * StringUtils.replace("abaa", "a", "", -1)   = "b"
     * StringUtils.replace("abaa", "a", "z", 0)   = "abaa"
     * StringUtils.replace("abaa", "a", "z", 1)   = "zbaa"
     * StringUtils.replace("abaa", "a", "z", 2)   = "zbza"
     * StringUtils.replace("abaa", "a", "z", -1)  = "zbzz"
     * </pre>
     *
     * @param text         text to search and replace in, may be null
     * @param searchString the String to search for, may be null
     * @param replacement  the String to replace it with, may be null
     * @param max          maximum number of values to replace, or {@code -1} if no maximum
     * @return the text with any replacements processed,
     * {@code null} if null String input
     */
    public static String replace(final String text, final String searchString, final String replacement, int max) {
        if (TextUtils.isEmpty(text) || TextUtils.isEmpty(searchString) || replacement == null || max == 0) {
            return text;
        }
        int start = 0;
        int end = text.indexOf(searchString, start);
        if (end < 0) {
            return text;
        }
        final int replLength = searchString.length();
        int increase = replacement.length() - replLength;
        increase = increase < 0 ? 0 : increase;
        increase *= max < 0 ? 16 : max > 64 ? 64 : max;
        final StringBuilder buf = new StringBuilder(text.length() + increase);
        while (end >= 0) {
            buf.append(text.substring(start, end)).append(replacement);
            start = end + replLength;
            if (--max == 0) {
                break;
            }
            end = text.indexOf(searchString, start);
        }
        buf.append(text.substring(start));
        return buf.toString();
    }

    public static boolean endsWith(String string, String[] suffixs) {
        for (int i = 0, n = suffixs.length; i < n; i++) {
            if (string.endsWith(suffixs[i])) {
                return true;
            }
        }
        return false;
    }

    /**
     * <p>Splits the provided text into an array, separator specified.
     * This is an alternative to using StringTokenizer.</p>
     *
     * <p>The separator is not included in the returned String array.
     * Adjacent separators are treated as one separator.
     * For more control over the split use the StrTokenizer class.</p>
     *
     * <p>A {@code null} input String returns {@code null}.</p>
     *
     * <pre>
     * StringUtils.split(null, *)         = null
     * StringUtils.split("", *)           = []
     * StringUtils.split("a.b.c", '.')    = ["a", "b", "c"]
     * StringUtils.split("a..b.c", '.')   = ["a", "b", "c"]
     * StringUtils.split("a:b:c", '.')    = ["a:b:c"]
     * StringUtils.split("a b c", ' ')    = ["a", "b", "c"]
     * </pre>
     *
     * @param str           the String to parse, may be null
     * @param separatorChar the character used as the delimiter
     * @return an array of parsed Strings, {@code null} if null String input
     * @since 2.0
     */
    // Get from org.apache.commons.lang3.StringUtils
    public static String[] split(final String str, final char separatorChar) {
        return splitWorker(str, separatorChar, false);
    }

    /**
     * Performs the logic for the {@code split} and
     * {@code splitPreserveAllTokens} methods that do not return a
     * maximum array length.
     *
     * @param str               the String to parse, may be {@code null}
     * @param separatorChar     the separate character
     * @param preserveAllTokens if {@code true}, adjacent separators are
     *                          treated as empty token separators; if {@code false}, adjacent
     *                          separators are treated as one separator.
     * @return an array of parsed Strings, {@code null} if null String input
     */
    // Get from org.apache.commons.lang3.StringUtils
    private static String[] splitWorker(final String str, final char separatorChar, final boolean preserveAllTokens) {
        // Performance tuned for 2.0 (JDK1.4)

        if (str == null) {
            return null;
        }
        final int len = str.length();
        if (len == 0) {
            return EMPTY_STRING_ARRAY;
        }
        final List<String> list = new ArrayList<>();
        int i = 0, start = 0;
        boolean match = false;
        boolean lastMatch = false;
        while (i < len) {
            if (str.charAt(i) == separatorChar) {
                if (match || preserveAllTokens) {
                    list.add(str.substring(start, i));
                    match = false;
                    lastMatch = true;
                }
                start = ++i;
                continue;
            }
            lastMatch = false;
            match = true;
            i++;
        }
        if (match || preserveAllTokens && lastMatch) {
            list.add(str.substring(start, i));
        }
        return list.toArray(new String[list.size()]);
    }

    public static String avoidNull(String value) {
        return avoidNull(value, "");
    }

    public static String avoidNull(String value, String defaultValue) {
        return value == null ? defaultValue : value;
    }

    public static boolean isAllDigit(String str) {
        if (TextUtils.isEmpty(str)) {
            return false;
        } else {
            for (int i = 0, n = str.length(); i < n; i++) {
                char ch = str.charAt(i);
                if (ch < '0' || ch > '9') {
                    return false;
                }
            }
            return true;
        }
    }

    public static int length(String str) {
        return null == str ? 0 : str.length();
    }

    /**
     * All null or empty, or all not
     */
    public static boolean equals(String str1, String str2) {
        return (TextUtils.isEmpty(str1) && TextUtils.isEmpty(str2)) ||
                (!TextUtils.isEmpty(str1) && !TextUtils.isEmpty(str2) && str1.equals(str2));
    }

    public static int ordinalIndexOf(String str, char c, int n) {
        if (null == str || n < 0) {
            return -1;
        }

        int pos = -1;
        do {
            pos = str.indexOf(c, pos + 1);
        } while (n-- > 0 && pos != -1);

        return pos;
    }

    /**
     * Works like {@link String#trim()}, but more white space is excluded.
     * The white space characters is {@link #WHITE_SPACE_ARRAY}.
     *
     * @see {@link #trim(String, char[])}
     */
    public static String trim(String str) {
        return trim(str, WHITE_SPACE_ARRAY);
    }

    /**
     * Works like {@link String#trim()}, but custom characters is excluded.
     *
     * @see {@link #trim(String)}
     */
    public static String trim(String str, char[] excluded) {
        if (null == str) {
            return null;
        }

        int start = 0, last = str.length() - 1;
        int end = last;
        while ((start <= end) && (Arrays.binarySearch(excluded, str.charAt(start)) >= 0)) {
            start++;
        }
        while ((end >= start) && (Arrays.binarySearch(excluded, str.charAt(end)) >= 0)) {
            end--;
        }
        if (start == 0 && end == last) {
            return str;
        }

        return str.substring(start, end + 1);
    }

    public static String remove(String str, char[] removed) {
        if (TextUtils.isEmpty(str) || null == removed || 0 == removed.length) {
            return str;
        }

        final char[] chars = str.toCharArray();
        int pos = 0;
        for (int i = 0; i < chars.length; i++) {
            if (!Utilities.contain(removed, chars[i])) {
                chars[pos++] = chars[i];
            }
        }
        return new String(chars, 0, pos);
    }
}
