package es.upm.pproject.parkingjam;

import es.upm.pproject.parkingjam.model.Puntuaciones;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;


class PuntuacionesTest {

    @Test
    void testConstructorYGetters() {
        Puntuaciones p = new Puntuaciones("Nivel Inicial", 3);
        assertEquals("Nivel Inicial", p.getNombre());
        assertEquals(3, p.getPuntuacion());
    }

    @Test
    void testEqualsMismaReferencia() {
        Puntuaciones p = new Puntuaciones("Nivel Inicial", 4);
        assertEquals(p, p);
    }

    @Test
    void testEqualsPuntuacion() {
        Puntuaciones p1 = new Puntuaciones("Nivel Inicial", 5);
        Puntuaciones p2 = new Puntuaciones("Nivel 2", 5);
        assertEquals(p1.getPuntuacion(), p2.getPuntuacion());
    }

    @Test
    void testEqualsNombre() {
        Puntuaciones p1 = new Puntuaciones("Nivel Inicial", 5);
        Puntuaciones p2 = new Puntuaciones("Nivel 2", 5);
        assertNotEquals(p1.getNombre(), p2.getNombre());
    }

    @Test
    void testPuntuacionesConstructorVacio() {
        Puntuaciones p = new Puntuaciones();
        assertNull(p.getNombre());
        assertNull(p.getPuntuacion());
    }

    @Test
    void testSetters() {
        Puntuaciones p = new Puntuaciones();
        p.setNombre("Level 2");
        p.setPuntuacion(15);
        assertEquals("Level 2", p.getNombre());
        assertEquals(15, p.getPuntuacion());
    }

}
