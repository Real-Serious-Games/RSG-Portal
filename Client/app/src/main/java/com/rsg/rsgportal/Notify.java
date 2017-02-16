package com.rsg.rsgportal;

import android.support.design.widget.Snackbar;
import android.view.View;

/**
 * Created by PhilipWarren on 11/01/2016.
 */
public class Notify {

    private View currentView;

    private static Notify instance = new Notify();

    public static Notify Instance() {
        return instance;
    }

    /**
     * Each time we move to a new activity the Notify snackbar will need a currently accessible
     * view to attach to. Make sure to call this function from the activities onCreate function
     * if you want them to show up.
     * @param newView
     */
    public void SetView(View newView) {
        currentView = newView;
    }

    /**
     * If a view has been set, and exists in the current activity, this will show a snackbar
     * with the provided message.
     * @param message
     */
    public void post(String message) {
        post(message, Snackbar.LENGTH_LONG);
    }

    /**
     * If a view has been set, and exists in the current activity, this will show a snackbar
     * with the provided message.
     * @param message
     */
    public void post(String message, int length) {

        if (currentView == null) {
            return;
        }

        Snackbar.make(currentView, message, length).show();
    }
}
