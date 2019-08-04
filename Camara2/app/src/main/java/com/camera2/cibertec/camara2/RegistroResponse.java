package com.camera2.cibertec.camara2;

import com.google.gson.annotations.SerializedName;

/**
 * Created by jcollantes on 13/02/2018.
 */

public class RegistroResponse {

    @SerializedName("respuesta")
    String respuesta;
    @SerializedName("codigo_registro")
    Integer codigoRegistro;
    @SerializedName("error_texto")
    String errorTexto;

    public RegistroResponse(String respuesta, Integer codigoRegistro, String errorTexto) {
        this.respuesta = respuesta;
        this.codigoRegistro = codigoRegistro;
        this.errorTexto = errorTexto;
    }

    public String getRespuesta() {
        return respuesta;
    }

    public void setRespuesta(String respuesta) {
        this.respuesta = respuesta;
    }

    public Integer getCodigoRegistro() {
        return codigoRegistro;
    }

    public void setCodigoRegistro(Integer codigoRegistro) {
        this.codigoRegistro = codigoRegistro;
    }

    public String getErrorTexto() {
        return errorTexto;
    }

    public void setErrorTexto(String errorTexto) {
        this.errorTexto = errorTexto;
    }
}
