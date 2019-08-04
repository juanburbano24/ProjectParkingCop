package com.camera2.cibertec.camara2.rest;



import com.camera2.cibertec.camara2.RegistroRequest;
import com.camera2.cibertec.camara2.RegistroResponse;

import java.util.HashMap;

import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;

/**
 * Created by jcollantes on 13/02/2018.
 */

public interface RegistroRestService {

    @POST("SrvDistribucion.svc/srestRegistrarEnviosLite")
    Call<HashMap<String, RegistroResponse>> postRegistroEnvio(@Body RegistroRequest valor);

    //envio de imagenes
    @Multipart
    @POST("SrvDistribucion.svc/PostImage")
    Call<HashMap<String, RegistroResponse>> uploadImage(@Part("description") RequestBody description, @Part MultipartBody.Part file);
    //Call<HashMap<String, RegistroResponse>> uploadImage(@Part MultipartBody.Part file);

}