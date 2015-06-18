/*
 * Copyright (c) 2014.
 * author: Ionut Damian <damian@informatik.uni-augsburg.de>
 * created: 2014/12/02
 */

package com.logue;

import android.support.v4.util.SimpleArrayMap;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;

/**
 * Created by Johnny on 02.12.2014.
 */
public class Config {

    protected SimpleArrayMap<String,String> _options = new SimpleArrayMap<String, String>();

    public void load(XmlPullParser parser) throws IOException, XmlPullParserException {
        parser.require(XmlPullParser.START_TAG, null, "options");

        //iterate through classes
        while (parser.next() != XmlPullParser.END_DOCUMENT)
        {
            if(parser.getEventType() == XmlPullParser.START_TAG && parser.getName().equalsIgnoreCase("option"))
            {
                String name = parser.getAttributeValue(null, "name");
                String value = "";
                value = parser.nextText();

                _options.put(name, value);
            }
            else if(parser.getEventType() == XmlPullParser.END_TAG && parser.getName().equalsIgnoreCase("options"))
                break; //jump out once we reach end tag
        }

        parser.require(XmlPullParser.END_TAG, null, "options");
    }

    public String getOption(String name) {
        return _options.get(name);
    }

    public float getOptionF(String name) {
        return Float.parseFloat(_options.get(name));
    }

    public int getOptionI(String name) {
        return Integer.parseInt(_options.get(name));
    }
}
