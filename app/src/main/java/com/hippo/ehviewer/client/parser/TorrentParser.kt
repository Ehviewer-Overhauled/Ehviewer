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

import org.jsoup.Jsoup;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.IntFunction;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TorrentParser {

    private static final Pattern PATTERN_TORRENT = Pattern.compile("</span> ([0-9-]+) [0-9:]+</td>[\\s\\S]+</span> ([0-9\\.]+ [KMGT]B)</td>[\\s\\S]+</span> ([0-9]+)</td>[\\s\\S]+</span> ([0-9]+)</td>[\\s\\S]+</span> ([0-9]+)</td>[\\s\\S]+</span>([^<]+)</td>[\\s\\S]+onclick=\"document.location='([^\"]+)'[^<]+>([^<]+)</a>");

    public static List<Result> parse(String body) {
        var torrentList = new ArrayList<Result>();
        var d = Jsoup.parse(body);
        var es = d.select("form>div>table");
        for (var e : es) {
            Matcher m = PATTERN_TORRENT.matcher(e.html());
            if (m.find()) {
                String posted = m.group(1);
                String size = m.group(2);
                int seeds = Integer.parseInt(m.group(3));
                int peers = Integer.parseInt(m.group(4));
                int downloads = Integer.parseInt(m.group(5));
                String uploader = ParserUtils.trim(m.group(6));
                String url = ParserUtils.trim(m.group(7));
                String name = ParserUtils.trim(m.group(8));
                var item = new Result(posted, size, seeds, peers, downloads, uploader, url, name);
                torrentList.add(item);
            }
        }
        return torrentList;
    }

    public static final class Result {
        private final String posted;
        private final String size;
        private final int seeds;
        private final int peers;
        private final int downloads;
        private final String uploader;
        private final String url;
        private final String name;

        public Result(String posted, String size, int seeds, int peers, int downloads, String uploader, String url, String name) {
            this.posted = posted;
            this.size = size;
            this.seeds = seeds;
            this.peers = peers;
            this.downloads = downloads;
            this.uploader = uploader;
            this.url = url;
            this.name = name;
        }

        public String format(IntFunction<String> getString) {
            return String.format("[%s] %s [%s] [↑%s ↓%s ✓%s]", posted, name, size, seeds, peers, downloads);
        }

        public String posted() {
            return posted;
        }

        public String size() {
            return size;
        }

        public int seeds() {
            return seeds;
        }

        public int peers() {
            return peers;
        }

        public int downloads() {
            return downloads;
        }

        public String uploader() {
            return uploader;
        }

        public String url() {
            return url;
        }

        public String name() {
            return name;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == this) return true;
            if (obj == null || obj.getClass() != this.getClass()) return false;
            var that = (Result) obj;
            return Objects.equals(this.posted, that.posted) &&
                    Objects.equals(this.size, that.size) &&
                    this.seeds == that.seeds &&
                    this.peers == that.peers &&
                    this.downloads == that.downloads &&
                    Objects.equals(this.uploader, that.uploader) &&
                    Objects.equals(this.url, that.url) &&
                    Objects.equals(this.name, that.name);
        }

        @Override
        public int hashCode() {
            return Objects.hash(posted, size, seeds, peers, downloads, uploader, url, name);
        }

        @NonNull
        @Override
        public String toString() {
            return "Result[" +
                    "posted=" + posted + ", " +
                    "size=" + size + ", " +
                    "seeds=" + seeds + ", " +
                    "peers=" + peers + ", " +
                    "downloads=" + downloads + ", " +
                    "uploader=" + uploader + ", " +
                    "url=" + url + ", " +
                    "name=" + name + ']';
        }
    }
}
