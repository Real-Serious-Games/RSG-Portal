package com.rsg.rsgportal;

import android.content.Context;
import android.graphics.Typeface;

import java.util.HashMap;

/**
 * Created by adamsingle on 17/02/2016.
 * https://futurestud.io/blog/custom-fonts-on-android-extending-textview
 */
public class FontCache {
    private static HashMap<String, Typeface> fontCache = new HashMap<>();

    public static Typeface getTypeface(String fontname, Context context) {
        Typeface typeface = fontCache.get(fontname);

        if (typeface == null) {
            try {
                typeface = Typeface.createFromAsset(context.getAssets(), "fonts/" + fontname);
            } catch (Exception e) {
                return null;
            }

            fontCache.put(fontname, typeface);
        }

        return typeface;
    }
}
