package es.upm.pproject.parkingjam;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import es.upm.pproject.parkingjam.model.Vehiculo;
import es.upm.pproject.parkingjam.model.Orientacion;

class VehiculoTest {

    @Test
    void testConstructorCompletoYGetters() {
        Vehiculo v = new Vehiculo("A", 2, Orientacion.HORIZONTAL, 1, 3, "rojo");
        assertEquals("A", v.getId());
        assertEquals(2, v.getTamano());
        assertEquals(Orientacion.HORIZONTAL, v.getOrientacion());
        assertEquals(1, v.getFila());
        assertEquals(3, v.getColumna());
        assertEquals("rojo", v.getColor());
    }

    @Test
    void testSetters() {
        Vehiculo v = new Vehiculo();
        v.setId("B");
        v.setTamano(3);
        v.setOrientacion(Orientacion.VERTICAL);
        v.setFila(4);
        v.setColumna(2);
        v.setColor("azul");
        assertEquals("B", v.getId());
        assertEquals(3, v.getTamano());
        assertEquals(Orientacion.VERTICAL, v.getOrientacion());
        assertEquals(4, v.getFila());
        assertEquals(2, v.getColumna());
        assertEquals("azul", v.getColor());
    }

    @Test
    void testConstructorCopia() {
        Vehiculo original = new Vehiculo("C", 1, Orientacion.HORIZONTAL, 0, 0, "verde");
        Vehiculo copia = new Vehiculo(original);
        assertEquals(original.getId(), copia.getId());
        assertEquals(original.getTamano(), copia.getTamano());
        assertEquals(original.getOrientacion(), copia.getOrientacion());
        assertEquals(original.getFila(), copia.getFila());
        assertEquals(original.getColumna(), copia.getColumna());
        assertEquals(original.getColor(), copia.getColor());
    }
}