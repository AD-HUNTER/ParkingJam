package es.upm.pproject.parkingjam;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import es.upm.pproject.parkingjam.model.Posicion;


class PosicionTest {

    @Test
    void testConstructorYGetters() {
        Posicion p = new Posicion(2, 3);
        assertEquals(2, p.getFila());
        assertEquals(3, p.getColumna());
    }

    @Test
    void testEqualsMismaReferencia() {
        Posicion p = new Posicion(1, 1);
        assertEquals(p, p);
    }

    @Test
    void testEqualsIgualdad() {
        Posicion p1 = new Posicion(4, 5);
        Posicion p2 = new Posicion(4, 5);
        assertEquals(p1, p2);
        assertEquals(p1.hashCode(), p2.hashCode());
    }

    @Test
    void testEqualsDistinto() {
        Posicion p1 = new Posicion(1, 2);
        Posicion p2 = new Posicion(2, 1);
        assertNotEquals(p1, p2);
    }

    @Test
    void testEqualsNullYClaseDistinta() {
        Posicion p = new Posicion(0, 0);
        assertNotEquals(null, p);
    }

    @Test
    void testHashCode() {
        Posicion p = new Posicion(3, 7);
        int esperado = 31 * 3 + 7;
        assertEquals(esperado, p.hashCode());
    }
}
