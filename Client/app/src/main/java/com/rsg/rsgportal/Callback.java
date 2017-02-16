package com.rsg.rsgportal;

/**
 * Created by PhilipWarren on 15/01/2016.
 */
public interface Callback {

    //
    // Called when the operation completes successfully.
    //
    void success();


    //
    // Called when the operation fails.
    //
    void fail(Exception e);
}

