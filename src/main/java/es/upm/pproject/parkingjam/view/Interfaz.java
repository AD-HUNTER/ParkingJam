package es.upm.pproject.parkingjam.view;

import es.upm.pproject.parkingjam.controller.PartidaDAO;
import es.upm.pproject.parkingjam.exceptions.PartidaDAOException;
import es.upm.pproject.parkingjam.model.*;
import com.jogamp.newt.event.WindowAdapter;
import com.jogamp.newt.event.WindowEvent;
import com.jogamp.newt.opengl.GLWindow;
import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLProfile;
import com.jogamp.opengl.util.FPSAnimator;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.io.BufferedReader;
import java.io.FileReader;

import static es.upm.pproject.parkingjam.view.PanelJuego.*;


/**
 * Controlador principal y fachada de la Vista (MVC) para el motor gráfico en 3D.
 * Se encarga de inicializar JOGL, instanciar y redibujar el GLWindow y
 * coordinar acciones de ciclo de vida solicitadas por eventos en la GPU (como
 * guardar o deshacer), orquestándolas con el Modelo de la aplicación.
 */
public class Interfaz {

    private static final Logger log = Logger.getLogger(Interfaz.class);
    public static ArrayList<Puntuaciones> puntuacionTotal = new ArrayList<>();
    private static final String RECURSOS = "resources";
    private static final String NIVELES  = "niveles";
    private static final String TITULO   = "ParkingJam Game";

    public static void setPuntuacionTotal(List<Puntuaciones> puntuaciones) {
        puntuacionTotal = new ArrayList<>(puntuaciones);
    }

    private Juego createGame() {
        Path path = Paths.get("src", "main", RECURSOS, NIVELES, "level_1.txt");
        this.archivonivelactual = path.toFile();
        return CargarNivel.cargarNivel(this.archivonivelactual);
    }

    private Juego juego;
    private final GLWindow window;
    private final FPSAnimator animator;

    // Panel central (OpenGL ocupa toda la ventana)
    private PanelJuego paneljuego;

    private File archivonivelactual;

    public Interfaz() throws IOException {
        GLProfile profile = GLProfile.get(GLProfile.GL2);
        GLCapabilities capabilities = new GLCapabilities(profile);
        this.window = GLWindow.create(capabilities);
        this.window.setSize(1280, 720);
        this.window.setTitle(TITULO);
        this.juego = createGame();
        this.paneljuego = new PanelJuego(juego, this);
        PanelJuego.hudNivelTitulo = TITULO;
        this.window.addGLEventListener(paneljuego);
        this.window.addMouseListener(paneljuego);
        this.animator = new FPSAnimator(this.window, 60, true);
        this.window.addWindowListener(new WindowAdapter() {
            @Override
            public void windowDestroyNotify(WindowEvent e) {
                if (animator.isStarted()) {
                    animator.stop();
                }
            }
        });
    }

    // ============================================================
    //  MÉTODOS DE ACCIÓN (llamados desde el HUD de OpenGL)
    // ============================================================

    public void nuevaPartida() {
        log.info("Ha iniciado una partida nueva");
        movimientos.clear();
        resetearPuntuacion();
        puntuacionTotal.clear();
        try {
            juego = createGame();
            Path path = Paths.get("src", "main", RECURSOS, NIVELES, "level_1.txt");
            archivonivelactual = path.toFile();
            actualizarTituloDesdeArchivo(archivonivelactual);
            recargarPanelJuego();
        } catch (IOException ex) {
            log.error("Error al cargar el nivel inicial: " + ex.getMessage());
        }
    }

    public void cerrarJuego() {
        log.info("Se ha cerrado el juego");
        if (animator.isStarted()) {
            animator.stop();
        }
        window.destroy();
    }

    public void deshacer() {
        log.info("Se intenta deshacer un movimiento");
        paneljuego.undo();
    }

    public void reiniciarNivel() {
        log.info("Se intenta reiniciar el nivel");
        movimientos.clear();
        reiniciarNivelPuntuacion();
        File archivoActual = this.archivonivelactual;
        if (archivoActual != null) {
            Juego juegoreiniciado = CargarNivel.cargarNivel(archivoActual);
            this.juego = juegoreiniciado;
            recargarPanelJuego();
        } else {
            log.error("No se ha encontrado el archivo del nivel actual.");
        }
    }

    public void guardarPartida() {
        log.info("Se ha intentado guardar la partida");
        Partida partida = new Partida();
        partida.setNivelactual(archivonivelactual);
        partida.setPuntuacionactual(PanelJuego.puntuacion);
        partida.setPuntuaciones(puntuacionTotal);
        partida.setMovimientos(movimientos);
        partida.setVehiculos(juego.getVehiculos());
        try {
            PartidaDAO partidaDAO = new PartidaDAO();
            partidaDAO.salvarPartida(partida);
        } catch (PartidaDAOException e) {
            log.error("Error al guardar la partida");
        }
    }

    public void cargarPartida() {
        Juego juegoguardado;
        Partida partida;
        try {
            PartidaDAO partidaDAO = new PartidaDAO();
            partida = partidaDAO.load();
            File nivelguardado = partida.getNivelactual();
            setArchivoNivelActual(nivelguardado);
            juegoguardado = CargarNivel.cargarNivel(nivelguardado);

            actualizarTituloDesdeArchivo(nivelguardado);
            List<Vehiculo> listavehiculos = partida.getVehiculos();
            juegoguardado.getVehiculos().clear();
            juegoguardado.getVehiculos().addAll(listavehiculos);
            Posicion salida = juegoguardado.getTablero().getSalida();
            char[][] celdas = juegoguardado.getTablero().getCeldas();
            for (int i = 0; i < celdas.length; i++) {
                for (int j = 0; j < celdas[i].length; j++) {
                    if (celdas[i][j] != '+' && !(i == salida.getFila() && j == salida.getColumna())) {
                        celdas[i][j] = ' ';
                    }
                }
            }
            for (Vehiculo vehiculo : listavehiculos) {
                for (int i = 0; i < vehiculo.getTamano(); i++) {
                    if (vehiculo.getOrientacion() == Orientacion.HORIZONTAL) {
                        celdas[vehiculo.getFila()][vehiculo.getColumna() + i] = vehiculo.getId().charAt(0);
                    } else {
                        celdas[vehiculo.getFila() + i][vehiculo.getColumna()] = vehiculo.getId().charAt(0);
                    }
                }
            }
        } catch (PartidaDAOException | IOException ex) {
            if (paneljuego != null) {
                paneljuego.mostrarModalInformativo(
                        "Error al cargar partida",
                        "Asegúrate de que existe una partida guardada."
                );
            }
            log.error("Error al cargar la partida: " + ex.getMessage());
            return;
        }

        this.juego = juegoguardado;
        PanelJuego.setPuntuacion(partida.getPuntuacionactual());
        PanelJuego.setMovimientos(partida.getMovimientos());
        setPuntuacionTotal(partida.getPuntuaciones());
        cargarNivelPuntuacion(partida.getPuntuacionactual(), partida.getPuntuaciones());
        recargarPanelJuego();
    }

    // ============================================================
    //  ACTUALIZACIÓN DE PUNTUACIÓN Y TÍTULO (→ campos del HUD)
    // ============================================================

    public static int puntuacionAcc = 0;

    public static void actualizarPuntuacion(int nuevaPuntuacion, int sumalista,
                                             boolean enNuevaPartida, boolean enReiniciarNivel,
                                             boolean enCargarPartida) {
        if (enNuevaPartida) {
            puntuacionAcc = 0;
        }
        if (enReiniciarNivel) {
            puntuacionAcc = puntuacionAcc - nuevaPuntuacion - 1;
            nuevaPuntuacion = 0;
        }
        if (enCargarPartida) {
            puntuacionAcc = sumalista + nuevaPuntuacion;
        }
        // Actualizar campos del HUD de OpenGL (en lugar de JLabels de Swing)
        PanelJuego.hudMovimientos       = "Movimientos: " + nuevaPuntuacion;
        PanelJuego.hudMovimientosTotales = "Movimientos Totales: " + puntuacionAcc;
        puntuacionAcc++;
    }

    public static void main(String[] args) throws IOException {
        Interfaz interfaz = new Interfaz();
        interfaz.mostrar();
    }

    public void mostrar() {
        window.setVisible(true);
        if (!animator.isStarted()) {
            animator.start();
        }
    }

    public File getArchivoNivelActual() {
        return archivonivelactual;
    }

    public static void actualizarTituloDesdeArchivo(File archivo) throws IOException {
        try (BufferedReader reader2 = new BufferedReader(new FileReader(archivo))) {
            String line = reader2.readLine();
            if (line != null) {
                PanelJuego.hudNivelTitulo = line;
            }
        } catch (IOException ex) {
            log.error("No se pudo leer la primera línea del nivel: " + ex.getMessage());
            PanelJuego.hudNivelTitulo = "Titulo de nivel desconocido";
        }
    }

    public void setJuego(Juego juego) {
        this.juego = juego;
    }

    public void setArchivoNivelActual(File archivo) {
        this.archivonivelactual = archivo;
    }

    public void recargarPanelJuego() {
        PanelJuego anterior = this.paneljuego;
        PanelJuego nuevo = new PanelJuego(juego, this);
        window.invoke(false, drawable -> {
            window.removeMouseListener(anterior);
            window.removeGLEventListener(anterior);
            window.addGLEventListener(nuevo);
            window.addMouseListener(nuevo);
            this.paneljuego = nuevo;
            return true;
        });
    }

    public static int sumaPuntuacionFinal(ArrayList<Puntuaciones> puntuacionTotal) {
        int suma = 0;
        for (Puntuaciones punt : puntuacionTotal) {
            suma += punt.getPuntuacion();
        }
        return suma;
    }

}
