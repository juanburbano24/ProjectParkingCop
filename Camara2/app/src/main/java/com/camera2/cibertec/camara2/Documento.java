package com.camera2.cibertec.camara2;

import com.google.gson.annotations.SerializedName;

/**
 * Created by jcollantes on 13/02/2018.
 */

public class Documento {

    @SerializedName("codigo_registro_det")
    int codigoRegistro ;
    @SerializedName("codigo_documento")
    int codigoDocumento ;
    @SerializedName("doc_referencial")
    String docReferencial ;
    @SerializedName("codigo_entrega_asignado")
    String codigoEntregaAsignado ;

    public Documento() {
    }

    public Documento(int codigoRegistro, int codigoDocumento, String docReferencial, String codigoEntregaAsignado) {
        this.codigoRegistro = codigoRegistro;
        this.codigoDocumento = codigoDocumento;
        this.docReferencial = docReferencial;
        this.codigoEntregaAsignado = codigoEntregaAsignado;
    }

    public int getCodigoRegistro() {
        return codigoRegistro;
    }

    public void setCodigoRegistro(int codigoRegistro) {
        this.codigoRegistro = codigoRegistro;
    }

    public int getCodigoDocumento() {
        return codigoDocumento;
    }

    public void setCodigoDocumento(int codigoDocumento) {
        this.codigoDocumento = codigoDocumento;
    }

    public String getDocReferencial() {
        return docReferencial;
    }

    public void setDocReferencial(String docReferencial) {
        this.docReferencial = docReferencial;
    }

    public String getCodigoEntregaAsignado() {
        return codigoEntregaAsignado;
    }

    public void setCodigoEntregaAsignado(String codigoEntregaAsignado) {
        this.codigoEntregaAsignado = codigoEntregaAsignado;
    }
}
