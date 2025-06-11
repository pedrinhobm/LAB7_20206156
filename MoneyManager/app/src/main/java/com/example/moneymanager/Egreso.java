package com.example.moneymanager;

import com.google.firebase.firestore.Exclude;
import com.google.firebase.firestore.ServerTimestamp;

import java.util.Date;

public class Egreso {
    private String id; // Usaremos este ID localmente
    private String userId; // Para identificar a qué usuario pertenece el egreso
    private String titulo;
    private double monto;
    private String descripcion;
    private Date fecha;

    public Egreso() {
        // Constructor vacío requerido por Firebase
    }

    public Egreso(String userId, String titulo, double monto, String descripcion, Date fecha) {
        this.userId = userId;
        this.titulo = titulo;
        this.monto = monto;
        this.descripcion = descripcion;
        this.fecha = fecha;
    }

    // Getters
    @Exclude
    public String getId() {
        return id;
    }

    public String getUserId() {
        return userId;
    }

    public String getTitulo() {
        return titulo;
    }

    public double getMonto() {
        return monto;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public Date getFecha() {
        return fecha;
    }

    // Setters
    public void setId(String id) {
        this.id = id;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public void setTitulo(String titulo) {
        this.titulo = titulo;
    }

    public void setMonto(double monto) {
        this.monto = monto;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public void setFecha(Date fecha) {
        this.fecha = fecha;
    }
}