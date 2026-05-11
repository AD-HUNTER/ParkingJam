package es.upm.pproject.parkingjam.view;

import java.io.BufferedReader;
import java.io.FileReader;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import com.jogamp.opengl.GL2;

/**
 * Clase de utilidad gráfica que carga archivos con formato .OBJ 
 * Permite renderizar mallas paramétricas pesadas, reemplazando 
 * potencialmente a la generación procedural.
 *
 * Funciona leyendo a mano las sentencias 'v' (vértices), 
 * 'vn' (normales) y 'f' (caras).
 */
public class ModeloOBJ {
    private static final Logger log = Logger.getLogger(ModeloOBJ.class);

    // Listas internas para alojar la geometría leída
    private final List<float[]> vertices = new ArrayList<>();
    private final List<float[]> normales = new ArrayList<>();
    private final List<int[]> caras = new ArrayList<>();

    /**
     * Construye un modelo 3D desde un fichero de disco.
     * @param archivo Ruta al fichero .obj
     */
    public ModeloOBJ(Path archivo) {
        try (BufferedReader br = new BufferedReader(new FileReader(archivo.toFile()))) {
            String linea;
            while ((linea = br.readLine()) != null) {
                String[] partes = linea.trim().split("\\s+");
                // Ignorar líneas vacías o comentarios
                if (partes.length == 0 || linea.startsWith("#")) continue;

                if (partes[0].equals("v")) {
                    // Posición de los vértices espaciales (X, Y, Z)
                    vertices.add(new float[]{
                            Float.parseFloat(partes[1]),
                            Float.parseFloat(partes[2]),
                            Float.parseFloat(partes[3])
                    });
                } else if (partes[0].equals("vn")) {
                    // Normales (direccion para reflejo de luz)
                    normales.add(new float[]{
                            Float.parseFloat(partes[1]),
                            Float.parseFloat(partes[2]),
                            Float.parseFloat(partes[3])
                    });
                } else if (partes[0].equals("f")) {
                    // Caras (f) que unen vértices: formato v/vt/vn
                    int[] cara = new int[9];
                    for (int i = 0; i < 3; i++) {
                        String[] idx = partes[i + 1].split("/");
                        // Los índices en archivos OBJ son base 1, en Java usamos base 0
                        cara[i * 3] = Integer.parseInt(idx[0]) - 1;
                        if (idx.length > 2 && !idx[2].isEmpty()) {
                            cara[i * 3 + 2] = Integer.parseInt(idx[2]) - 1;
                        } else {
                            cara[i * 3 + 2] = -1;
                        }
                    }
                    caras.add(cara);
                }
            }
        } catch (Exception e) {
            log.error("No se pudo cargar o parsear el archivo OBJ: " + archivo.toString(), e);
        }
    }

    /**
     * Emite la lista leída a la tarjeta gráfica mediante dibujo fijo.
     * @param gl Contexto OpenGL activo
     */
    public void dibujar(GL2 gl) {
        gl.glBegin(GL2.GL_TRIANGLES);
        for (int[] c : caras) {
            for (int i = 0; i < 3; i++) {
                int vnIdx = c[i * 3 + 2];
                // Si la cara tiene normal definida, avisar a OpenGL
                if (vnIdx >= 0 && vnIdx < normales.size()) {
                    float[] vn = normales.get(vnIdx);
                    gl.glNormal3f(vn[0], vn[1], vn[2]);
                }
                
                int vIdx = c[i * 3];
                // Definir el vértice exacto
                if(vIdx >= 0 && vIdx < vertices.size()) {
                    float[] v = vertices.get(vIdx);
                    gl.glVertex3f(v[0], v[1], v[2]);
                }
            }
        }
        gl.glEnd();
    }
}
