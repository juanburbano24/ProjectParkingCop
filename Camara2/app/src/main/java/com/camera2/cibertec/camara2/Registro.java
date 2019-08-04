package com.camera2.cibertec.camara2;

import com.google.gson.annotations.SerializedName;

/**
 * Created by jcollantes on 13/02/2018.
 */

public class Registro {
    @SerializedName("codigo_registro")
    Integer codigoRegistro ;
    @SerializedName("solicitud_ano")
    String solicitudAno ;
    @SerializedName("solicitud_nro")
    Integer solicitudNro;
    @SerializedName("registro_tipo")
    String registroTipo;
    @SerializedName("registro_obs")
    String registroObs ;
    @SerializedName("registro_fecha")
    String registroFecha;
    @SerializedName("usuario_registro")
    Integer usuarioRegistro ;
    @SerializedName("latitud")
    String latitud ;
    @SerializedName("longitud")
    String longitud ;
    @SerializedName("entrega_lugar")
    String entregaLugar ;
    @SerializedName("entrega_recibido_por")
    String entregaRecibidoPor ;
    @SerializedName("motivo_tipo")
    String motivoTipo ;
    @SerializedName("ubicacion")
    String ubicacion;
    @SerializedName("entrega_tipo")
    String entregaTipo;
    @SerializedName("dni")
    String dni;
    @SerializedName("ruta_destino_latitud")
    String rutaDestinoLatitud;
    @SerializedName("ruta_destino_longitud")
    String rutaDestinoLongitud;
    @SerializedName("ruta_tiempo_texto")
    String rutaTiempoTexto;
    @SerializedName("ruta_tiempo_segundos")
    Integer rutaTiempoSegundos;
    @SerializedName("ruta_distancia_texto")
    String rutaDistanciaTexto;
    @SerializedName("ruta_distancia_metros")
    Integer rutaDistanciaMetros;


    public Registro() {
    }

    public Registro(Integer codigoRegistro, String solicitudAno, Integer solicitudNro, String registroTipo, String registroObs, String registroFecha, Integer usuarioRegistro, String latitud, String longitud, String entregaLugar, String entregaRecibidoPor, String motivoTipo, String ubicacion, String entregaTipo, String dni, String rutaDestinoLatitud, String rutaDestinoLongitud, String rutaTiempoTexto, Integer rutaTiempoSegundos, String rutaDistanciaTexto, Integer rutaDistanciaMetros) {
        this.codigoRegistro = codigoRegistro;
        this.solicitudAno = solicitudAno;
        this.solicitudNro = solicitudNro;
        this.registroTipo = registroTipo;
        this.registroObs = registroObs;
        this.registroFecha = registroFecha;
        this.usuarioRegistro = usuarioRegistro;
        this.latitud = latitud;
        this.longitud = longitud;
        this.entregaLugar = entregaLugar;
        this.entregaRecibidoPor = entregaRecibidoPor;
        this.motivoTipo = motivoTipo;
        this.ubicacion = ubicacion;
        this.entregaTipo = entregaTipo;
        this.dni = dni;
        this.rutaDestinoLatitud = rutaDestinoLatitud;
        this.rutaDestinoLongitud = rutaDestinoLongitud;
        this.rutaTiempoTexto = rutaTiempoTexto;
        this.rutaTiempoSegundos = rutaTiempoSegundos;
        this.rutaDistanciaTexto = rutaDistanciaTexto;
        this.rutaDistanciaMetros = rutaDistanciaMetros;
    }

    public Integer getCodigoRegistro() {
        return codigoRegistro;
    }

    public void setCodigoRegistro(Integer codigoRegistro) {
        this.codigoRegistro = codigoRegistro;
    }

    public String getSolicitudAno() {
        return solicitudAno;
    }

    public void setSolicitudAno(String solicitudAno) {
        this.solicitudAno = solicitudAno;
    }

    public Integer getSolicitudNro() {
        return solicitudNro;
    }

    public void setSolicitudNro(Integer solicitudNro) {
        this.solicitudNro = solicitudNro;
    }

    public String getRegistroTipo() {
        return registroTipo;
    }

    public void setRegistroTipo(String registroTipo) {
        this.registroTipo = registroTipo;
    }

    public String getRegistroObs() {
        return registroObs;
    }

    public void setRegistroObs(String registroObs) {
        this.registroObs = registroObs;
    }

    public String getRegistroFecha() {
        return registroFecha;
    }

    public void setRegistroFecha(String registroFecha) {
        this.registroFecha = registroFecha;
    }

    public Integer getUsuarioRegistro() {
        return usuarioRegistro;
    }

    public void setUsuarioRegistro(Integer usuarioRegistro) {
        this.usuarioRegistro = usuarioRegistro;
    }

    public String getLatitud() {
        return latitud;
    }

    public void setLatitud(String latitud) {
        this.latitud = latitud;
    }

    public String getLongitud() {
        return longitud;
    }

    public void setLongitud(String longitud) {
        this.longitud = longitud;
    }

    public String getEntregaLugar() {
        return entregaLugar;
    }

    public void setEntregaLugar(String entregaLugar) {
        this.entregaLugar = entregaLugar;
    }

    public String getEntregaRecibidoPor() {
        return entregaRecibidoPor;
    }

    public void setEntregaRecibidoPor(String entregaRecibidoPor) {
        this.entregaRecibidoPor = entregaRecibidoPor;
    }

    public String getMotivoTipo() {
        return motivoTipo;
    }

    public void setMotivoTipo(String motivoTipo) {
        this.motivoTipo = motivoTipo;
    }

    public String getUbicacion() {
        return ubicacion;
    }

    public void setUbicacion(String ubicacion) {
        this.ubicacion = ubicacion;
    }

    public String getEntregaTipo() {
        return entregaTipo;
    }

    public void setEntregaTipo(String entregaTipo) {
        this.entregaTipo = entregaTipo;
    }

    public String getDni() {
        return dni;
    }

    public void setDni(String dni) {
        this.dni = dni;
    }


    public String getRutaDestinoLatitud() {
        return rutaDestinoLatitud;
    }

    public void setRutaDestinoLatitud(String rutaDestinoLatitud) {
        this.rutaDestinoLatitud = rutaDestinoLatitud;
    }

    public String getRutaDestinoLongitud() {
        return rutaDestinoLongitud;
    }

    public void setRutaDestinoLongitud(String rutaDestinoLongitud) {
        this.rutaDestinoLongitud = rutaDestinoLongitud;
    }

    public String getRutaTiempoTexto() {
        return rutaTiempoTexto;
    }

    public void setRutaTiempoTexto(String rutaTiempoTexto) {
        this.rutaTiempoTexto = rutaTiempoTexto;
    }

    public Integer getRutaTiempoSegundos() {
        return rutaTiempoSegundos;
    }

    public void setRutaTiempoSegundos(Integer rutaTiempoSegundos) {
        this.rutaTiempoSegundos = rutaTiempoSegundos;
    }

    public String getRutaDistanciaTexto() {
        return rutaDistanciaTexto;
    }

    public void setRutaDistanciaTexto(String rutaDistanciaTexto) {
        this.rutaDistanciaTexto = rutaDistanciaTexto;
    }

    public Integer getRutaDistanciaMetros() {
        return rutaDistanciaMetros;
    }

    public void setRutaDistanciaMetros(Integer rutaDistanciaMetros) {
        this.rutaDistanciaMetros = rutaDistanciaMetros;
    }
}
