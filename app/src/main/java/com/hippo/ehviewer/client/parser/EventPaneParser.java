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

import com.hippo.util.ExceptionUtils;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

public class EventPaneParser {

    public static String parse(String body) {
        String event = null;
        try {
            Document d = Jsoup.parse(body);
            Element eventpane = d.getElementById("eventpane");
            if (eventpane != null) {
                event = eventpane.html();
            }
        } catch (Throwable e) {
            ExceptionUtils.throwIfFatal(e);
            e.printStackTrace();
        }
        return event;
    }


}
