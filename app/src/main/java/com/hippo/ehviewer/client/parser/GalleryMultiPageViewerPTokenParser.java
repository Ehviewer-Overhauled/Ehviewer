package com.hippo.ehviewer.client.parser;

import com.hippo.ehviewer.client.exception.EhException;
import com.hippo.util.ExceptionUtils;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;

public class GalleryMultiPageViewerPTokenParser {
    public static ArrayList<String> parse(String body) throws Exception {
        ArrayList<String> list = new ArrayList<>();
        String imagelist = body.substring(body.indexOf("var imagelist = ") + 16, body.indexOf(";", body.indexOf("var imagelist = ")));
        try {
            JSONArray ja = new JSONArray(imagelist);
            for (int i = 0; i < ja.length(); i++) {
                JSONObject jo = ja.getJSONObject(i);
                list.add(jo.getString("k"));
            }
        } catch (Throwable e) {
            ExceptionUtils.throwIfFatal(e);
            throw new EhException(body);
        }
        return list;
    }
}
