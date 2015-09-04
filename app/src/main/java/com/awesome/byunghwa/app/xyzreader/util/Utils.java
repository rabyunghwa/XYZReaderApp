package com.awesome.byunghwa.app.xyzreader.util;

import android.graphics.Rect;
import android.support.annotation.NonNull;
import android.view.View;

import java.util.Iterator;
import java.util.Set;

public final class Utils {

    public static final String[] RADIOHEAD_BACKGROUND_URLS = {
            "https://dl.dropboxusercontent.com/u/231329/xyzreader_data/thumbs/p1.jpg",
            "https://dl.dropboxusercontent.com/u/231329/xyzreader_data/thumbs/p6.jpg",
            "https://dl.dropboxusercontent.com/u/231329/xyzreader_data/thumbs/p3.jpg",
            "https://dl.dropboxusercontent.com/u/231329/xyzreader_data/thumbs/p4.jpg",
            "https://dl.dropboxusercontent.com/u/231329/xyzreader_data/thumbs/p5.jpg",
            "https://dl.dropboxusercontent.com/u/231329/xyzreader_data/thumbs/p2.jpg",
            "https://dl.dropboxusercontent.com/u/231329/xyzreader_data/thumbs/p7.jpg",
            "https://dl.dropboxusercontent.com/u/231329/xyzreader_data/thumbs/p8.jpg",
            "https://dl.dropboxusercontent.com/u/231329/xyzreader_data/thumbs/p9.jpg",
            "https://dl.dropboxusercontent.com/u/231329/xyzreader_data/thumbs/p10.jpg",
            "https://dl.dropboxusercontent.com/u/231329/xyzreader_data/thumbs/p11.jpg"
    };

    /**
     * Returns true if {@param view} is contained within {@param container}'s bounds.
     */
    public static boolean isViewInBounds(@NonNull View container, @NonNull View view) {
        Rect containerBounds = new Rect();
        container.getHitRect(containerBounds);
        return view.getLocalVisibleRect(containerBounds);
    }

    /**
     * Returns a string representation of {@param set}. Used only for debugging purposes.
     */
    @NonNull
    public static String setToString(@NonNull Set<String> set) {
        Iterator<String> i = set.iterator();
        if (!i.hasNext()) {
            return "[]";
        }
        StringBuilder sb = new StringBuilder().append('[');
        while (true) {
            sb.append(i.next());
            if (!i.hasNext()) {
                return sb.append(']').toString();
            }
            sb.append(", ");
        }
    }

    private Utils() {
        // This utility class should not be instantiated.
    }
}