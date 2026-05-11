package es.upm.pproject.parkingjam.model;
import java.io.Serializable;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Juego  implements Serializable {
        private static final Logger logger = LoggerFactory.getLogger(Juego.class);
        private final String nombre;
        private final Tablero tablero;
        private final List<Vehiculo> vehiculos;
        private final Vehiculo cocheRojo;

        public Juego(String nombre, Tablero tablero, List<Vehiculo> vehiculos, Vehiculo cocheRojo) {
            this.nombre = nombre;
            this.tablero = tablero;
            this.vehiculos = vehiculos;
            this.cocheRojo = cocheRojo;
        }

    public String getNombre() {
        return nombre;
    }

    public Tablero getTablero() {
        return tablero;
    }

    public List<Vehiculo> getVehiculos() {
        return vehiculos;
    }

    public Vehiculo getCocheRojo() {
        return cocheRojo;
    }

    public void show() {
            logger.info("Nivel: {}", nombre);
            tablero.print();
        }
}