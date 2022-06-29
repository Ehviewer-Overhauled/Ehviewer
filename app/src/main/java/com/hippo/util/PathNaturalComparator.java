/*
 * Copyright 2022 Tarsin Norbin
 *
 * This file is part of EhViewer
 *
 * EhViewer is free software: you can redistribute it and/or
 * modify it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * EhViewer is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with EhViewer.
 * If not, see <https://www.gnu.org/licenses/>.
 */

package com.hippo.util;

import java.util.Comparator;

/**
 * Compares each path segment in natural sort order.
 * {@code \} and {@code /} are the same. Duplicate {@code \} and {@code /} are ignored.
 * Leading {@code \} and {@code /} are ignored, so there is no difference between
 * relative path and absolute path.
 * {@code /./} and {@code /../} are treated as normal path.
 */
public class PathNaturalComparator implements Comparator<String> {

    private static final boolean TYPE_SEPARATOR = false;
    private static final boolean TYPE_NORMAL = true;

    private final Comparator<String> naturalComparator = new NaturalComparator();

    private static int nextSegmentStart(String str, int index) {
        if (index >= str.length()) {
            return index;
        }

        boolean type = getType(str.charAt(index));
        int i = index + 1;
        while (i < str.length() && type == getType(str.charAt(i))) {
            i += 1;
        }

        return i;
    }

    private static boolean getType(char c) {
        if (c == '/' || c == '\\') {
            return TYPE_SEPARATOR;
        } else {
            return TYPE_NORMAL;
        }
    }

    @Override
    public int compare(String o1, String o2) {
        if (o1 == null && o2 == null) {
            return 0;
        }
        if (o1 == null) {
            return -1;
        }
        if (o2 == null) {
            return 1;
        }

        int index1 = 0;
        int index2 = 0;

        while (true) {
            String data1;
            String data2;

            for (; ; ) {
                int newIndex1 = nextSegmentStart(o1, index1);
                if (newIndex1 == index1) {
                    data1 = null;
                    break;
                }
                if (getType(o1.charAt(newIndex1 - 1)) == TYPE_NORMAL) {
                    data1 = o1.substring(index1, newIndex1);
                    index1 = newIndex1;
                    break;
                }
                index1 = newIndex1;
            }

            for (; ; ) {
                int newIndex2 = nextSegmentStart(o2, index2);
                if (newIndex2 == index2) {
                    data2 = null;
                    break;
                }
                if (getType(o2.charAt(newIndex2 - 1)) == TYPE_NORMAL) {
                    data2 = o2.substring(index2, newIndex2);
                    index2 = newIndex2;
                    break;
                }
                index2 = newIndex2;
            }

            if (data1 == null && data2 == null) {
                return 0;
            }
            if (data1 == null) {
                return -1;
            }
            if (data2 == null) {
                return 1;
            }

            int result = naturalComparator.compare(data1, data2);
            if (result != 0) {
                return result;
            }
        }
    }
}
