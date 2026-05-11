package es.upm.pproject.parkingjam;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import es.upm.pproject.parkingjam.model.*;
import java.io.File;


class CargarNivelTest {

    private Juego juego;

    @BeforeEach
    void lecturaNivel(){
        File archivoNivel1 = new File("src/main/resources/niveles/level_1.txt");
        juego = CargarNivel.cargarNivel(archivoNivel1);
    }

    @Test
    void testJuegoNoEsNull(){
        assertNotNull(juego);
    }

    @Test
    void testNombreDelNivelCorrecto(){
        assertEquals("Nivel Inicial", juego.getNombre());
    }

    @Test
    void testTableroNoEsNull(){
        assertNotNull(juego.getTablero());
    }

    @Test
    void testVehiculosNotNull(){
        assertNotNull(juego.getVehiculos());
    }

    @Test
    void testVehiculosNoVacio(){
        assertFalse(juego.getVehiculos().isEmpty());
    }

    @Test
    void testContainsCocheRojo(){
        assertTrue(juego.getVehiculos().stream().anyMatch(v -> v.getId().equals("*")), "Falta coche rojo identificado con *");
    }

    @Test
    void testHaySalida(){
        assertNotNull(juego.getTablero().getSalida(), "El tablero necesita salida con id @");
    }

    @Test
    void testHaySoloUnaSalida(){
        char[][] celdas = juego.getTablero().getCeldas();
        int contadorSalidas = 0;
        int contadorCocheRojo = 0;
        for(char[] fila: celdas){
            for(char celda : fila){
                if(celda == '@'){
                    contadorSalidas++;
                }
                if(celda == '*'){
                    contadorCocheRojo++;
                }
            }
        }

        assertEquals(1,contadorSalidas, "El tablero tiene mas de una salida");
        assertEquals(2,contadorCocheRojo, "El coche rojo no es de tamaño adecuado o no esta");
    }

    @Test
    void testHayIDCocheEnPared(){
        boolean hayIdCocheEnPared = false;
        char[][] celdas = juego.getTablero().getCeldas();
        char ch = 0;
        for (int i = 0; i < juego.getTablero().getFilas(); i++) {
            for (int j = 0; j < juego.getTablero().getColumnas(); j++) {
                celdas[i][j] = ch;
                if ((i == 0 || j == 0 || i == juego.getTablero().getFilas() - 1 || j == juego.getTablero().getColumnas() - 1) && (ch != '+' && ch != '@')) {
                    hayIdCocheEnPared = true;
                    break;
                }
            }
        }
        assertTrue(hayIdCocheEnPared, "Hay un id de coche donde deberia haber una pared");
    }
}
