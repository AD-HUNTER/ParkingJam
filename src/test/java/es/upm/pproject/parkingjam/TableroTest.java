package es.upm.pproject.parkingjam;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import es.upm.pproject.parkingjam.model.*;

class TableroTest {

    @Test
    void testConstructorYGetters() {
        char[][] celdas = {
                {'+', '+', '+'},
                {'+', '@', '+'},
                {'+', '+', '+'}
        };
        Posicion salida = new Posicion(1, 1);
        Tablero tablero = new Tablero(3, 3, celdas, salida);

        assertEquals(3, tablero.getFilas());
        assertEquals(3, tablero.getColumnas());
        assertArrayEquals(celdas, tablero.getCeldas());
        assertEquals(salida, tablero.getSalida());
    }

    @Test
    void testGetCell() {
        char[][] celdas = {
                {'A', 'B'},
                {'C', 'D'}
        };
        Tablero tablero = new Tablero(2, 2, celdas, new Posicion(0, 0));
        assertEquals('A', tablero.getCell(0, 0));
        assertEquals('D', tablero.getCell(1, 1));
    }

    @Test
    void testPrintNoLanzaExcepcion() {
        char[][] celdas = {
                {'X', 'Y'},
                {'Z', '@'}
        };
        Tablero tablero = new Tablero(2, 2, celdas, new Posicion(1, 1));
        assertDoesNotThrow(tablero::print);
    }
}
