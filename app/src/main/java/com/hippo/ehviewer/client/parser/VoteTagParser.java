package com.hippo.ehviewer.client.parser;

import org.json.JSONException;
import org.json.JSONObject;

public class VoteTagParser {

    // {"error":"The tag \"neko\" is not allowed. Use character:neko or artist:neko"}
    public static VoteTagParser.Result parse(String body) throws JSONException {
        VoteTagParser.Result result = new VoteTagParser.Result();
        JSONObject jo = new JSONObject(body);
        if (jo.has("error")) result.error = jo.getString("error");
        return result;
    }

    public static class Result {
        public String error;
    }
}
