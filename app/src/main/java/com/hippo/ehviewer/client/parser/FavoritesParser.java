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

import com.hippo.ehviewer.GetText;
import com.hippo.ehviewer.R;
import com.hippo.ehviewer.client.data.GalleryInfo;
import com.hippo.ehviewer.client.exception.EhException;
import com.hippo.ehviewer.client.exception.ParseException;
import com.hippo.util.ExceptionUtils;
import com.hippo.util.JsoupUtils;
import com.hippo.yorozuya.AssertUtils;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FavoritesParser {
    private static final Pattern PATTERN_PREV_PAGE = Pattern.compile("prev=(\\d+(-\\d+)?)");
    private static final Pattern PATTERN_NEXT_PAGE = Pattern.compile("next=(\\d+(-\\d+)?)");

    public static Result parse(String body) throws Exception {
        if (body.contains("This page requires you to log on.</p>")) {
            throw new EhException(GetText.getString(R.string.need_sign_in));
        }
        Document d = Jsoup.parse(body);

        String[] catArray = new String[10];
        int[] countArray = new int[10];
        try {
            Element ido = JsoupUtils.getElementByClass(d, "ido");
            //noinspection ConstantConditions
            Elements fps = ido.getElementsByClass("fp");
            // Last one is "fp fps"
            AssertUtils.assertEquals(11, fps.size());

            for (int i = 0; i < 10; i++) {
                Element fp = fps.get(i);
                countArray[i] = ParserUtils.parseInt(fp.child(0).text(), 0);
                catArray[i] = ParserUtils.trim(fp.child(2).text());
            }
        } catch (Throwable e) {
            ExceptionUtils.throwIfFatal(e);
            e.printStackTrace();
            throw new ParseException("Parse favorites error", body);
        }

        Result re = new Result();
        try {
            Element prev = d.getElementById("uprev");
            Element next = d.getElementById("unext");
            assert prev != null;
            assert next != null;
            Matcher matcherPrev = PATTERN_PREV_PAGE.matcher(prev.attr("href"));
            Matcher matcherNext = PATTERN_NEXT_PAGE.matcher(next.attr("href"));
            if (matcherPrev.find()) re.prevPage = matcherPrev.group(1);
            if (matcherNext.find()) re.nextPage = matcherNext.group(1);
        } catch (Throwable e) {
            e.printStackTrace();
        }

        GalleryListParser.Result result = GalleryListParser.parse(body);

        re.catArray = catArray;
        re.countArray = countArray;
        re.galleryInfoList = result.galleryInfoList;

        return re;
    }

    public static class Result {
        public String[] catArray; // Size 10
        public int[] countArray; // Size 10
        public String prevPage;
        public String nextPage;
        public List<GalleryInfo> galleryInfoList;
    }
}
