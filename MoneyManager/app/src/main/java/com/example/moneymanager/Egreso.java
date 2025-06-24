package com.example.moneymanager;

import java.util.Date;

public class Egreso {
    private String id;
    private String userId;
    private String titulo;
    private double monto;
    private String descripcion;
    private Date fecha;
    private String comprobanteUrl;

    public Egreso() {
        // Constructor vacío requerido por Firestore
    }

    public Egreso(String userId, String titulo, double monto, String descripcion, Date fecha) {
        this.userId = userId;
        this.titulo = titulo;
        this.monto = monto;
        this.descripcion = descripcion;
        this.fecha = fecha;
        this.comprobanteUrl = null; // Por defecto sin comprobante
    }

    // Nuevo constructor para incluir el comprobanteUrl
    public Egreso(String userId, String titulo, double monto, String descripcion, Date fecha, String comprobanteUrl) {
        this.userId = userId;
        this.titulo = titulo;
        this.monto = monto;
        this.descripcion = descripcion;
        this.fecha = fecha;
        this.comprobanteUrl = comprobanteUrl;
    }

    // Getters y Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getTitulo() {
        return titulo;
    }

    public void setTitulo(String titulo) {
        this.titulo = titulo;
    }

    public double getMonto() {
        return monto;
    }

    public void setMonto(double monto) {
        this.monto = monto;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public Date getFecha() {
        return fecha;
    }

    public void setFecha(Date fecha) {
        this.fecha = fecha;
    }

    // Getter y Setter para comprobanteUrl
    public String getComprobanteUrl() {
        return comprobanteUrl;
    }

    public void setComprobanteUrl(String comprobanteUrl) {
        this.comprobanteUrl = comprobanteUrl;
    }
}