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
