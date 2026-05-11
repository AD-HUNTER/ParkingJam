package es.upm.pproject.parkingjam.model;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;
import java.io.Serializable;

@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class Vehiculo implements Serializable {
    String id;
    int tamano;
    Orientacion orientacion;
    int fila;
    int columna;
    String color;

    public Vehiculo() {}

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public Vehiculo(String id, int tamano, Orientacion orientacion, int fila, int columna, String color) {
        this.id = id;
        this.tamano = tamano;
        this.orientacion = orientacion;
        this.fila = fila;
        this.columna = columna;
        this.color = color;
    }
    
    public Vehiculo(Vehiculo v) {
    	this.id = v.id;
        this.tamano = v.tamano;
        this.orientacion = v.orientacion;
        this.fila = v.fila;
        this.columna = v.columna;
        this.color = v.color;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public int getTamano() {
        return tamano;
    }

    public void setTamano(int tamano) {
        this.tamano = tamano;
    }

    public Orientacion getOrientacion() {
        return orientacion;
    }

    public void setOrientacion(Orientacion orientacion) {
        this.orientacion = orientacion;
    }

    public int getFila() {
        return fila;
    }

    public void setFila(int fila) {
        this.fila = fila;
    }

    public int getColumna() {
        return columna;
    }

    public void setColumna(int columna) {
        this.columna = columna;
    }
}
