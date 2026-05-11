package es.upm.pproject.parkingjam.model;

import es.upm.pproject.parkingjam.exceptions.CargarNivelException;
import es.upm.pproject.parkingjam.exceptions.FormatoNivelException;
import org.apache.log4j.Logger;

import java.io.*;
import java.util.*;

public class CargarNivel {

    private static final Logger log = Logger.getLogger(CargarNivel.class);

    private CargarNivel() {
        throw new UnsupportedOperationException("Clase utilitaria");
    }

    public static Juego cargarNivel(File file) {
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String levelName = reader.readLine();
            String[] dims = reader.readLine().split(" ");
            int filas = Integer.parseInt(dims[0]);
            int columnas = Integer.parseInt(dims[1]);

            char[][] celdas = new char[filas][columnas];
            Map<Character, List<Posicion>> carMap = new HashMap<>();
            Posicion salida = null;
            int numArrobas = 0;
            int numAsteriscos = 0;

            for (int i = 0; i < filas; i++) {
                String line = reader.readLine();
                for (int j = 0; j < columnas; j++) {
                    char ch = line.charAt(j);
                    celdas[i][j] = ch;
                    if (esBorde(i, j, filas, columnas)) {
                        validarBorde(ch);
                    }
                    if (ch == '*') numAsteriscos++;
                    if (ch == '@') {
                        salida = new Posicion(i, j);
                        numArrobas++;
                    } else if (Character.isLetter(ch) || ch == '*') {
                        carMap.computeIfAbsent(ch, k -> new ArrayList<>()).add(new Posicion(i, j));
                    }
                }
            }

            validarVehiculos(carMap);
            validarFormatoFinal(numAsteriscos, numArrobas);

            List<Vehiculo> cars = new ArrayList<>();
            Vehiculo redCar = null;
            for (Map.Entry<Character, List<Posicion>> entrada : carMap.entrySet()) {
                Vehiculo car = crearVehiculo(entrada.getKey(), entrada.getValue());
                cars.add(car);
                if (entrada.getKey() == '*'){
                    redCar = car;
                    break; // Solo necesitamos el coche rojo
                }

            }

            if (salida == null || redCar == null) {
                throw new FormatoNivelException("Nivel inválido: falta salida o coche rojo");
            }

            return new Juego(levelName, new Tablero(filas, columnas, celdas, salida), cars, redCar);
        } catch (IOException e) {
            throw new CargarNivelException("Error al cargar el nivel desde el archivo: " + file.getName(), e);
        }
    }

    private static boolean esBorde(int i, int j, int filas, int columnas) {
        return i == 0 || j == 0 || i == filas - 1 || j == columnas - 1;
    }

    private static void validarBorde(char ch) {
        if (ch != '+' && ch != '@') {
            throw new FormatoNivelException("ERROR: Formato incorrecto de fichero de nivel (hay un coche donde deberia haber pared)");
        }
    }

    private static void validarVehiculos(Map<Character, List<Posicion>> carMap) {
        for (Map.Entry<Character, List<Posicion>> entry : carMap.entrySet()) {
            char vehiculo = entry.getKey();
            List<Posicion> posiciones = entry.getValue();

            Set<Integer> filasVeh = new HashSet<>();
            Set<Integer> columnasVeh = new HashSet<>();
            for (Posicion pos : posiciones) {
                filasVeh.add(pos.getFila());
                columnasVeh.add(pos.getColumna());
            }
            if (filasVeh.size() > 1 && columnasVeh.size() > 1) {
                throw new FormatoNivelException("ERROR: Formato fichero incorrecto. EL vehiculo " + vehiculo + " no sigue el formato");
            }
            if (posiciones.size() == 1) {
                throw new FormatoNivelException("ERROR: Formato fichero incorrecto. EL vehiculo " + vehiculo + " es de 1x1");
            }
            List<Integer> coords = new ArrayList<>();
            if (filasVeh.size() == 1) {
                for (Posicion pos : posiciones) coords.add(pos.getColumna());
            } else {
                for (Posicion pos : posiciones) coords.add(pos.getFila());
            }
            Collections.sort(coords);
            for (int i = 1; i < coords.size(); i++) {
                if (coords.get(i) != coords.get(i - 1) + 1) {
                    throw new FormatoNivelException("ERROR: formato de vehiculo incorrecto. Posiciones disjuntas");
                }
            }
        }
    }

    private static void validarFormatoFinal(int numAsteriscos, int numArrobas) {
        if (numAsteriscos != 2 || numArrobas != 1) {
            throw new FormatoNivelException("ERROR: El archivo nivel no es correcto");
        }
    }

    private static Vehiculo crearVehiculo(char id, List<Posicion> posiciones) {
        Orientacion o = deduceOrientacion(posiciones);
        Posicion inicio = Collections.min(posiciones, Comparator.comparingInt(p -> o == Orientacion.HORIZONTAL ? p.getColumna() : p.getFila()));
        Vehiculo car = new Vehiculo(String.valueOf(id), posiciones.size(), o, inicio.getFila(), inicio.getColumna(), "");
        switch (id) {
            case '*': car.setColor("R"); break;
            case 'a': car.setColor("B"); break;
            case 'b': car.setColor("G"); break;
            case 'c': car.setColor("Y"); break;
            case 'd': car.setColor("O"); break;
            case 'e': car.setColor("P"); break;
            case 'f': car.setColor("GR"); break;
            case 'g': car.setColor("BL"); break;
            case 'h': car.setColor("GRE"); break;
            case 'i': car.setColor("YE"); break;
            default: car.setColor("CYANDEFAULT");
        }
        return car;
    }

    private static Orientacion deduceOrientacion(List<Posicion> posiciones) {
        if (posiciones.size() < 2) return Orientacion.HORIZONTAL;
        Posicion p1 = posiciones.get(0);
        Posicion p2 = posiciones.get(1);
        return p1.getFila() == p2.getFila() ? Orientacion.HORIZONTAL : Orientacion.VERTICAL;
    }
    public static Juego cargarSiguienteNivel(File archivoActual) {
        // Obtener el número del nivel actual
        String nombreArchivo = archivoActual.getName();
        int nivelActual = Integer.parseInt(nombreArchivo.replaceAll("\\D", ""));
        int siguienteNivel = nivelActual + 1;

        // Crear el archivo del siguiente nivel
        File siguienteArchivo = new File("src/main/resources/niveles/level_" + siguienteNivel + ".txt");

        if (siguienteArchivo.exists()) {
            log.info("Cargando nivel " + siguienteNivel);
            return cargarNivel(siguienteArchivo);
        } else {
            log.info("No hay más niveles disponibles");
            return null;
        }
    }
}