package com.camera2.cibertec.camara2.rest.response;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

/**
 * Created by jcollantes on 13/02/2018.
 */

public class Result {

    @SerializedName("result")
    @Expose
    private String result;

    /**
     * @return The result
     */
    public String getResult() {
        return result;
    }

    /**
     * @param result The result
     */
    public void setResult(String result) {
        this.result = result;
    }

}