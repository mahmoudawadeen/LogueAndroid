/*
 * Copyright (c) 2014.
 * author: Ionut Damian <damian@informatik.uni-augsburg.de>
 * created: 2014/12/02
 */

package com.logue;

import android.content.Context;
import android.graphics.drawable.Drawable;

import com.logue.classes.SpeechRate;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;


/**
 * Created by Johnny on 01.12.2014.
 */
public class FeedbackClass {

    protected String _name;
    protected String _event;
    protected String _sender;

    protected float _value = 0.5f;
    protected ArrayList<Threshold> _thres = new ArrayList<Threshold>();

    public static FeedbackClass create(XmlPullParser xml, Context context)
    {
        FeedbackClass c = null;

        if(xml.getAttributeValue(null, "name").equalsIgnoreCase("SpeechRate"))
            c = new SpeechRate();
        else
            c = new FeedbackClass();

        try {
            c.load(xml, context);
        }
        catch(Exception e)
        {
            e.printStackTrace();
        }

        return c;
    }

    public boolean checkEvent(XmlPullParser xml)
    {
        if (xml.getAttributeValue(null, "event").equalsIgnoreCase(_event)
                && xml.getAttributeValue(null, "sender").equalsIgnoreCase(_sender))
            return true;

        return false;
    }

    public void parseEvent(XmlPullParser xml) throws XmlPullParserException, IOException
    {
        xml.require(XmlPullParser.START_TAG, null, "event");

        _value = Float.parseFloat(xml.nextText());

        xml.require(XmlPullParser.END_TAG, null, "event");
    }

    public void setValue(float value)
    {
        _value = value;
    }

    //returns icon which should be currently displayed
    public Drawable getIcon()
    {
        Drawable icon = _thres.get(0).icon;

        for(Threshold t : _thres)
        {
            if(_value > t.value)
                icon = t.icon;
        }

        return icon;
    }

    /**
     * Returns quality indicator between 0 and 1
     * @param continuous computes exact quality value instead of snapping to thresholds
     */
    public float getQuality(boolean continuous)
    {
        float q;

        Threshold lower_thres = null;
        Threshold upper_thres = null;
        for(Threshold t : _thres)
        {
            if(_value >= t.value)
            lower_thres = new Threshold(t);

            if(lower_thres != null && _value < t.value && t.quality != lower_thres.quality)
            upper_thres = new Threshold(t);
        }

        if(!continuous)
            q = lower_thres.quality;
        else
        {
            if(upper_thres != null)
            {
                float median_rel = (upper_thres.value - lower_thres.value) / 2;
                float median_abs = median_rel + lower_thres.value;

                float x = Math.abs(median_abs - _value) / median_rel;

                q = lower_thres.quality * (1.0f - x) + upper_thres.quality * x;
            }
            else
            {
                float x = Math.abs(lower_thres.value - _value) / lower_thres.value;
                if(x > 1.0) x = 1;

                q = lower_thres.quality * (1.0f - x) + (1.0f - lower_thres.quality) * x;
            }
        }

        return q;
    }

    protected FeedbackClass()
    {
    }

    protected void load(XmlPullParser xml, Context context) throws XmlPullParserException, IOException
    {
        xml.require(XmlPullParser.START_TAG, null, "class");

        _name = xml.getAttributeValue(null, "name");
        _event = xml.getAttributeValue(null, "event");
        _sender = xml.getAttributeValue(null, "sender");

        while (xml.next() != XmlPullParser.END_DOCUMENT)
        {
            if (xml.getEventType() == XmlPullParser.START_TAG && xml.getName().equalsIgnoreCase("threshold"))
            {
                Threshold t = new Threshold();
                t.value = Float.parseFloat(xml.getAttributeValue(null, "value"));
                t.quality = (xml.getAttributeValue(null, "quality").equalsIgnoreCase("positive")) ? 1 : 0;
                t.iconName = xml.getAttributeValue(null, "icon");

                InputStream icon_is = context.getAssets().open(t.iconName);
                t.icon = Drawable.createFromStream(icon_is, null);

                _thres.add(t);
            }
            else if(xml.getEventType() == XmlPullParser.END_TAG && xml.getName().equalsIgnoreCase("class"))
                break; //jump out once we reach end tag
        }

        xml.require(XmlPullParser.END_TAG, null, "class");
    }
}
