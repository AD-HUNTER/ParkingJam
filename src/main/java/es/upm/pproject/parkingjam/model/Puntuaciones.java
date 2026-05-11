package es.upm.pproject.parkingjam.model;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "puntuaciones")
public class Puntuaciones {
    private String nombre;
    private Integer puntuacion;

    public Puntuaciones() {
    }

    public Puntuaciones(String nombre, Integer puntuacion) {
        this.nombre = nombre;
        this.puntuacion = puntuacion;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public Integer getPuntuacion() {
        return puntuacion;
    }

    public void setPuntuacion(Integer puntuacion) {
        this.puntuacion = puntuacion;
    }
}
