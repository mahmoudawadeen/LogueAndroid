/*
 * Copyright (c) 2015.
 * author: Ionut Damian <damian@informatik.uni-augsburg.de>
 * created: 2015/02/27
 */

package com.logue;

import com.google.android.glass.eye.EyeGesture;
import com.google.android.glass.eye.EyeGestureManager;

import com.google.android.glass.widget.CardScrollView;
import com.logue.util.ConfigUtils;

import com.ssj.*;
import com.ssj.core.*;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.app.Activity;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.util.Xml;
import android.view.WindowManager;
import android.view.animation.DecelerateInterpolator;
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

    private EyeGestureManager mEyeGestureManager;
    private EyeGestureListener mEyeGestureListener;
    private EyeGesture blink = EyeGesture.BLINK;
    private EyeGesture wink = EyeGesture.WINK;
    private EyeGesture doubleWink = EyeGesture.DOUBLE_WINK;
    private EyeGesture doubleBlink = EyeGesture.DOUBLE_BLINK;

    private static final String TAG = "EyeGestureTest";



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

        //setup gestures
        mEyeGestureManager = EyeGestureManager.from(this);
        mEyeGestureListener = new EyeGestureListener();


        //load config file
        XmlPullParser parser;
        try {
            InputStream in;

            //look for config file on sdcard first
            File sdcard = Environment.getExternalStorageDirectory();
            File folder = new File(sdcard.getPath() + "/logue");
            if (!folder.exists() && !folder.isDirectory()) {
                if (!folder.mkdirs())
                    Log.e("Activity", "Error creating folder");
            }
            File file = new File(folder, "config.xml");

            if (file.exists())
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

            while (parser.next() != XmlPullParser.END_DOCUMENT) {
                switch (parser.getEventType()) {
                    case XmlPullParser.START_TAG:
                        String name = parser.getName();

                        if (parser.getName().equalsIgnoreCase("options")) {
                            _conf.load(parser);
                        } else if (parser.getName().equalsIgnoreCase("classes")) {
                            _man.load(parser);
                        }
                }
            }

            in.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    @Override
    protected void onStart(){
        super.onStart();
        Log.i(TAG, "Binding listener");
        mEyeGestureManager.register(blink, mEyeGestureListener);
        mEyeGestureManager.register(wink, mEyeGestureListener);
        mEyeGestureManager.register(doubleBlink, mEyeGestureListener);
        mEyeGestureManager.register(doubleWink, mEyeGestureListener);
        Log.i(TAG, "App started");

    }
    @Override
    protected void onStop(){
        super.onStop();

        mEyeGestureManager.unregister(blink, mEyeGestureListener);
        mEyeGestureManager.unregister(wink, mEyeGestureListener);
        mEyeGestureManager.unregister(doubleBlink, mEyeGestureListener);
        mEyeGestureManager.unregister(doubleWink, mEyeGestureListener);
        Log.i(TAG, "App stopped");
    }
    @Override
    protected void onDestroy() {

        try {
            _pipe.close();
            _console.close();
            _ssi.terminate();
            _man.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        super.onDestroy();
    }

    public void setIcon(Drawable icon, Drawable quality, int pos, int duration, float quality_value) {

        Context context = getApplicationContext();

        try {
            //set feedback icon
            int id = getResources().getIdentifier("icon_fb" + pos, "id", context.getPackageName());
            updateImage(id, icon, quality_value);

            //set quality icon
            id = getResources().getIdentifier("icon_q" + pos, "id", context.getPackageName());
            updateImage(id, quality, quality_value);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void updateImage(final int id, final Drawable img, final float quality) {
        this.runOnUiThread(new Runnable() {
            public void run() {
                final ImageView text = (ImageView) findViewById(id);
                ImageView other = null; //image qualityIcon the feedback icon

                switch (id) {
                    case R.id.icon_fb0:
                        other = (ImageView) findViewById(R.id.icon_q0);
                        break;
                    case R.id.icon_fb1:
                        other = (ImageView) findViewById(R.id.icon_q1);
                        break;
                    case R.id.icon_fb2:
                        other = (ImageView) findViewById(R.id.icon_q2);
                        break;
                    case R.id.icon_q0:
                        other = (ImageView) findViewById(R.id.icon_fb0);
                        break;
                    case R.id.icon_q1:
                        other = (ImageView) findViewById(R.id.icon_fb1);
                        break;
                    case R.id.icon_q2:
                        other = (ImageView) findViewById(R.id.icon_fb2);
                        break;
                }
                if (text.getDrawable() != null) {

                    AnimatorSet animatorSet = new AnimatorSet();
                    animatorSet.setInterpolator(new DecelerateInterpolator());
                    if (!text.getDrawable().equals(img)) {
                        final ObjectAnimator fadeInGood = ObjectAnimator.ofFloat(text, "alpha", 0f, 1f, 0f);
                        fadeInGood.setDuration(8000);
                        fadeInGood.setAutoCancel(false);
                        fadeInGood.addListener(new Animator.AnimatorListener() {
                            @Override
                            public void onAnimationStart(Animator animation) {
                                text.setImageDrawable(img);
                                text.setTag(R.id.lock, true);
                            }

                            @Override
                            public void onAnimationEnd(Animator animation) {
                                text.setTag(R.id.lock, false);
                            }

                            @Override
                            public void onAnimationCancel(Animator animation) {

                            }

                            @Override
                            public void onAnimationRepeat(Animator animation) {

                            }
                        });

                        final ObjectAnimator fadeInGoodQuality = ObjectAnimator.ofFloat(other, "alpha", 0f, 1f, 0f);
                        fadeInGoodQuality.setAutoCancel(false);
                        fadeInGoodQuality.setDuration(8000);


                        final ObjectAnimator fadeInBad = ObjectAnimator.ofFloat(text, "alpha", 1f);
                        fadeInBad.setAutoCancel(false);
                        fadeInBad.setDuration(4000);
                        fadeInBad.addListener(new Animator.AnimatorListener() {
                            @Override
                            public void onAnimationStart(Animator animation) {
                                text.setImageDrawable(img);
                                text.setTag(R.id.lock, true);


                            }

                            @Override
                            public void onAnimationEnd(Animator animation) {
                                text.setTag(R.id.lock, false);

                            }

                            @Override
                            public void onAnimationCancel(Animator animation) {

                            }

                            @Override
                            public void onAnimationRepeat(Animator animation) {

                            }
                        });

                        final ObjectAnimator fadeInBadQuality = ObjectAnimator.ofFloat(other, "alpha", 1f);
                        fadeInBadQuality.setAutoCancel(false);
                        fadeInBadQuality.setDuration(4000);


                        final ObjectAnimator fadeOut = ObjectAnimator.ofFloat(other, "alpha", 0f);
                        fadeOut.setDuration(4000);
                        fadeOut.setAutoCancel(false);
                        fadeOut.addListener(new Animator.AnimatorListener() {
                            @Override
                            public void onAnimationStart(Animator animation) {
                                text.setTag(R.id.lock, true);

                            }

                            @Override
                            public void onAnimationEnd(Animator animation) {
                                text.setTag(R.id.lock, false);
                            }

                            @Override
                            public void onAnimationCancel(Animator animation) {
                            }

                            @Override
                            public void onAnimationRepeat(Animator animation) {

                            }
                        });

                        final ObjectAnimator fadeOutQuality = ObjectAnimator.ofFloat(other, "alpha", 0f);
                        fadeOutQuality.setDuration(4000);
                        fadeOutQuality.setAutoCancel(false);


                        if (!(boolean) text.getTag(R.id.lock)) {
                            if ((boolean) text.getTag(R.id.visible)) {
                                text.setTag(R.id.visible, false);
                                animatorSet.play(fadeOut).with(fadeOutQuality);

                            } else {
                                if (quality == 0) {
                                    text.setTag(R.id.visible, true);
                                    animatorSet.play(fadeInBad).with(fadeInBadQuality);
                                } else {
                                    if (!(boolean) text.getTag(R.id.lock)) {
                                        text.setTag(R.id.visible, false);
                                        animatorSet.play(fadeInGood).with(fadeInGoodQuality);
                                    }
                                }
                            }
                            animatorSet.start();
                        }

                    }
                } else {
                    text.setAlpha(0f);
                    text.setTag(R.id.visible, false);
                    text.setTag(R.id.lock, false);
                    if (other != null) {
                        other.setAlpha(0f);
                    }
                    text.setImageDrawable(img);
                }

            }
        });
    }
    private class EyeGestureListener implements EyeGestureManager.Listener
    {
        @Override
        public void onEnableStateChange(EyeGesture eyeGesture, boolean paramBoolean)
        {
            Log.i(TAG, eyeGesture + " state changed:" + paramBoolean);
        }

        @Override
        public void onDetected(final EyeGesture eyeGesture)
        {

            Log.i(TAG, eyeGesture + " is detected");
            runOnUiThread(new Runnable()
            {
                @Override
                public void run()
                {
                    Console.print(eyeGesture + " is detected");

                    if (eyeGesture == EyeGesture.LOOK_AT_SCREEN)
                    {
                        WindowManager.LayoutParams wMLayout = getWindow().getAttributes();
                        wMLayout.screenBrightness = 1.0f; //Modify Brightness
                        getWindow().setAttributes(wMLayout); //Apply changes
                    }
                    else
                    {
                        WindowManager.LayoutParams wMLayout = getWindow().getAttributes();
                        wMLayout.screenBrightness = 0.0f; //Modify Brightness
                        getWindow().setAttributes(wMLayout); //Apply changes
                    }



                }
            });
        }
    }

}