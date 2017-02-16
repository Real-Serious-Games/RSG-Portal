package com.rsg.rsgportal;

/**
 * This class is passed to another function as a callback.
 *
 * Created by PhilipWarren on 15/01/2016.
 */
public interface GenericCallback<T> {

    //
    // Called when the operation completes successfully.
    //
    void success(T data);


    //
    // Called when the operation fails.
    //
    void fail(Exception e);
}
