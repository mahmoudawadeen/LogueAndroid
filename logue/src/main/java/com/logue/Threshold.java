/*
 * Copyright (c) 2014.
 * author: Ionut Damian <damian@informatik.uni-augsburg.de>
 * created: 2014/12/02
 */

package com.logue;

import android.graphics.drawable.Drawable;

/**
 * Created by Johnny on 01.12.2014.
 */
public class Threshold
{
    public int quality = 0; //0 = bad, 1 = good
    public float value = 0;
    public String iconName;
    public Drawable icon;

    public Threshold()
    {}

    public Threshold(Threshold other)
    {
        this.quality = other.quality;
        this.value = other.value;
        this.iconName = other.iconName;
        this.icon = other.icon;
    }
}
