/*
 * Copyright (c) 2014.
 * author: Ionut Damian <damian@informatik.uni-augsburg.de>
 * created: 2014/12/02
 */

package com.logue.classes;
import com.logue.Console;
import com.logue.FeedbackClass;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import java.io.IOException;
import java.util.ArrayList;

/**
 * Created by Johnny on 01.12.2014.
 */
public class SpeechRate extends FeedbackClass {

    ArrayList<Float> _sr = new ArrayList<Float>();

    @Override
    public void parseEvent(XmlPullParser xml) throws XmlPullParserException, IOException
    {
        xml.require(XmlPullParser.START_TAG, null, "event");

        float srate = 0;
        while (xml.next() != XmlPullParser.END_DOCUMENT)
        {
            if (xml.getEventType() == XmlPullParser.START_TAG && xml.getName().equalsIgnoreCase("tuple"))
            {
                if (xml.getAttributeValue(null, "string").equalsIgnoreCase("Speechrate (syllables/sec)")) {
                    srate = Float.parseFloat(xml.getAttributeValue(null, "value"));

                    _sr.add(srate);
                    if (_sr.size() > 5)
                        _sr.remove(0);

                    break;
                }
            }
            else if(xml.getEventType() == XmlPullParser.END_TAG && xml.getName().equalsIgnoreCase("event"))
                break; //jump out once we reach end tag
        }

        _value = getAvg(_sr);
        Console.print("SpeechRate_avg = " + _value);
    }

    private float getAvg(ArrayList<Float> vec)
    {
        if(vec.size() == 0)
            return 0;

        float sum = 0;
        for(float i : vec)
        {
            sum += i;
        }
        return sum / vec.size();
    }
}
