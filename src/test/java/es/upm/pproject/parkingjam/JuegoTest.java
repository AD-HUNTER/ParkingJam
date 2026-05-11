package es.upm.pproject.parkingjam;

import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import es.upm.pproject.parkingjam.model.*;

class JuegoTest {

    static class TableroMock extends Tablero {
        boolean printCalled = false;
        public TableroMock() { super(1, 1, new char[][]{{'+'}}, new Posicion(0,0)); }
        @Override
        public void print() { printCalled = true; }
    }

    @Test
    void testConstructorYGetters() {
        TableroMock tablero = new TableroMock();
        Vehiculo v1 = new Vehiculo();
        Vehiculo v2 = new Vehiculo();
        List<Vehiculo> vehiculos = List.of(v1, v2);
        Juego juego = new Juego("Nivel1", tablero, vehiculos, v1);

        assertEquals("Nivel1", juego.getNombre());
        assertEquals(tablero, juego.getTablero());
        assertEquals(vehiculos, juego.getVehiculos());
        assertEquals(v1, juego.getCocheRojo());
    }

    @Test
    void testShowLlamaPrint() {
        TableroMock tablero = new TableroMock();
        Vehiculo v = new Vehiculo();
        Juego juego = new Juego("NivelX", tablero, List.of(v), v);

        juego.show();
        assertTrue(tablero.printCalled);
    }
}
