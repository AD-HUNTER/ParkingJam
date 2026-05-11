package es.upm.pproject.parkingjam;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import es.upm.pproject.parkingjam.exceptions.CargarNivelException;
import es.upm.pproject.parkingjam.exceptions.FormatoNivelException;
import es.upm.pproject.parkingjam.exceptions.PartidaDAOException;

public class ExceptionTest {
	@Test
    public void testCargarNivelException() {
        Throwable causa = new NullPointerException("Causa interna");
        CargarNivelException ex = new CargarNivelException("Error al cargar nivel", causa);

        assertEquals("Error al cargar nivel", ex.getMessage());
        assertEquals(causa, ex.getCause());
    }

    @Test
    public void testFormatoNivelException() {
        FormatoNivelException ex = new FormatoNivelException("Formato inválido");

        assertEquals("Formato inválido", ex.getMessage());
        assertNull(ex.getCause());
    }

    @Test
    public void testPartidaDAOException() {
        Throwable causa = new IllegalArgumentException("Causa DAO");
        PartidaDAOException ex = new PartidaDAOException("Error DAO", causa);

        assertEquals("Error DAO", ex.getMessage());
        assertEquals(causa, ex.getCause());
    }
}
