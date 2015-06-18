/*
 * Copyright (c) 2014.
 * author: Ionut Damian <damian@informatik.uni-augsburg.de>
 * created: 2014/12/02
 */

package com.logue;

import android.app.Activity;
import android.graphics.drawable.Drawable;
import android.util.Xml;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.util.ArrayList;

/**
 * Created by Johnny on 02.12.2014.
 */
public class FeedbackManager extends Thread {

    protected MainActivity _activity;
    protected SSIConnector _comm;
    protected Config _conf;

    protected boolean _terminate = false;
    protected boolean _safeToKill = false;

    protected ArrayList<FeedbackClass> _classes = new ArrayList<FeedbackClass>();
    protected ArrayList<Drawable> _qualityIcons = new ArrayList<Drawable>();

    public FeedbackManager(Activity act, SSIConnector comm, Config conf)
    {
        _activity = (MainActivity)act;
        _comm = comm;
        _conf = conf;

        start();
    }

    public void close() throws InterruptedException
    {
        _terminate = true;
        while(!_safeToKill)
            sleep(200);
    }

    public void terminate()
    {
    }

    @Override
    public void run()
    {
        while(!_terminate)
        {
            try {
                update();
                sleep(100); //update once every 100ms
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
        }

        _safeToKill = true;
    }

    public void update() throws XmlPullParserException, IOException {

        XmlPullParser xml = Xml.newPullParser();
        xml.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
        String event = null;

        do {
            event = _comm.getLastEvent(false);

            if(event != null) {
                StringReader reader = new StringReader(event);
                xml.setInput(reader);

                while (xml.next() != XmlPullParser.END_DOCUMENT)
                {
                    if(xml.getEventType() == XmlPullParser.START_TAG && xml.getName().equalsIgnoreCase("event"))
                    {
                        process(xml);
                    }
                    else if(xml.getEventType() == XmlPullParser.END_TAG && xml.getName().equalsIgnoreCase("events"))
                        break; //jump out once we reach end tag
                }
                reader.close();
            }
        } while(event != null);
    }

    public void process(XmlPullParser event) throws XmlPullParserException, IOException {

        if(_classes.size() == 0)
            return;

        int id = 0;
        for(FeedbackClass i : _classes)
        {
            if(i.checkEvent(event))
            {
                i.parseEvent(event);

                Drawable icon_behaviour = i.getIcon();
                float quality = i.getQuality(false);
                Drawable icon_quality = getQualityIcon(quality);

                _activity.setIcon(icon_behaviour,       //feedback class icon
                        icon_quality,                   //quality icon (appropriateness)
                        id,                             //id of feedback class defines position on screen
                        _conf.getOptionI("timeout"),
                        quality);   //timeout of feedback is globally defined
                break; //one event can be processed by only one class
            }
            id++;
        }
    }

    public void load(XmlPullParser parser) throws IOException, XmlPullParserException
    {
        parser.require(XmlPullParser.START_TAG, null, "classes");

        //iterate through classes
        while (parser.next() != XmlPullParser.END_DOCUMENT)
        {
            if(parser.getEventType() == XmlPullParser.START_TAG && parser.getName().equalsIgnoreCase("class"))
            {
                //parse feedback classes
                FeedbackClass c = FeedbackClass.create(parser, _activity.getApplicationContext());
                _classes.add(c);

                //load quality icons
                InputStream neg_is = _activity.getAssets().open(_conf.getOption("qualityIconNeg"));
                Drawable neg = Drawable.createFromStream(neg_is, null);
                _qualityIcons.add(neg);

                InputStream pos_is = _activity.getAssets().open(_conf.getOption("qualityIconPos"));
                Drawable pos = Drawable.createFromStream(pos_is, null);
                _qualityIcons.add(pos);

                //init gui
                _activity.setIcon(c.getIcon(), getQualityIcon(1), _classes.size() - 1, _conf.getOptionI("timeout"),-1);
            }
            else if(parser.getEventType() == XmlPullParser.END_TAG && parser.getName().equalsIgnoreCase("classes"))
                break; //jump out once we reach end tag for classes
        }

        parser.require(XmlPullParser.END_TAG, null, "classes");
    }

    Drawable getQualityIcon(float quality)
    {
        if(quality < 0.5)
            return _qualityIcons.get(0); //neg
        else
            return _qualityIcons.get(1); //pos
    }
}
