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
