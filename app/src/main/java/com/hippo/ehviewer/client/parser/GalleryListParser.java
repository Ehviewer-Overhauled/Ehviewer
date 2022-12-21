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

import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;

import com.hippo.ehviewer.EhDB;
import com.hippo.ehviewer.client.EhUtils;
import com.hippo.ehviewer.client.data.GalleryInfo;
import com.hippo.ehviewer.client.exception.ParseException;
import com.hippo.util.ExceptionUtils;
import com.hippo.util.JsoupUtils;
import com.hippo.yorozuya.NumberUtils;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GalleryListParser {

    private static final String TAG = GalleryListParser.class.getSimpleName();

    private static final String NO_UNFILTERED_TEXT = "No unfiltered results in this page range. You either requested an invalid page or used too aggressive filters.";

    private static final Pattern PATTERN_RATING = Pattern.compile("\\d+px");
    private static final Pattern PATTERN_THUMB_SIZE = Pattern.compile("height:(\\d+)px;width:(\\d+)px");
    private static final Pattern PATTERN_FAVORITE_SLOT = Pattern.compile("background-color:rgba\\((\\d+),(\\d+),(\\d+),");
    private static final Pattern PATTERN_PAGES = Pattern.compile("(\\d+) page");
    private static final Pattern PATTERN_PREV_PAGE = Pattern.compile("prev=(\\d+(-\\d+)?)");
    private static final Pattern PATTERN_NEXT_PAGE = Pattern.compile("next=(\\d+(-\\d+)?)");

    private static final String[][] FAVORITE_SLOT_RGB = new String[][]{
            new String[]{"0", "0", "0"},
            new String[]{"240", "0", "0"},
            new String[]{"240", "160", "0"},
            new String[]{"208", "208", "0"},
            new String[]{"0", "128", "0"},
            new String[]{"144", "240", "64"},
            new String[]{"64", "176", "240"},
            new String[]{"0", "0", "240"},
            new String[]{"80", "0", "128"},
            new String[]{"224", "128", "224"},
    };

    private static String parseRating(String ratingStyle) {
        Matcher m = PATTERN_RATING.matcher(ratingStyle);
        int num1 = Integer.MIN_VALUE;
        int num2 = Integer.MIN_VALUE;
        int rate = 5;
        String re;
        if (m.find()) {
            num1 = ParserUtils.parseInt(m.group().replace("px", ""), Integer.MIN_VALUE);
        }
        if (m.find()) {
            num2 = ParserUtils.parseInt(m.group().replace("px", ""), Integer.MIN_VALUE);
        }
        if (num1 == Integer.MIN_VALUE || num2 == Integer.MIN_VALUE) {
            return null;
        }
        rate = rate - num1 / 16;
        if (num2 == 21) {
            rate--;
            re = Integer.toString(rate);
            re = re + ".5";
        } else
            re = Integer.toString(rate);
        return re;
    }

    private static int parseFavoriteSlot(String style) {
        Matcher m = PATTERN_FAVORITE_SLOT.matcher(style);
        if (m.find()) {
            String r = m.group(1);
            String g = m.group(2);
            String b = m.group(3);
            int slot = 0;
            for (String[] rgb : FAVORITE_SLOT_RGB) {
                if (r.equals(rgb[0]) && g.equals(rgb[1]) && b.equals(rgb[2])) {
                    return slot;
                }
                slot++;
            }
        }
        return -2;
    }

    private static GalleryInfo parseGalleryInfo(Element e) {
        GalleryInfo gi = new GalleryInfo();

        // Title, gid, token (required), tags
        Element glname = JsoupUtils.getElementByClass(e, "glname");
        if (glname != null) {
            Element a = JsoupUtils.getElementByTag(glname, "a");
            if (a == null) {
                Element parent = glname.parent();
                if (parent != null && "a".equals(parent.tagName())) {
                    a = parent;
                }
            }
            if (a != null) {
                GalleryDetailUrlParser.Result result = GalleryDetailUrlParser.parse(a.attr("href"));
                if (result != null) {
                    gi.gid = result.gid;
                    gi.token = result.token;
                }
            }

            Element child = glname;
            Elements children = glname.children();
            while (children.size() != 0) {
                child = children.get(0);
                children = child.children();
            }
            gi.title = child.text().trim();
        }
        if (gi.title == null) {
            return null;
        }

        // Tags
        Elements gts = e.select(".gt, .gtl");
        if (gts.size() != 0) {
            ArrayList<String> tags = new ArrayList<>();
            for (Element gt : gts) {
                tags.add(gt.attr("title"));
            }
            gi.simpleTags = tags.toArray(new String[0]);
        }

        // Category
        gi.category = EhUtils.UNKNOWN;
        Element ce = JsoupUtils.getElementByClass(e, "cn");
        if (ce == null) {
            ce = JsoupUtils.getElementByClass(e, "cs");
        }
        if (ce != null) {
            gi.category = EhUtils.getCategory(ce.text());
        }

        // Thumb
        Element glthumb = JsoupUtils.getElementByClass(e, "glthumb");
        if (glthumb != null) {
            Element img = glthumb.select("div:nth-child(1)>img").first();
            if (img != null) {
                // Thumb size
                Matcher m = PATTERN_THUMB_SIZE.matcher(img.attr("style"));
                if (m.find()) {
                    gi.thumbWidth = NumberUtils.parseIntSafely(m.group(2), 0);
                    gi.thumbHeight = NumberUtils.parseIntSafely(m.group(1), 0);
                } else {
                    Log.w(TAG, "Can't parse gallery info thumb size");
                    gi.thumbWidth = 0;
                    gi.thumbHeight = 0;
                }
                // Thumb url
                String url = img.attr("data-src");
                if (TextUtils.isEmpty(url)) {
                    url = img.attr("src");
                }
                if (TextUtils.isEmpty(url)) {
                    url = null;
                }
                gi.thumb = EhUtils.handleThumbUrlResolution(url);
            }

            // Pages
            Element div = glthumb.select("div:nth-child(2)>div:nth-child(2)>div:nth-child(2)").first();
            if (div != null) {
                Matcher matcher = PATTERN_PAGES.matcher(div.text());
                if (matcher.find()) {
                    gi.pages = NumberUtils.parseIntSafely(matcher.group(1), 0);
                }
            }
        }
        // Try extended and thumbnail version
        if (gi.thumb == null) {
            Element gl = JsoupUtils.getElementByClass(e, "gl1e");
            if (gl == null) {
                gl = JsoupUtils.getElementByClass(e, "gl3t");
            }
            if (gl != null) {
                Element img = JsoupUtils.getElementByTag(gl, "img");
                if (img != null) {
                    // Thumb size
                    Matcher m = PATTERN_THUMB_SIZE.matcher(img.attr("style"));
                    if (m.find()) {
                        gi.thumbWidth = NumberUtils.parseIntSafely(m.group(2), 0);
                        gi.thumbHeight = NumberUtils.parseIntSafely(m.group(1), 0);
                    } else {
                        Log.w(TAG, "Can't parse gallery info thumb size");
                        gi.thumbWidth = 0;
                        gi.thumbHeight = 0;
                    }
                    gi.thumb = EhUtils.handleThumbUrlResolution(img.attr("src"));
                }
            }
        }

        // Posted
        gi.favoriteSlot = -2;
        Element posted = e.getElementById("posted_" + gi.gid);
        if (posted != null) {
            gi.posted = posted.text().trim();
            gi.favoriteSlot = parseFavoriteSlot(posted.attr("style"));
        }
        if (gi.favoriteSlot == -2) {
            gi.favoriteSlot = EhDB.containLocalFavorites(gi.gid) ? -1 : -2;
        }

        // Rating
        Element ir = JsoupUtils.getElementByClass(e, "ir");
        if (ir != null) {
            gi.rating = NumberUtils.parseFloatSafely(parseRating(ir.attr("style")), -1.0f);
            // TODO The gallery may be rated even if it doesn't has one of these classes
            gi.rated = ir.hasClass("irr") || ir.hasClass("irg") || ir.hasClass("irb");
        }

        // Uploader and pages
        Element gl = JsoupUtils.getElementByClass(e, "glhide");
        int uploaderIndex = 0;
        int pagesIndex = 1;
        if (gl == null) {
            // For extended
            gl = JsoupUtils.getElementByClass(e, "gl3e");
            uploaderIndex = 3;
            pagesIndex = 4;
        }
        if (gl != null) {
            Elements children = gl.children();
            if (children.size() > uploaderIndex) {
                Element div = children.get(uploaderIndex);
                if (div != null) {
                    gi.disowned = div.attr("style").contains("opacity:0.5");
                    Element a = div.children().first();
                    gi.uploader = a == null ? div.text().trim() : a.text().trim();
                }
            }
            if (children.size() > pagesIndex) {
                Matcher matcher = PATTERN_PAGES.matcher(children.get(pagesIndex).text());
                if (matcher.find()) {
                    gi.pages = NumberUtils.parseIntSafely(matcher.group(1), 0);
                }
            }
        }
        // For thumbnail
        Element gl5t = JsoupUtils.getElementByClass(e, "gl5t");
        if (gl5t != null) {
            Element div = gl5t.select("div:nth-child(2)>div:nth-child(2)").first();
            if (div != null) {
                Matcher matcher = PATTERN_PAGES.matcher(div.text());
                if (matcher.find()) {
                    gi.pages = NumberUtils.parseIntSafely(matcher.group(1), 0);
                }
            }
        }

        gi.generateSLang();

        return gi;
    }

    public static Result parse(@NonNull String body) throws Exception {

        Result result = new Result();
        Document d = Jsoup.parse(body);
        try {
            Element prev = d.getElementById("uprev");
            Element next = d.getElementById("unext");
            assert prev != null;
            assert next != null;
            Matcher matcherPrev = PATTERN_PREV_PAGE.matcher(prev.attr("href"));
            Matcher matcherNext = PATTERN_NEXT_PAGE.matcher(next.attr("href"));
            if (matcherPrev.find())
                result.prev = matcherPrev.group(1);
            if (matcherNext.find())
                result.next = matcherNext.group(1);
        } catch (Throwable e) {
            ExceptionUtils.throwIfFatal(e);
            result.noWatchedTags = body.contains("<p>You do not have any watched tags");
            if (body.contains("No hits found</p>")) {
                //noinspection unchecked
                result.galleryInfoList = Collections.EMPTY_LIST;
                return result;
            }
        }

        try { // For toplists
            Element ptt = d.getElementsByClass("ptt").first();
            if (ptt != null) {
                Elements es = ptt.child(0).child(0).children();
                result.pages = Integer.parseInt(es.get(es.size() - 2).text().trim());

                Element e = es.get(es.size() - 1);
                if (e != null) {
                    e = e.children().first();
                    if (e != null) {
                        String href = e.attr("href");
                        Matcher matcher = PATTERN_NEXT_PAGE.matcher(href);
                        if (matcher.find()) {
                            result.nextPage = NumberUtils.parseIntSafely(matcher.group(1), 0);
                        }
                    }
                }
            }
        } catch (Throwable e) {
            e.printStackTrace();
        }

        try {
            Element itg = d.getElementsByClass("itg").first();
            Elements es;
            if ("table".equalsIgnoreCase(itg.tagName())) {
                es = itg.child(0).children();
            } else {
                es = itg.children();
            }
            List<GalleryInfo> list = new ArrayList<>(es.size());
            // First one is table header, skip it
            for (int i = 0; i < es.size(); i++) {
                GalleryInfo gi = parseGalleryInfo(es.get(i));
                if (null != gi) {
                    list.add(gi);
                }
            }
            if (list.isEmpty()) {
                if (es.size() < 2 || !NO_UNFILTERED_TEXT.equals(es.get(1).text())) {
                    Log.d(TAG, "No gallery found");
                }
            }
            result.galleryInfoList = list;
        } catch (Throwable e) {
            ExceptionUtils.throwIfFatal(e);
            e.printStackTrace();
            throw new ParseException("Can't parse gallery list", body);
        }

        return result;
    }

    public static class Result {
        public int pages;
        public int nextPage;
        public String prev;
        public String next;
        public boolean noWatchedTags;
        public List<GalleryInfo> galleryInfoList;
    }
}
