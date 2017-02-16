package com.rsg.rsgportal;

/**
 * Created by adamsingle on 16/02/2016.
 */
public interface GenericProgressFeedbackCallback<T> {

        // Call this when the operation has completed successfully
        //
        void success(T data);

        //
        // Call this if the operation fails to complete
        //
        void fail(Exception ex);

        //
        // Call this with the progress of the operation. Value is expected to be
        // between 0 and 1
        //
        void progress(float progress);
}
