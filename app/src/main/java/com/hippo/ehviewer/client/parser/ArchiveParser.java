/*
 * Copyright 2016 Hippo Seven
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

package com.hippo.ehviewer.client.parser;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ArchiveParser {

    private static final Pattern PATTERN_FORM = Pattern.compile("<form id=\"hathdl_form\" action=\"[^\"]*?or=([^=\"]*?)\" method=\"post\">");
    private static final Pattern PATTERN_ARCHIVE = Pattern.compile("<p><a href=\"[^\"]*\" onclick=\"return do_hathdl\\('([0-9]+|org)'\\)\">([^<]+)</a></p>\\s*<p>([\\w. ]+)</p>\\s*<p>([\\w. ]+)</p>");

    public static Result parse(String body) {
        Matcher m = PATTERN_FORM.matcher(body);
        if (!m.find()) {
            return new Result(null, null);
        }
        String paramOr = m.group(1);
        var archiveList = new ArrayList<Archive>();
        m = PATTERN_ARCHIVE.matcher(body);
        while (m.find()) {
            String res = ParserUtils.trim(m.group(1));
            String name = ParserUtils.trim(m.group(2));
            String size = ParserUtils.trim(m.group(3));
            String cost = ParserUtils.trim(m.group(4));
            var item = new Archive(res, name, size, cost);
            archiveList.add(item);
        }
        return new Result(paramOr, archiveList);
    }

    public static final class Archive {
        private final String res;
        private final String name;
        private final String size;
        private final String cost;

        public Archive(String res, String name, String size, String cost) {
            this.res = res;
            this.name = name;
            this.size = size;
            this.cost = cost;
        }

        public String format() {
            return String.format("%s [%s, %s]", name, size, cost);
        }

        public String res() {
            return res;
        }

        public String name() {
            return name;
        }

        public String size() {
            return size;
        }

        public String cost() {
            return cost;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == this) return true;
            if (obj == null || obj.getClass() != this.getClass()) return false;
            var that = (Archive) obj;
            return Objects.equals(this.res, that.res) &&
                    Objects.equals(this.name, that.name) &&
                    Objects.equals(this.size, that.size) &&
                    Objects.equals(this.cost, that.cost);
        }

        @Override
        public int hashCode() {
            return Objects.hash(res, name, size, cost);
        }

        @NonNull
        @Override
        public String toString() {
            return "Archive[" +
                    "res=" + res + ", " +
                    "name=" + name + ", " +
                    "size=" + size + ", " +
                    "cost=" + cost + ']';
        }

    }

    public static final class Result {
        private final String paramOr;
        private final List<Archive> archiveList;

        public Result(String paramOr, List<Archive> archiveList) {
            this.paramOr = paramOr;
            this.archiveList = archiveList;
        }

        public String paramOr() {
            return paramOr;
        }

        public List<Archive> archiveList() {
            return archiveList;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == this) return true;
            if (obj == null || obj.getClass() != this.getClass()) return false;
            var that = (Result) obj;
            return Objects.equals(this.paramOr, that.paramOr) &&
                    Objects.equals(this.archiveList, that.archiveList);
        }

        @Override
        public int hashCode() {
            return Objects.hash(paramOr, archiveList);
        }

        @NonNull
        @Override
        public String toString() {
            return "Result[" +
                    "paramOr=" + paramOr + ", " +
                    "archiveList=" + archiveList + ']';
        }

    }
}
