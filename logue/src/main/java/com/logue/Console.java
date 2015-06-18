/*
 * Copyright (c) 2015.
 * author: Ionut Damian <damian@informatik.uni-augsburg.de>
 * created: 2015/01/29
 */

package com.logue;

import android.app.Activity;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Created by Johnny on 29.01.2015.
 */
public class Console extends Thread {

    final static int NUM_LINES = 1;
    final static int TIMEOUT = 100; //in ms

    protected Activity _activity;
    protected static ArrayList<String> _buffer = new ArrayList<String>();
    String _text;

    protected static ReentrantLock _lock = new ReentrantLock();

    protected boolean _terminate = false;
    protected boolean _safeToKill = false;

    public static void print(String text)
    {
        _lock.lock();

        _buffer.add(text); //push

        if(_buffer.size() > NUM_LINES)
            _buffer.remove(0); //pop front

        _lock.unlock();
    }

    public Console(Activity act)
    {
        _activity = act;
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
                _lock.lock();

                _text = "";
                for(String s : _buffer)
                {
                    _text += "> " + s + '\n';
                }

                _lock.unlock();

                _activity.runOnUiThread(new Runnable() {
                    public void run() {
                    TextView view = (TextView) _activity.findViewById(R.id.debug);
                    view.setText(_text);
                    }
                });

                sleep(TIMEOUT);
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
        }
        _safeToKill = true;
    }
}
