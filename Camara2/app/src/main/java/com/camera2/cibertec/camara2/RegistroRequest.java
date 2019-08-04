package com.camera2.cibertec.camara2;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * Created by jcollantes on 13/02/2018.
 */

public class RegistroRequest extends  Registro{
    @SerializedName("lista_documento")
    private List<Documento> lista_documento;
    @SerializedName("login")
    private String login;
    @SerializedName("pwd")
    private String password;
    @SerializedName("imei")
    private String imei;


    public RegistroRequest() {
    }

    public List<Documento> getLista_documento() {
        return lista_documento;
    }

    public void setLista_documento(List<Documento> lista_documento) {
        this.lista_documento = lista_documento;
    }

    public String getLogin() {
        return login;
    }

    public void setLogin(String login) {
        this.login = login;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getImei() {
        return imei;
    }

    public void setImei(String imei) {
        this.imei = imei;
    }
}
