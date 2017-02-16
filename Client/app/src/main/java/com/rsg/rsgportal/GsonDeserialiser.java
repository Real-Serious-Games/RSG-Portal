package com.rsg.rsgportal;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

/**
 * Created by adamsingle on 17/02/2016.
 */
public class GsonDeserialiser {
    /**
     * Wrapper function to allow mocking
     * @param json
     * @param classOfT
     * @param <T>
     * @return
     * @throws JsonSyntaxException
     */
    public <T> T fromJson(String json, Class<T> classOfT) throws JsonSyntaxException {
        return new Gson().fromJson(json, classOfT);
    }
}
