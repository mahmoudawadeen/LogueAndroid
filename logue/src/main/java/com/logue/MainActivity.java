/*
 * Copyright (c) 2015.
 * author: Ionut Damian <damian@informatik.uni-augsburg.de>
 * created: 2015/02/27
 */

package com.logue;

import com.google.android.glass.media.Sounds;
import com.google.android.glass.widget.CardBuilder;
import com.google.android.glass.widget.CardScrollAdapter;
import com.google.android.glass.widget.CardScrollView;
import com.logue.util.ConfigUtils;

import com.ssj.*;
import com.ssj.core.*;

import android.app.Activity;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.util.Xml;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ImageView;

import org.xmlpull.v1.XmlPullParser;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * An {@link Activity} showing a tuggable "Hello World!" card.
 * <p/>
 * The main content view is composed of a one-card {@link CardScrollView} that provides tugging
 * feedback to the user when swipe gestures are detected.
 * If your Glassware intends to intercept swipe gestures, you should set the content view directly
 * and use a {@link com.google.android.glass.touchpad.GestureDetector}.
 *
 * @see <a href="https://developers.google.com/glass/develop/gdk/touch">GDK Developer Guide</a>
 */
public class MainActivity extends Activity {

    private final int GLASS_PORT = 34106;

    private final String SSI_IP = "137.250.171.175";
    private final int SSI_PORT = 34300;

    private Config _conf;
    private Console _console;

    private SSIConnector _ssi;
    private FeedbackManager _man;

    private Pipeline _pipe;

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);

        setContentView(R.layout.activity);

        //setup an SSJ pipeline to send sensor data to SSI
        _pipe = new Pipeline();

        AudioSensor audio = new AudioSensor();
        audio.options.updateRate = 20;
        audio.options.sampleRate = 16000;
        _pipe.setSensor(audio);

        SocketWriter socket = new SocketWriter();
        socket.options.type = SocketWriter.SOCKET_TYPE_UDP;
        socket.options.ip = SSI_IP;
        socket.options.port = SSI_PORT;
        _pipe.setConsumer(socket);

        _pipe.begin();

        //start up the logic
        _conf = new Config();
        _console = new Console(this);
        _ssi = new SSIConnector(null, GLASS_PORT);
        _man = new FeedbackManager(this, _ssi, _conf);

        //load config file
        XmlPullParser parser;
        try {
            InputStream in;

            //look for config file on sdcard first
            File sdcard = Environment.getExternalStorageDirectory();
            File folder = new File(sdcard.getPath() + "/logue");
            if(!folder.exists() && !folder.isDirectory())
            {
                if(!folder.mkdirs())
                    Log.e("Activity", "Error creating folder");
            }
            File file = new File(folder, "config.xml");

            if(file.exists())
                in = new FileInputStream(file);
            else {
                //if not found, copy the one from assets
                InputStream from = getAssets().open("config.xml");
                OutputStream to = new FileOutputStream(file);

                ConfigUtils.copyFile(from, to);

                from.close();
                to.close();

                //than try loading it again
                file = new File(folder, "config.xml");
                in = new FileInputStream(file);
            }

            parser = Xml.newPullParser();
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);

            parser.setInput(in, null);

            while(parser.next() != XmlPullParser.END_DOCUMENT)
            {
                switch(parser.getEventType())
                {
                    case XmlPullParser.START_TAG:
                        String name = parser.getName();

                        if(parser.getName().equalsIgnoreCase("options"))
                        {
                            _conf.load(parser);
                        }
                        else if(parser.getName().equalsIgnoreCase("classes"))
                        {
                            _man.load(parser);
                        }
                }
            }

            in.close();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    @Override
    protected void onDestroy() {

        try {
            _pipe.close();
            _console.close();
            _ssi.terminate();
            _man.close();
        }
        catch(Exception e)
        {
            e.printStackTrace();
        }

        super.onDestroy();
    }

    public void setIcon(Drawable icon, Drawable quality, int pos, int duration) {

        Context context = getApplicationContext();

        try {
            //set feedback icon
            int id = getResources().getIdentifier("icon_fb" + pos, "id", context.getPackageName());
            updateImage(id, icon);

            //set quality icon
            id = getResources().getIdentifier("icon_q" + pos, "id", context.getPackageName());
            updateImage(id, quality);
        }
        catch(Exception e)
        {
            e.printStackTrace();
        }
    }

    private void updateImage(final int id, final Drawable img)
    {
        this.runOnUiThread(new Runnable() {
            public void run() {
                ImageView text = (ImageView)findViewById(id);
                text.setImageDrawable(img);
            }
        });
    }

}
