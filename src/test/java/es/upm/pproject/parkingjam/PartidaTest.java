package es.upm.pproject.parkingjam;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import es.upm.pproject.parkingjam.model.*;
import java.io.File;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;
import java.util.List;

public class PartidaTest {
	@Test
    public void testConstructor() {
        Partida partida = new Partida();
        assertNotNull(partida.getPuntuaciones());
        assertTrue(partida.getPuntuaciones().isEmpty());
        assertNotNull(partida.getMovimientos());
        assertTrue(partida.getMovimientos().isEmpty());
        assertNotNull(partida.getVehiculos());
        assertTrue(partida.getVehiculos().isEmpty());
        assertEquals(0, partida.getPuntuacionactual());
        assertNull(partida.getNivelactual());
    }

    @Test
    public void testSetYGetNivelactual() {
        Partida partida = new Partida();
        File nivel = new File("nivel1.txt");
        partida.setNivelactual(nivel);
        assertEquals(nivel, partida.getNivelactual());
    }

    @Test
    public void testSetYGetPuntuacionactual() {
        Partida partida = new Partida();
        partida.setPuntuacionactual(120);
        assertEquals(120, partida.getPuntuacionactual());
    }

    @Test
    public void testSetYGetPuntuaciones() {
        Partida partida = new Partida();
        List<Puntuaciones> lista = Arrays.asList(new Puntuaciones("Ana", 100));
        partida.setPuntuaciones(lista);
        assertEquals(lista, partida.getPuntuaciones());
    }

    @Test
    public void testSetYGetMovimientos() {
        Partida partida = new Partida();
        Deque<Vehiculo> movimientos = new ArrayDeque<>();
        Vehiculo v = new Vehiculo();
        movimientos.add(v);
        partida.setMovimientos(movimientos);
        assertEquals(movimientos, partida.getMovimientos());
        assertEquals(v, partida.getMovimientos().peek());
    }

    @Test
    public void testSetYGetVehiculos() {
        Partida partida = new Partida();
        Vehiculo v = new Vehiculo();
        List<Vehiculo> vehiculos = Arrays.asList(v);
        partida.setVehiculos(vehiculos);
        assertEquals(vehiculos, partida.getVehiculos());
        assertEquals(v, partida.getVehiculos().get(0));
    }
}
