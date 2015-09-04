package com.awesome.byunghwa.app.xyzreader.application;

import android.app.Application;

/**
 * Created by ByungHwa on 7/3/2015.
 */
public class MyApplication extends Application {

    public static MyApplication instance;

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
    }
}
