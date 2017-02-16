package com.rsg.rsgportal;

import android.content.Context;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.widget.TextView;

/**
 * Created by adamsingle on 17/02/2016.
 * https://futurestud.io/blog/custom-fonts-on-android-extending-textview
 */
public class IconTextView extends TextView {

    public IconTextView(Context context) {
        super(context);

        applyCustomFont(context);
    }

    public IconTextView(Context context, AttributeSet attrs) {
        super(context, attrs);

        applyCustomFont(context);
    }

    public IconTextView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        applyCustomFont(context);
    }

    private void applyCustomFont(Context context) {
        Typeface customFont = FontCache.getTypeface("fontawesome-webfont.ttf", context);
        setTypeface(customFont);
    }
}
