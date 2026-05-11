package es.upm.pproject.parkingjam.model;


import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.io.File;
import java.util.*;


@XmlRootElement(name = "partida")
public class Partida {

    //Nivel actual
    private File nivelactual;
    // Puntuacion actual y total
    private int puntuacionactual;
    private List<Puntuaciones> puntuaciones;
    // Movimientos undo
    private Deque<Vehiculo> movimientos;
    // Vehiclos en el tablero
    private List<Vehiculo> vehiculos;

    public Partida() {
        this.puntuaciones = new ArrayList<>();
        this.movimientos = new ArrayDeque<>();
        this.vehiculos = new ArrayList<>();
    }

    @XmlElement
    public List<Vehiculo> getVehiculos() {
        return vehiculos;
    }
    public void setVehiculos(List<Vehiculo> vehiculos) {
        this.vehiculos = vehiculos;
    }

    public File getNivelactual() {
        return nivelactual;
    }

    public void setNivelactual(File nivelactual) {
        this.nivelactual = nivelactual;
    }

    public int getPuntuacionactual() {
        return puntuacionactual;
    }

    public void setPuntuacionactual(int puntuacion) {
        this.puntuacionactual = puntuacion;
    }

    @XmlElement
    public List<Puntuaciones> getPuntuaciones() {
        return puntuaciones;
    }

    public void setPuntuaciones(List<Puntuaciones> puntuaciones) {
        this.puntuaciones = puntuaciones;
    }

    @XmlElement
    public Deque<Vehiculo> getMovimientos() {
        return movimientos;
    }

    public void setMovimientos(Deque<Vehiculo> movimientos) {
        this.movimientos = movimientos;
    }

}
