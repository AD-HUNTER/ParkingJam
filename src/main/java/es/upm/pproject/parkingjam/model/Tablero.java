package es.upm.pproject.parkingjam.model;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;

public class Tablero implements Serializable {
    private final int filas;
    private final int columnas;
    private final char[][] celdas;
    private final Posicion salida;
    private static final Logger logger = LoggerFactory.getLogger(Tablero.class);

    public Tablero(int filas, int columnas, char[][] celdas, Posicion salida) {
        this.filas = filas;
        this.columnas = columnas;
        this.celdas = celdas;
        this.salida = salida;
    }

    public char[][] getCeldas() {
        return celdas;
    }

    public char getCell(int fila, int columna) {
        return celdas[fila][columna];
    }


    public int getFilas() { return filas; }
    public int getColumnas() { return columnas; }
    public Posicion getSalida() { return salida; }

    public void print() {
        StringBuilder boardStr = new StringBuilder();
        for (char[] fila : celdas) {
            for (char c : fila) {
                boardStr.append(c);
            }
            boardStr.append("\n");
        }
        logger.info("\n{}", boardStr);
    }
}
