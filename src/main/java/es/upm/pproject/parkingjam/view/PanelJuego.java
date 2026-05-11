package es.upm.pproject.parkingjam.view;
import com.jogamp.opengl.*;
import com.jogamp.opengl.util.texture.Texture;
import com.jogamp.opengl.util.texture.TextureIO;
import com.jogamp.opengl.util.gl2.GLUT;
import com.jogamp.newt.event.MouseEvent;
import com.jogamp.newt.event.MouseListener;
import java.awt.*;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;
import org.apache.log4j.Logger;
import es.upm.pproject.parkingjam.model.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
/**
 * Motor gráfico principal del juego.
 * Gestiona el contexto OpenGL, el pipeline fijo y programable (Shaders),
 * la creación y dibujado procedural de la escena, raycasting de interacción
 * lógica, y el dibujado de un HUD en 2D superpuesto.
 */
public class PanelJuego implements GLEventListener, MouseListener {
    private static final int TAMDECUADRADOS = 70;
    private static final Logger log = Logger.getLogger(PanelJuego.class);
    // Variables de Texturas
    private Texture texturaSuelo;
    private Texture texturaSkybox;
    // Variables de Cámara
    private float rotCamX = 45.0f;
    private float rotCamY = -20.0f;
    private float zoomCam = -25.0f;
    private Point lastMousePosCam;
    private List<Particula> particulas = new java.util.ArrayList<>();
    public static Deque<Vehiculo> movimientos = new ArrayDeque<Vehiculo>();
    public static int puntuacion = 0;
    private String nombreNivel;
    private final Tablero tablero;
    private final List<Vehiculo> vehiculos;
    private Vehiculo vehiculoseleccionado;
    private Point dondeclick;
    private boolean completado = false;
    // HUD OpenGL
    private GLUT glut;
    private Interfaz interfazRef;
    public static String hudMovimientos = "Movimientos: 0";
    public static String hudMovimientosTotales = "Movimientos Totales: 0";
    public static String hudNivelTitulo = "Parking Jam";
    private static final int HUD_BOT_PX = 92;
    private static final int HUD_TOP_PX = 52;
    private static final int BTN_W = 185;
    private static final int BTN_H = 52;
    private static final int BTN_Y_OGL = 20;
    private static final int HUD_FONT = GLUT.BITMAP_HELVETICA_18;
    private static final float[] SUN_DIR = { -0.35f, 0.88f, -0.32f };
    private static final int MODAL_TITLE_FONT = GLUT.BITMAP_HELVETICA_18;
    private static final int MODAL_TEXT_FONT = GLUT.BITMAP_HELVETICA_18;
    private static final int MODAL_BUTTON_FONT = GLUT.BITMAP_HELVETICA_18;
    private static final String[] BTN_LABELS = {"Nueva Partida", "Cargar Partida", "Guardar", "Deshacer", "Reiniciar", "Salir"};
    private int[] btnX = new int[6];
    private int viewWidth = 1280;
    private int viewHeight = 720;
    private boolean modalActivo = false;
    private String modalTitulo = "";
    private String[] modalLineas = new String[0];
    private String modalBotonTexto = "Aceptar";
    private Runnable modalAccionConfirmar;
    private int modalX, modalY, modalW, modalH;
    private int modalBtnX, modalBtnY, modalBtnW, modalBtnH;

    // Variables para el Pipeline Programable (Shaders)
    private int shaderProgram;
    private int useTextureLoc;
    private int lightDirLoc;
    private int isShadowLoc;

    // Identificadores para VBOs
    private int[] vboCube = new int[2]; // 0: Vértices/Texcoords, 1: Normales
    private static final int CUBE_VERTEX_COUNT = 24;

    public static void resetearPuntuacion() {
        puntuacion = 0;
        Interfaz.actualizarPuntuacion(puntuacion, 0, true, false, false);
    }
    public static void reiniciarNivelPuntuacion() {
        Interfaz.actualizarPuntuacion(puntuacion, 0, false, true, false);
        puntuacion = 0;
    }
    public static void undoNivelPuntuacion() {
        if (puntuacion > 0) {
            Interfaz.actualizarPuntuacion(--puntuacion, 0, false, false, false);
        }
        Interfaz.actualizarPuntuacion(puntuacion, 0, false, false, false);
    }
    public static void cargarNivelPuntuacion(int puntuacionActual, List<Puntuaciones> listaPunts) {
        int sumaLista = 0;
        for (int i = 0; i < listaPunts.size(); i++) {
            sumaLista += listaPunts.get(i).getPuntuacion();
        }
        Interfaz.actualizarPuntuacion(puntuacionActual, sumaLista, false, false, true);
    }
    public static void incrementarPuntuacion() { puntuacion++; }
    public static void setPuntuacion(int valor) { puntuacion = valor; }
    public static void setMovimientos(Deque<Vehiculo> nuevosMovimientos) { movimientos = nuevosMovimientos; }

    public PanelJuego(Juego juego, Interfaz interfaz) {
        this.tablero   = juego.getTablero();
        this.vehiculos = juego.getVehiculos();
        this.nombreNivel = juego.getNombre();
        this.interfazRef = interfaz;
    }

    private void initShaders(GL2 gl) {
        // Vertex Shader: Pasa datos al Fragment Shader para iluminación por píxel (Per-pixel lighting)
        String vsrc = "#version 120\n" +
            "varying vec4 vColor;\n" +
            "varying vec3 vNormal;\n" +
            "varying vec3 vViewPos;\n" +
            "void main() {\n" +
            "    vColor = gl_Color;\n" +
            "    vNormal = normalize(gl_NormalMatrix * gl_Normal);\n" +
            "    vec4 viewPos = gl_ModelViewMatrix * gl_Vertex;\n" +
            "    vViewPos = viewPos.xyz;\n" +
            "    gl_Position = gl_ProjectionMatrix * viewPos;\n" +
            "    gl_TexCoord[0] = gl_MultiTexCoord0;\n" +
            "}\n";

        // Fragment Shader: Calcula la iluminación (Ambient + Diffuse + Specular) píxel a píxel
        String fsrc = "#version 120\n" +
            "varying vec4 vColor;\n" +
            "varying vec3 vNormal;\n" +
            "varying vec3 vViewPos;\n" +
            "uniform sampler2D sampler;\n" +
            "uniform int useTexture;\n" +
            "uniform vec3 lightDir;\n" +
            "uniform int isShadow;\n" +
            "void main() {\n" +
            "    if (isShadow == 1) {\n" +
            "        gl_FragColor = vec4(0.08, 0.08, 0.1, 0.45);\n" +
            "        return;\n" +
            "    }\n" +
            "    vec3 norm = normalize(vNormal);\n" +
            "    vec3 lDir = normalize(gl_NormalMatrix * lightDir);\n" + // Luz a espacio de cámara
            "    float diff = max(dot(norm, lDir), 0.0);\n" +
            "    vec4 texColor = (useTexture == 1) ? texture2D(sampler, gl_TexCoord[0].st) : vec4(1.0);\n" +
            "    \n" +
            "    // Specular (Blinn-Phong)\n" +
            "    vec3 viewDir = normalize(-vViewPos);\n" +
            "    vec3 halfDir = normalize(lDir + viewDir);\n" +
            "    float spec = pow(max(dot(norm, halfDir), 0.0), 16.0);\n" +
            "    \n" +
            "    vec3 ambient = vColor.rgb * texColor.rgb * 0.45;\n" +
            "    vec3 diffuse = vColor.rgb * texColor.rgb * diff * 0.75;\n" +
            "    vec3 specular = vec3(0.2) * spec * ((diff > 0.0) ? 1.0 : 0.0);\n" +
            "    \n" +
            "    gl_FragColor = vec4(ambient + diffuse + specular, vColor.a * texColor.a);\n" +
            "}\n";

        int vs = compileShader(gl, GL2.GL_VERTEX_SHADER, vsrc);
        int fs = compileShader(gl, GL2.GL_FRAGMENT_SHADER, fsrc);
        
        shaderProgram = gl.glCreateProgram();
        gl.glAttachShader(shaderProgram, vs);
        gl.glAttachShader(shaderProgram, fs);
        gl.glLinkProgram(shaderProgram);
        
        useTextureLoc = gl.glGetUniformLocation(shaderProgram, "useTexture");
        lightDirLoc = gl.glGetUniformLocation(shaderProgram, "lightDir");
        isShadowLoc = gl.glGetUniformLocation(shaderProgram, "isShadow");
    }

    private void initVBOs(GL2 gl) {
        // Datos del cubo (XYZ, Normales)
        float[] cubeVertices = {
            // Front (Z+)
            -0.5f, -0.5f,  0.5f,   0.5f, -0.5f,  0.5f,   0.5f,  0.5f,  0.5f,  -0.5f,  0.5f,  0.5f,
            // Back (Z-)
            -0.5f, -0.5f, -0.5f,  -0.5f,  0.5f, -0.5f,   0.5f,  0.5f, -0.5f,   0.5f, -0.5f, -0.5f,
            // Bottom (Y-)
            -0.5f, -0.5f, -0.5f,  -0.5f, -0.5f,  0.5f,   0.5f, -0.5f,  0.5f,   0.5f, -0.5f, -0.5f,
            // Top (Y+)
            -0.5f,  0.5f, -0.5f,   0.5f,  0.5f, -0.5f,   0.5f,  0.5f,  0.5f,  -0.5f,  0.5f,  0.5f,
            // Right (X+)
             0.5f, -0.5f, -0.5f,   0.5f,  0.5f, -0.5f,   0.5f,  0.5f,  0.5f,   0.5f, -0.5f,  0.5f,
            // Left (X-)
            -0.5f, -0.5f, -0.5f,  -0.5f, -0.5f,  0.5f,  -0.5f,  0.5f,  0.5f,  -0.5f,  0.5f, -0.5f
        };

        float[] cubeNormals = {
            // Front
             0.0f, 0.0f, 1.0f,   0.0f, 0.0f, 1.0f,   0.0f, 0.0f, 1.0f,   0.0f, 0.0f, 1.0f,
            // Back
             0.0f, 0.0f,-1.0f,   0.0f, 0.0f,-1.0f,   0.0f, 0.0f,-1.0f,   0.0f, 0.0f,-1.0f,
            // Bottom
             0.0f,-1.0f, 0.0f,   0.0f,-1.0f, 0.0f,   0.0f,-1.0f, 0.0f,   0.0f,-1.0f, 0.0f,
            // Top
             0.0f, 1.0f, 0.0f,   0.0f, 1.0f, 0.0f,   0.0f, 1.0f, 0.0f,   0.0f, 1.0f, 0.0f,
            // Right
             1.0f, 0.0f, 0.0f,   1.0f, 0.0f, 0.0f,   1.0f, 0.0f, 0.0f,   1.0f, 0.0f, 0.0f,
            // Left
            -1.0f, 0.0f, 0.0f,  -1.0f, 0.0f, 0.0f,  -1.0f, 0.0f, 0.0f,  -1.0f, 0.0f, 0.0f
        };

        gl.glGenBuffers(2, vboCube, 0);

        // Subir vértices
        gl.glBindBuffer(GL2.GL_ARRAY_BUFFER, vboCube[0]);
        // Se multiplican los 72 floats por 4 bytes (Float.BYTES) para el tamaño en tarjeta
        java.nio.FloatBuffer vertBuf = com.jogamp.common.nio.Buffers.newDirectFloatBuffer(cubeVertices);
        gl.glBufferData(GL2.GL_ARRAY_BUFFER, cubeVertices.length * 4L, vertBuf, GL2.GL_STATIC_DRAW);

        // Subir normales
        gl.glBindBuffer(GL2.GL_ARRAY_BUFFER, vboCube[1]);
        java.nio.FloatBuffer normBuf = com.jogamp.common.nio.Buffers.newDirectFloatBuffer(cubeNormals);
        gl.glBufferData(GL2.GL_ARRAY_BUFFER, cubeNormals.length * 4L, normBuf, GL2.GL_STATIC_DRAW);

        gl.glBindBuffer(GL2.GL_ARRAY_BUFFER, 0);
    }

    private int compileShader(GL2 gl, int type, String source) {
        int shader = gl.glCreateShader(type);
        gl.glShaderSource(shader, 1, new String[]{source}, null);
        gl.glCompileShader(shader);
        return shader;
    }

    @Override
    public void init(GLAutoDrawable drawable) {
        GL2 gl = drawable.getGL().getGL2();
        gl.glClearColor(0.4f, 0.65f, 0.9f, 1.0f); // Azul cielo como fallback
        gl.glEnable(GL.GL_DEPTH_TEST);
        glut = new GLUT();
        // Carga de Texturas
        try {
            File fileSuelo = new File("src/main/resources/assets/ithappy/Cartoon_City_Free/Textures/Tiles_2.png");
            if (fileSuelo.exists()) {
                texturaSuelo = TextureIO.newTexture(fileSuelo, true);
                texturaSuelo.setTexParameteri(gl, GL2.GL_TEXTURE_WRAP_S, GL2.GL_REPEAT);
                texturaSuelo.setTexParameteri(gl, GL2.GL_TEXTURE_WRAP_T, GL2.GL_REPEAT);
                texturaSuelo.setTexParameteri(gl, GL2.GL_TEXTURE_MAG_FILTER, GL2.GL_LINEAR);
                texturaSuelo.setTexParameteri(gl, GL2.GL_TEXTURE_MIN_FILTER, GL2.GL_LINEAR_MIPMAP_LINEAR);
            }
            // Ruta corregida: la textura está dentro de Skybox_Textures/
            File skyboxImg = new File("src/main/resources/assets/ithappy/Cartoon_City_Free/Skyboxes/Skybox_Textures/Skybox_5.png");
            if (skyboxImg.exists()) {
                texturaSkybox = TextureIO.newTexture(skyboxImg, true);
                texturaSkybox.setTexParameteri(gl, GL2.GL_TEXTURE_WRAP_S, GL2.GL_CLAMP_TO_EDGE);
                texturaSkybox.setTexParameteri(gl, GL2.GL_TEXTURE_WRAP_T, GL2.GL_CLAMP_TO_EDGE);
            }
        } catch (IOException e) {
            log.error("No se pudieron cargar las texturas.");
        }
        gl.glEnable(GL2.GL_LIGHTING);
        gl.glEnable(GL2.GL_LIGHT0);
        gl.glEnable(GL2.GL_COLOR_MATERIAL);
        float[] lightPos = { SUN_DIR[0], SUN_DIR[1], SUN_DIR[2], 0.0f };
        float[] ambient = { 0.30f, 0.30f, 0.32f, 1.0f };
        float[] diffuse = { 0.95f, 0.92f, 0.86f, 1.0f };
        float[] specular = { 0.70f, 0.68f, 0.62f, 1.0f };
        gl.glLightfv(GL2.GL_LIGHT0, GL2.GL_AMBIENT, ambient, 0);
        gl.glLightfv(GL2.GL_LIGHT0, GL2.GL_DIFFUSE, diffuse, 0);
        gl.glLightfv(GL2.GL_LIGHT0, GL2.GL_SPECULAR, specular, 0);
        gl.glLightfv(GL2.GL_LIGHT0, GL2.GL_POSITION, lightPos, 0);

        // Compilar y encadenar Pipeline Programable
        initShaders(gl);

        // Crear mallas reutilizables (VBOs)
        initVBOs(gl);
    }

    // --> MÉTODOS DE GENERACIÓN DE MATRICES MANUALES (COLUMN-MAJOR PARA OPENGL) <--
    private float[] createTranslationMatrix(float tx, float ty, float tz) {
        return new float[]{
            1.0f, 0.0f, 0.0f, 0.0f,
            0.0f, 1.0f, 0.0f, 0.0f,
            0.0f, 0.0f, 1.0f, 0.0f,
            tx,   ty,   tz,   1.0f
        };
    }
    private float[] createScaleMatrix(float sx, float sy, float sz) {
        return new float[]{
            sx,   0.0f, 0.0f, 0.0f,
            0.0f, sy,   0.0f, 0.0f,
            0.0f, 0.0f, sz,   0.0f,
            0.0f, 0.0f, 0.0f, 1.0f
        };
    }
    private float[] createRotationXMatrix(float angleDegrees) {
        float rad = (float) Math.toRadians(angleDegrees);
        float cos = (float) Math.cos(rad);
        float sin = (float) Math.sin(rad);
        return new float[]{
            1.0f, 0.0f, 0.0f, 0.0f,
            0.0f, cos,  sin,  0.0f,
            0.0f, -sin, cos,  0.0f,
            0.0f, 0.0f, 0.0f, 1.0f
        };
    }
    private float[] createRotationYMatrix(float angleDegrees) {
        float rad = (float) Math.toRadians(angleDegrees);
        float cos = (float) Math.cos(rad);
        float sin = (float) Math.sin(rad);
        return new float[]{
            cos,  0.0f, -sin, 0.0f,
            0.0f, 1.0f, 0.0f, 0.0f,
            sin,  0.0f, cos,  0.0f,
            0.0f, 0.0f, 0.0f, 1.0f
        };
    }
    private float[] multiplyMatrix(float[] a, float[] b) {
        float[] res = new float[16];
        for (int col = 0; col < 4; col++) {
            for (int row = 0; row < 4; row++) {
                for (int k = 0; k < 4; k++) {
                    res[col * 4 + row] += a[k * 4 + row] * b[col * 4 + k];
                }
            }
        }
        return res;
    }
    private float[] multiplyVector(float[] m, float[] v) {
        float[] res = new float[4];
        for (int row = 0; row < 4; row++) {
            for (int col = 0; col < 4; col++) {
                res[row] += m[col * 4 + row] * v[col];
            }
        }
        return res;
    }

    private float[] createShadowProjectionMatrix(float lx, float ly, float lz, float lw, float planeY) {
        // Plano receptor: y = planeY  =>  0x + 1y + 0z - planeY = 0
        float a = 0.0f;
        float b = 1.0f;
        float c = 0.0f;
        float d = -planeY;
        float dot = a * lx + b * ly + c * lz + d * lw;

        return new float[]{
                dot - lx * a,      -ly * a,          -lz * a,          -lw * a,
                -lx * b,           dot - ly * b,     -lz * b,          -lw * b,
                -lx * c,           -ly * c,          dot - lz * c,     -lw * c,
                -lx * d,           -ly * d,          -lz * d,          dot - lw * d
        };
    }

    // Raycasting manual desde coordenadas de pantalla hacia el plano XZ
    private Point obtenerCoordenadasTablero(int mouseX, int mouseY) {
        int width = viewWidth;
        int height = viewHeight == 0 ? 1 : viewHeight;
        float xNdc = (2.0f * mouseX) / width - 1.0f;
        float yNdc = 1.0f - (2.0f * mouseY) / height;
        float aspect = (float) width / height;
        float fh = 0.5f * (float) Math.tan(Math.toRadians(45.0 / 2.0));
        float fw = fh * aspect;
        float[] rayEyeDir = {xNdc * fw, yNdc * fh, -1.0f, 0.0f};
        float[] vTransC = createTranslationMatrix(0.0f, -2.0f, zoomCam);
        float[] vRotX = createRotationXMatrix(rotCamX);
        float[] vRotY = createRotationYMatrix(rotCamY);
        float[] vTransT = createTranslationMatrix(-tablero.getColumnas() / 2.0f, 0.0f, -tablero.getFilas() / 2.0f);
        float[] invVTransT = createTranslationMatrix(tablero.getColumnas() / 2.0f, 0.0f, tablero.getFilas() / 2.0f);
        float[] invVRotY = createRotationYMatrix(-rotCamY);
        float[] invVRotX = createRotationXMatrix(-rotCamX);
        float[] invVTransC = createTranslationMatrix(0.0f, 2.0f, -zoomCam);
        float[] invView = multiplyMatrix(invVTransT, multiplyMatrix(invVRotY, multiplyMatrix(invVRotX, invVTransC)));
        float[] rayOriginView = {0.0f, 0.0f, 0.0f, 1.0f};
        float[] rayOriginWorld = multiplyVector(invView, rayOriginView);
        float[] rayDirWorld = multiplyVector(invView, rayEyeDir);
        if (Math.abs(rayDirWorld[1]) < 0.0001f) return null;
        float t = (0.25f - rayOriginWorld[1]) / rayDirWorld[1];
        if (t < 0) return null;
        float finalX = rayOriginWorld[0] + t * rayDirWorld[0];
        float finalZ = rayOriginWorld[2] + t * rayDirWorld[2];
        return new Point((int) (finalX * TAMDECUADRADOS), (int) (finalZ * TAMDECUADRADOS));
    }

    @Override
    public void dispose(GLAutoDrawable drawable) {}

    @Override
    public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {
        GL2 gl = drawable.getGL().getGL2();
        viewWidth = width;
        viewHeight = height == 0 ? 1 : height;
        gl.glMatrixMode(GL2.GL_PROJECTION);
        gl.glLoadIdentity();
        float aspect = (float) viewWidth / (float) viewHeight;
        float fh = 0.5f * (float) Math.tan(Math.toRadians(45.0 / 2.0)) * 1.0f;
        float fw = fh * aspect;
        gl.glFrustum(-fw, fw, -fh, fh, 1.0, 100.0);
        gl.glMatrixMode(GL2.GL_MODELVIEW);
        gl.glLoadIdentity();
    }

    @Override
    public void display(GLAutoDrawable drawable) {
        GL2 gl = drawable.getGL().getGL2();
        gl.glClear(GL.GL_COLOR_BUFFER_BIT | GL.GL_DEPTH_BUFFER_BIT);
        gl.glLoadIdentity();

        // 1. Cielo: domo de gradiente procedural + textura opcional 
        // (Sin iluminar/shaders por ahora para el cielo lejano)
        drawSkyGradiente(gl);

        // === ACTIVACIÓN DEL PIPELINE DE SHADER ===
        gl.glUseProgram(shaderProgram);
        gl.glUniform3f(lightDirLoc, SUN_DIR[0], SUN_DIR[1], SUN_DIR[2]);
        gl.glUniform1i(useTextureLoc, 0);
        gl.glUniform1i(isShadowLoc, 0);

        // 2. Cámara dinámica
        gl.glMultMatrixf(createTranslationMatrix(0.0f, -2.0f, zoomCam), 0);
        gl.glMultMatrixf(createRotationXMatrix(rotCamX), 0);
        gl.glMultMatrixf(createRotationYMatrix(rotCamY), 0);
        gl.glLightfv(GL2.GL_LIGHT0, GL2.GL_POSITION, new float[]{SUN_DIR[0], SUN_DIR[1], SUN_DIR[2], 0.0f}, 0);
        // 3. Suelo infinito
        drawPlanoSueloMundo(gl);
        // 4. Sombras simples proyectadas con luz fija (sol)
        drawProjectedShadows(gl);
        // 4. Centrar tablero y dibujar decorado + tablero + coches
        gl.glMultMatrixf(createTranslationMatrix(-tablero.getColumnas() / 2.0f, 0.0f, -tablero.getFilas() / 2.0f), 0);
        dibujarDecorado(gl);
        drawTablero(gl);
        dibujarTodosLosCoches(gl);
        actualizarYDibujarParticulas(gl);
        
        // === DESACTIVACIÓN DE SHADERS (Volvemos a pipeline fijo para la interfaz 2D) ===
        gl.glUseProgram(0);

        // 5. HUD 2D encima de todo
        drawHUD(gl);
    }

    private void drawProjectedShadows(GL2 gl) {
        float[] shadowMatrix = createShadowProjectionMatrix(SUN_DIR[0], SUN_DIR[1], SUN_DIR[2], 0.0f, 0.0f);

        gl.glPushAttrib(GL2.GL_ENABLE_BIT | GL2.GL_CURRENT_BIT | GL2.GL_LIGHTING_BIT | GL2.GL_DEPTH_BUFFER_BIT | GL2.GL_POLYGON_BIT);
        gl.glEnable(GL2.GL_BLEND);
        gl.glBlendFunc(GL2.GL_SRC_ALPHA, GL2.GL_ONE_MINUS_SRC_ALPHA);
        gl.glDisable(GL2.GL_TEXTURE_2D);
        gl.glDepthMask(false);
        gl.glEnable(GL2.GL_POLYGON_OFFSET_FILL);
        gl.glPolygonOffset(-1.0f, -1.0f);
        gl.glDisable(GL2.GL_COLOR_MATERIAL);

        // Activar modo sombra en el shader para forzar color gris oscuro
        gl.glUniform1i(isShadowLoc, 1);

        gl.glPushMatrix();
        // Pequeño desplazamiento para evitar z-fighting.
        gl.glMultMatrixf(createTranslationMatrix(0.0f, 0.02f, 0.0f), 0);
        gl.glMultMatrixf(shadowMatrix, 0);
        gl.glMultMatrixf(createTranslationMatrix(-tablero.getColumnas() / 2.0f, 0.0f, -tablero.getFilas() / 2.0f), 0);
        dibujarDecorado(gl);
        // drawTablero(gl);
        dibujarTodosLosCoches(gl);
        gl.glPopMatrix();

        // Restaurar modo normal
        gl.glUniform1i(isShadowLoc, 0);

        gl.glEnable(GL2.GL_COLOR_MATERIAL);
        gl.glPopAttrib();
    }

    private void dibujarTodosLosCoches(GL2 gl) {
        for (Vehiculo v : vehiculos) {
            gl.glPushMatrix();
            float vx = v.getColumna();
            float vz = v.getFila();
            float length = v.getTamano();
            float offsetX = v.getOrientacion() == Orientacion.HORIZONTAL ? length / 2.0f : 0.5f;
            float offsetZ = v.getOrientacion() == Orientacion.VERTICAL ? length / 2.0f : 0.5f;
            float centroX = vx + offsetX;
            float centroZ = vz + offsetZ;
            gl.glMultMatrixf(createTranslationMatrix(centroX, 0.15f, centroZ), 0);
            if (v.getOrientacion() == Orientacion.HORIZONTAL) {
                gl.glMultMatrixf(createRotationYMatrix(90.0f), 0);
            }
            float velRotRuedas = (vx + vz) * -100.0f;
            // Chasis
            gl.glPushMatrix();
            gl.glMultMatrixf(createTranslationMatrix(0, 0.1f, 0), 0);
            gl.glMultMatrixf(createScaleMatrix(0.8f, 0.25f, length * 0.9f), 0);
            setColorByVehiculo(gl, v);
            drawCubeProcedural(gl);
            gl.glPopMatrix();
            // Cabina de cristal tintado
            gl.glPushMatrix();
            gl.glMultMatrixf(createTranslationMatrix(0, 0.35f, -0.1f * length), 0);
            gl.glMultMatrixf(createScaleMatrix(0.6f, 0.25f, length * 0.45f), 0);
            gl.glColor3f(0.1f, 0.1f, 0.1f);
            drawCubeProcedural(gl);
            gl.glPopMatrix();
            // Ruedas
            float wheelRadius = 0.12f;
            float wheelThickness = 0.1f;
            float pX = 0.45f;
            float pY = 0.05f;
            float pZ = (length * 0.45f) - 0.25f;
            gl.glColor3f(0.05f, 0.05f, 0.05f);
            drawRueda(gl,  pX, pY,  pZ, velRotRuedas, wheelRadius, wheelThickness);
            drawRueda(gl, -pX, pY,  pZ, velRotRuedas, wheelRadius, wheelThickness);
            drawRueda(gl,  pX, pY, -pZ, velRotRuedas, wheelRadius, wheelThickness);
            drawRueda(gl, -pX, pY, -pZ, velRotRuedas, wheelRadius, wheelThickness);
            // Tubo de escape
            float exhaustZ = -(length * 0.45f);
            float exhaustX = -0.2f;
            float exhaustY = 0.05f;
            gl.glPushMatrix();
            gl.glMultMatrixf(createTranslationMatrix(exhaustX, exhaustY, exhaustZ), 0);
            gl.glMultMatrixf(createScaleMatrix(0.05f, 0.05f, 0.15f), 0);
            gl.glColor3f(0.5f, 0.5f, 0.5f);
            drawCubeProcedural(gl);
            gl.glPopMatrix();
            // Partículas de escape
            if (Math.random() < 0.04) {
                float exWorldX = centroX;
                float exWorldZ = centroZ;
                if (v.getOrientacion() == Orientacion.HORIZONTAL) {
                    exWorldX += exhaustZ;
                    exWorldZ += -exhaustX;
                } else {
                    exWorldX += exhaustX;
                    exWorldZ += exhaustZ;
                }
                particulas.add(new Particula(exWorldX, 0.15f, exWorldZ));
            }
            gl.glPopMatrix();
        }
    }

    private void drawRueda(GL2 gl, float px, float py, float pz, float rotAngle, float radius, float thickness) {
        gl.glPushMatrix();
        gl.glMultMatrixf(createTranslationMatrix(px, py, pz), 0);
        gl.glMultMatrixf(createRotationXMatrix(rotAngle), 0);
        gl.glMultMatrixf(createRotationYMatrix(90.0f), 0);
        drawPrisma(gl, radius, thickness, 8);
        gl.glPopMatrix();
    }

    private void drawPrisma(GL2 gl, float radius, float height, int lados) {
        int numVertices = lados * 4 + lados * 2; // Quads + 2 Polygons
        float[] vertices = new float[numVertices * 3];
        float[] normales = new float[numVertices * 3];
        int vIdx = 0;
        int nIdx = 0;

        // Lados (Quads)
        for (int i = 0; i < lados; i++) {
            float angle1 = (float) (i * 2 * Math.PI / lados);
            float angle2 = (float) ((i + 1) * 2 * Math.PI / lados);
            float x1 = (float) Math.cos(angle1) * radius;
            float y1 = (float) Math.sin(angle1) * radius;
            float x2 = (float) Math.cos(angle2) * radius;
            float y2 = (float) Math.sin(angle2) * radius;
            
            float nx = (x1 + x2) / 2f;
            float ny = (y1 + y2) / 2f;
            
            normales[nIdx++] = nx; normales[nIdx++] = ny; normales[nIdx++] = 0;
            vertices[vIdx++] = x1; vertices[vIdx++] = y1; vertices[vIdx++] = height / 2;
            
            normales[nIdx++] = nx; normales[nIdx++] = ny; normales[nIdx++] = 0;
            vertices[vIdx++] = x2; vertices[vIdx++] = y2; vertices[vIdx++] = height / 2;
            
            normales[nIdx++] = nx; normales[nIdx++] = ny; normales[nIdx++] = 0;
            vertices[vIdx++] = x2; vertices[vIdx++] = y2; vertices[vIdx++] = -height / 2;
            
            normales[nIdx++] = nx; normales[nIdx++] = ny; normales[nIdx++] = 0;
            vertices[vIdx++] = x1; vertices[vIdx++] = y1; vertices[vIdx++] = -height / 2;
        }

        int verticesSides = vIdx / 3;

        // Tapa superior
        int topStart = vIdx / 3;
        for (int i = 0; i < lados; i++) {
            float angle1 = (float) (i * 2 * Math.PI / lados);
            normales[nIdx++] = 0; normales[nIdx++] = 0; normales[nIdx++] = 1;
            vertices[vIdx++] = (float) Math.cos(angle1) * radius;
            vertices[vIdx++] = (float) Math.sin(angle1) * radius;
            vertices[vIdx++] = height / 2;
        }

        // Tapa inferior
        int bottomStart = vIdx / 3;
        for (int i = 0; i < lados; i++) {
            float angle1 = (float) (i * 2 * Math.PI / lados);
            normales[nIdx++] = 0; normales[nIdx++] = 0; normales[nIdx++] = -1;
            vertices[vIdx++] = (float) Math.cos(angle1) * radius;
            vertices[vIdx++] = (float) Math.sin(angle1) * radius;
            vertices[vIdx++] = -height / 2;
        }

        java.nio.FloatBuffer vBuf = com.jogamp.common.nio.Buffers.newDirectFloatBuffer(vertices);
        java.nio.FloatBuffer nBuf = com.jogamp.common.nio.Buffers.newDirectFloatBuffer(normales);

        gl.glEnableClientState(GL2.GL_VERTEX_ARRAY);
        gl.glEnableClientState(GL2.GL_NORMAL_ARRAY);
        
        gl.glVertexPointer(3, GL2.GL_FLOAT, 0, vBuf);
        gl.glNormalPointer(GL2.GL_FLOAT, 0, nBuf);

        gl.glDrawArrays(GL2.GL_QUADS, 0, verticesSides);
        gl.glDrawArrays(GL2.GL_POLYGON, topStart, lados);
        gl.glDrawArrays(GL2.GL_POLYGON, bottomStart, lados);

        gl.glDisableClientState(GL2.GL_VERTEX_ARRAY);
        gl.glDisableClientState(GL2.GL_NORMAL_ARRAY);
    }

    // Cono o tronco cónico: base en Y=0, apex/top en Y=height
    private void drawConoTruncado(GL2 gl, float baseRadius, float topRadius, float height, int lados) {
        int numVertices = lados * 4 + lados * 2;
        float[] vertices = new float[numVertices * 3];
        float[] normales = new float[numVertices * 3];
        int vIdx = 0;
        int nIdx = 0;

        for (int i = 0; i < lados; i++) {
            float a1 = (float) (i * 2 * Math.PI / lados);
            float a2 = (float) ((i + 1) * 2 * Math.PI / lados);
            float bx1 = baseRadius * (float) Math.cos(a1);
            float bz1 = baseRadius * (float) Math.sin(a1);
            float bx2 = baseRadius * (float) Math.cos(a2);
            float bz2 = baseRadius * (float) Math.sin(a2);
            float tx1 = topRadius * (float) Math.cos(a1);
            float tz1 = topRadius * (float) Math.sin(a1);
            float tx2 = topRadius * (float) Math.cos(a2);
            float tz2 = topRadius * (float) Math.sin(a2);
            
            float nx = (bx1 + bx2) * 0.5f;
            float nz = (bz1 + bz2) * 0.5f;
            float ny = (baseRadius - topRadius) / height;

            normales[nIdx++] = nx; normales[nIdx++] = ny; normales[nIdx++] = nz;
            vertices[vIdx++] = bx1; vertices[vIdx++] = 0; vertices[vIdx++] = bz1;
            
            normales[nIdx++] = nx; normales[nIdx++] = ny; normales[nIdx++] = nz;
            vertices[vIdx++] = bx2; vertices[vIdx++] = 0; vertices[vIdx++] = bz2;
            
            normales[nIdx++] = nx; normales[nIdx++] = ny; normales[nIdx++] = nz;
            vertices[vIdx++] = tx2; vertices[vIdx++] = height; vertices[vIdx++] = tz2;
            
            normales[nIdx++] = nx; normales[nIdx++] = ny; normales[nIdx++] = nz;
            vertices[vIdx++] = tx1; vertices[vIdx++] = height; vertices[vIdx++] = tz1;
        }

        int verticesSides = vIdx / 3;

        int bottomStart = vIdx / 3;
        for (int i = 0; i < lados; i++) {
            float a = (float) (i * 2 * Math.PI / lados);
            normales[nIdx++] = 0; normales[nIdx++] = -1; normales[nIdx++] = 0;
            vertices[vIdx++] = baseRadius * (float) Math.cos(a);
            vertices[vIdx++] = 0;
            vertices[vIdx++] = baseRadius * (float) Math.sin(a);
        }

        int topStart = vIdx / 3;
        int topCount = 0;
        if (topRadius > 0.001f) {
            for (int i = 0; i < lados; i++) {
                float a = (float) (i * 2 * Math.PI / lados);
                normales[nIdx++] = 0; normales[nIdx++] = 1; normales[nIdx++] = 0;
                vertices[vIdx++] = topRadius * (float) Math.cos(a);
                vertices[vIdx++] = height;
                vertices[vIdx++] = topRadius * (float) Math.sin(a);
                topCount++;
            }
        }

        java.nio.FloatBuffer vBuf = com.jogamp.common.nio.Buffers.newDirectFloatBuffer(vertices, 0, vIdx);
        java.nio.FloatBuffer nBuf = com.jogamp.common.nio.Buffers.newDirectFloatBuffer(normales, 0, nIdx);

        gl.glEnableClientState(GL2.GL_VERTEX_ARRAY);
        gl.glEnableClientState(GL2.GL_NORMAL_ARRAY);
        
        gl.glVertexPointer(3, GL2.GL_FLOAT, 0, vBuf);
        gl.glNormalPointer(GL2.GL_FLOAT, 0, nBuf);

        gl.glDrawArrays(GL2.GL_QUADS, 0, verticesSides);
        gl.glDrawArrays(GL2.GL_POLYGON, bottomStart, lados);
        if (topCount > 0) {
            gl.glDrawArrays(GL2.GL_POLYGON, topStart, topCount);
        }

        gl.glDisableClientState(GL2.GL_VERTEX_ARRAY);
        gl.glDisableClientState(GL2.GL_NORMAL_ARRAY);
    }

    private void actualizarYDibujarParticulas(GL2 gl) {
        gl.glDisable(GL2.GL_LIGHTING);
        gl.glColor3f(0.7f, 0.7f, 0.7f);
        Iterator<Particula> it = particulas.iterator();
        while (it.hasNext()) {
            Particula p = it.next();
            p.x += p.vx;
            p.y += p.vy;
            p.z += p.vz;
            p.life--;
            if (p.life <= 0) {
                it.remove();
                continue;
            }
            gl.glPushMatrix();
            gl.glMultMatrixf(createTranslationMatrix(p.x, p.y, p.z), 0);
            float scale = 0.02f + (p.maxLife - p.life) * 0.003f;
            gl.glMultMatrixf(createScaleMatrix(scale, scale, scale), 0);
            drawCubeProcedural(gl);
            gl.glPopMatrix();
        }
        gl.glEnable(GL2.GL_LIGHTING);
    }

    private void drawPlanoSueloMundo(GL2 gl) {
        gl.glPushMatrix();
        gl.glMultMatrixf(createTranslationMatrix(0.0f, -0.1f, 0.0f), 0);
        if (texturaSuelo != null) {
            gl.glEnable(GL2.GL_TEXTURE_2D);
            texturaSuelo.bind(gl);
            gl.glUniform1i(useTextureLoc, 1); // Avisar al shader que use textura
        }
        float size = 150.0f;
        float tiling = 40.0f;
        gl.glColor3f(1.0f, 1.0f, 1.0f);
        
        float[] vertices = {
            -size, 0.0f, -size,
            -size, 0.0f,  size,
             size, 0.0f,  size,
             size, 0.0f, -size
        };
        
        float[] normales = {
            0.0f, 1.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 1.0f, 0.0f
        };
        
        float[] texcoords = {
            0.0f, tiling,
            0.0f, 0.0f,
            tiling, 0.0f,
            tiling, tiling
        };

        java.nio.FloatBuffer vBuf = com.jogamp.common.nio.Buffers.newDirectFloatBuffer(vertices);
        java.nio.FloatBuffer nBuf = com.jogamp.common.nio.Buffers.newDirectFloatBuffer(normales);
        java.nio.FloatBuffer tBuf = com.jogamp.common.nio.Buffers.newDirectFloatBuffer(texcoords);

        gl.glEnableClientState(GL2.GL_VERTEX_ARRAY);
        gl.glEnableClientState(GL2.GL_NORMAL_ARRAY);
        gl.glEnableClientState(GL2.GL_TEXTURE_COORD_ARRAY);
        
        gl.glVertexPointer(3, GL2.GL_FLOAT, 0, vBuf);
        gl.glNormalPointer(GL2.GL_FLOAT, 0, nBuf);
        gl.glTexCoordPointer(2, GL2.GL_FLOAT, 0, tBuf);
        
        gl.glDrawArrays(GL2.GL_QUADS, 0, 4);
        
        gl.glDisableClientState(GL2.GL_VERTEX_ARRAY);
        gl.glDisableClientState(GL2.GL_NORMAL_ARRAY);
        gl.glDisableClientState(GL2.GL_TEXTURE_COORD_ARRAY);

        if (texturaSuelo != null) {
            gl.glDisable(GL2.GL_TEXTURE_2D);
            gl.glUniform1i(useTextureLoc, 0); // Desactivar textura en el shader
        }
        gl.glPopMatrix();
    }

    private void drawTablero(GL2 gl) {
        char[][] celdas = tablero.getCeldas();
        int filas = tablero.getFilas();
        int cols  = tablero.getColumnas();

        gl.glDisable(GL2.GL_LIGHTING);  // ← imprescindible para que el color sea exacto

        for (int i = 0; i < filas; i++) {
            for (int j = 0; j < cols; j++) {
                gl.glPushMatrix();
                gl.glMultMatrixf(createTranslationMatrix(j + 0.5f, -0.05f, i + 0.5f), 0);
                gl.glMultMatrixf(createScaleMatrix(0.95f, 0.1f, 0.95f), 0);

                boolean esBorde = (i == 0 || i == filas - 1 || j == 0 || j == cols - 1);
                if (esBorde) {
                    gl.glColor3f(0.35f, 0.35f, 0.35f); // gris oscuro
                } else {
                    gl.glColor3f(0.8f, 0.8f, 0.8f); // gris claro
                }
                drawCubeProcedural(gl);
                gl.glPopMatrix();
            }
        }

        gl.glEnable(GL2.GL_LIGHTING);   // ← restaurar

        // Celda de salida
        Posicion salida = tablero.getSalida();
        gl.glPushMatrix();
        gl.glMultMatrixf(createTranslationMatrix(salida.getColumna() + 0.5f, -0.04f, salida.getFila() + 0.5f), 0);
        gl.glMultMatrixf(createScaleMatrix(1.0f, 0.12f, 1.0f), 0);
        gl.glColor3f(0.0f, 0.8f, 0.0f);
        drawCubeProcedural(gl);
        gl.glPopMatrix();
    }

    // ==================== CIELO GRADIENTE PROCEDURAL ====================
    // Dibuja un domo semiesférico con degradado de color (horizonte -> cénit)
    // Con la textura del skybox superpuesta si está disponible.
    private void drawSkyGradiente(GL2 gl) {
        gl.glPushMatrix();
        gl.glLoadIdentity();
        gl.glDepthMask(false);
        gl.glDisable(GL2.GL_LIGHTING);
        // Rotar con la cámara para que el cielo se mueva con la vista
        gl.glMultMatrixf(createRotationXMatrix(rotCamX), 0);
        gl.glMultMatrixf(createRotationYMatrix(rotCamY), 0);

        float radio = 45.0f;
        int anillos = 18;
        int sectores = 36;
        // Colores del degradado (horizonte → cénit)
        float[] horizonte = {0.80f, 0.88f, 0.98f};
        float[] cenital   = {0.18f, 0.45f, 0.82f};

        // Semiesfera superior
        for (int ring = 0; ring < anillos; ring++) {
            float phi1 = (float) (ring     * Math.PI * 0.5 / anillos); // 0 → PI/2
            float phi2 = (float) ((ring+1) * Math.PI * 0.5 / anillos);
            float t1 = (float) ring     / anillos;
            float t2 = (float) (ring+1) / anillos;
            float r1 = horizonte[0] * (1-t1) + cenital[0] * t1;
            float g1 = horizonte[1] * (1-t1) + cenital[1] * t1;
            float b1 = horizonte[2] * (1-t1) + cenital[2] * t1;
            float r2 = horizonte[0] * (1-t2) + cenital[0] * t2;
            float g2 = horizonte[1] * (1-t2) + cenital[1] * t2;
            float b2 = horizonte[2] * (1-t2) + cenital[2] * t2;

            if (texturaSkybox != null) {
                gl.glEnable(GL2.GL_TEXTURE_2D);
                texturaSkybox.bind(gl);
                gl.glColor3f(1f, 1f, 1f);
            }

            gl.glBegin(GL2.GL_QUAD_STRIP);
            for (int sec = 0; sec <= sectores; sec++) {
                float theta = (float) (sec * 2 * Math.PI / sectores);
                float cosT = (float) Math.cos(theta);
                float sinT = (float) Math.sin(theta);
                float sinP1 = (float) Math.sin(phi1);
                float cosP1 = (float) Math.cos(phi1);
                float sinP2 = (float) Math.sin(phi2);
                float cosP2 = (float) Math.cos(phi2);
                if (texturaSkybox != null) {
                    // UV equirectangular
                    float u = (float) sec / sectores;
                    float v1 = t1;
                    float v2 = t2;
                    gl.glTexCoord2f(u, v1);
                    gl.glVertex3f(radio * sinP1 * cosT, radio * cosP1, radio * sinP1 * sinT);
                    gl.glTexCoord2f(u, v2);
                    gl.glVertex3f(radio * sinP2 * cosT, radio * cosP2, radio * sinP2 * sinT);
                } else {
                    gl.glColor3f(r1, g1, b1);
                    gl.glVertex3f(radio * sinP1 * cosT, radio * cosP1, radio * sinP1 * sinT);
                    gl.glColor3f(r2, g2, b2);
                    gl.glVertex3f(radio * sinP2 * cosT, radio * cosP2, radio * sinP2 * sinT);
                }
            }
            gl.glEnd();
        }

        if (texturaSkybox != null) gl.glDisable(GL2.GL_TEXTURE_2D);

        // Franja de horizonte / bruma (semiesfera inferior, solo gradiente)
        float[] bruma = {0.70f, 0.78f, 0.85f};
        for (int ring = 0; ring < 6; ring++) {
            float phi1 = (float) (ring     * Math.PI * 0.25 / 6); // 0 → PI/4 hacia abajo
            float phi2 = (float) ((ring+1) * Math.PI * 0.25 / 6);
            float t1 = (float) ring     / 6;
            float t2 = (float) (ring+1) / 6;
            float r1 = horizonte[0] * (1-t1) + bruma[0] * t1;
            float g1 = horizonte[1] * (1-t1) + bruma[1] * t1;
            float b1 = horizonte[2] * (1-t1) + bruma[2] * t1;
            float r2 = horizonte[0] * (1-t2) + bruma[0] * t2;
            float g2 = horizonte[1] * (1-t2) + bruma[1] * t2;
            float b2 = horizonte[2] * (1-t2) + bruma[2] * t2;
            gl.glBegin(GL2.GL_QUAD_STRIP);
            for (int sec = 0; sec <= sectores; sec++) {
                float theta = (float) (sec * 2 * Math.PI / sectores);
                float cosT = (float) Math.cos(theta);
                float sinT = (float) Math.sin(theta);
                gl.glColor3f(r1, g1, b1);
                gl.glVertex3f(radio * (float)Math.sin(-phi1) * cosT, radio * (float)Math.cos(-phi1), radio * (float)Math.sin(-phi1) * sinT);
                gl.glColor3f(r2, g2, b2);
                gl.glVertex3f(radio * (float)Math.sin(-phi2) * cosT, radio * (float)Math.cos(-phi2), radio * (float)Math.sin(-phi2) * sinT);
            }
            gl.glEnd();
        }

        drawSolEnCielo(gl, radio * 0.9f, 2.4f);

        gl.glEnable(GL2.GL_LIGHTING);
        gl.glDepthMask(true);
        gl.glPopMatrix();
    }

    private void drawSolEnCielo(GL2 gl, float distancia, float radioSol) {
        float dx = SUN_DIR[0];
        float dy = SUN_DIR[1];
        float dz = SUN_DIR[2];
        float len = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (len == 0.0f) {
            return;
        }
        dx /= len;
        dy /= len;
        dz /= len;

        float cx = dx * distancia;
        float cy = dy * distancia;
        float cz = dz * distancia;

        float refX = 0.0f, refY = 1.0f, refZ = 0.0f;
        if (Math.abs(dy) > 0.95f) {
            refX = 1.0f;
            refY = 0.0f;
            refZ = 0.0f;
        }

        float rx = refY * dz - refZ * dy;
        float ry = refZ * dx - refX * dz;
        float rz = refX * dy - refY * dx;
        float rlen = (float) Math.sqrt(rx * rx + ry * ry + rz * rz);
        if (rlen == 0.0f) {
            return;
        }
        rx /= rlen;
        ry /= rlen;
        rz /= rlen;

        float ux = dy * rz - dz * ry;
        float uy = dz * rx - dx * rz;
        float uz = dx * ry - dy * rx;

        int segmentos = 28;
        float[] vertices = new float[(segmentos + 2) * 3];
        float[] colors = new float[(segmentos + 2) * 3];
        int vIdx = 0;
        int cIdx = 0;

        colors[cIdx++] = 1.0f; colors[cIdx++] = 0.96f; colors[cIdx++] = 0.74f;
        vertices[vIdx++] = cx; vertices[vIdx++] = cy; vertices[vIdx++] = cz;

        for (int i = 0; i <= segmentos; i++) {
            float a = (float) (2.0 * Math.PI * i / segmentos);
            float px = cx + (float) Math.cos(a) * radioSol * rx + (float) Math.sin(a) * radioSol * ux;
            float py = cy + (float) Math.cos(a) * radioSol * ry + (float) Math.sin(a) * radioSol * uy;
            float pz = cz + (float) Math.cos(a) * radioSol * rz + (float) Math.sin(a) * radioSol * uz;
            
            colors[cIdx++] = 1.0f; colors[cIdx++] = 0.84f; colors[cIdx++] = 0.34f;
            vertices[vIdx++] = px; vertices[vIdx++] = py; vertices[vIdx++] = pz;
        }

        java.nio.FloatBuffer vBuf = com.jogamp.common.nio.Buffers.newDirectFloatBuffer(vertices);
        java.nio.FloatBuffer cBuf = com.jogamp.common.nio.Buffers.newDirectFloatBuffer(colors);

        gl.glEnableClientState(GL2.GL_VERTEX_ARRAY);
        gl.glEnableClientState(GL2.GL_COLOR_ARRAY);
        
        gl.glVertexPointer(3, GL2.GL_FLOAT, 0, vBuf);
        gl.glColorPointer(3, GL2.GL_FLOAT, 0, cBuf);

        gl.glDrawArrays(GL2.GL_TRIANGLE_FAN, 0, segmentos + 2);

        gl.glDisableClientState(GL2.GL_VERTEX_ARRAY);
        gl.glDisableClientState(GL2.GL_COLOR_ARRAY);
    }

    private void setColorByVehiculo(GL2 gl, Vehiculo v) {
        switch (v.getColor()) {
            case "R":   gl.glColor3f(1.0f, 0.0f, 0.0f); break;
            case "B":   gl.glColor3f(0.0f, 0.0f, 1.0f); break;
            case "G":   gl.glColor3f(0.2f, 0.2f, 0.2f); break;
            case "Y":   gl.glColor3f(1.0f, 1.0f, 0.0f); break;
            case "O":   gl.glColor3f(1.0f, 0.5f, 0.0f); break;
            case "P":   gl.glColor3f(1.0f, 0.75f, 0.8f); break;
            case "GR":  gl.glColor3f(0.5f, 0.5f, 0.5f); break;
            case "BL":  gl.glColor3f(0.0f, 0.0f, 0.8f); break;
            case "GRE": gl.glColor3f(1.0f, 0.0f, 1.0f); break;
            case "YE":  gl.glColor3f(0.8f, 0.8f, 0.0f); break;
            default:    gl.glColor3f(0.0f, 1.0f, 1.0f); break;
        }
    }

    private void drawCubeProcedural(GL2 gl) {
        // Enlazar VBO de vértices
        gl.glBindBuffer(GL2.GL_ARRAY_BUFFER, vboCube[0]);
        gl.glEnableClientState(GL2.GL_VERTEX_ARRAY);
        gl.glVertexPointer(3, GL2.GL_FLOAT, 0, 0);

        // Enlazar VBO de normales
        gl.glBindBuffer(GL2.GL_ARRAY_BUFFER, vboCube[1]);
        gl.glEnableClientState(GL2.GL_NORMAL_ARRAY);
        gl.glNormalPointer(GL2.GL_FLOAT, 0, 0);

        // Dibujar el cubo mediante hardware instancing/VBO batch en vez de glBegin/glEnd
        gl.glDrawArrays(GL2.GL_QUADS, 0, CUBE_VERTEX_COUNT);

        // Desactivar punteros y desenlazar buffer
        gl.glDisableClientState(GL2.GL_VERTEX_ARRAY);
        gl.glDisableClientState(GL2.GL_NORMAL_ARRAY);
        gl.glBindBuffer(GL2.GL_ARRAY_BUFFER, 0);
    }

    // ==================== DECORADO: ÁRBOLES Y CONOS ====================

    // Árbol procedural: tronco + 3 capas de follaje (conos)
    private void drawArbol(GL2 gl) {
        // Tronco: usar drawConoTruncado que dibuja en eje Y (Y=0 → Y=height),
        // no drawPrisma (que dibuja en eje Z y se tumba al rotar la cámara)
        gl.glPushMatrix();
        gl.glColor3f(0.40f, 0.25f, 0.10f);
        drawConoTruncado(gl, 0.07f, 0.07f, 0.7f, 6);
        gl.glPopMatrix();

        // Capa 1 de follaje – base
        gl.glPushMatrix();
        gl.glMultMatrixf(createTranslationMatrix(0, 0.4f, 0), 0);
        gl.glColor3f(0.10f, 0.55f, 0.10f);
        drawConoTruncado(gl, 0.40f, 0.02f, 0.55f, 10);
        gl.glPopMatrix();

        // Capa 2
        gl.glPushMatrix();
        gl.glMultMatrixf(createTranslationMatrix(0, 0.72f, 0), 0);
        gl.glColor3f(0.12f, 0.62f, 0.12f);
        drawConoTruncado(gl, 0.30f, 0.02f, 0.48f, 10);
        gl.glPopMatrix();

        // Capa 3 – punta
        gl.glPushMatrix();
        gl.glMultMatrixf(createTranslationMatrix(0, 0.98f, 0), 0);
        gl.glColor3f(0.15f, 0.70f, 0.15f);
        drawConoTruncado(gl, 0.20f, 0.02f, 0.40f, 10);
        gl.glPopMatrix();
    }

    // Cono de tráfico procedural: base plana + cuerpo cónico naranja + franja blanca
    private void drawConoDeTráfico(GL2 gl) {
        // Base plana (disco hexagonal aplastado, naranja oscuro)
        gl.glPushMatrix();
        gl.glColor3f(0.85f, 0.35f, 0.0f);
        drawConoTruncado(gl, 0.18f, 0.15f, 0.04f, 8);
        gl.glPopMatrix();

        // Cuerpo del cono (naranja brillante)
        gl.glPushMatrix();
        gl.glMultMatrixf(createTranslationMatrix(0, 0.04f, 0), 0);
        gl.glColor3f(1.0f, 0.45f, 0.0f);
        drawConoTruncado(gl, 0.15f, 0.01f, 0.45f, 10);
        gl.glPopMatrix();

        // Franja blanca (anillo estrecho a media altura)
        gl.glPushMatrix();
        gl.glMultMatrixf(createTranslationMatrix(0, 0.18f, 0), 0);
        gl.glColor3f(0.95f, 0.95f, 0.95f);
        drawConoTruncado(gl, 0.095f, 0.07f, 0.05f, 10);
        gl.glPopMatrix();
    }

    private void drawEdificio(GL2 gl, float ancho, float alto, float fondo, float r, float g, float b) {
        // Cuerpo principal
        gl.glPushMatrix();
        gl.glMultMatrixf(createTranslationMatrix(0, alto * 0.5f, 0), 0);
        gl.glMultMatrixf(createScaleMatrix(ancho, alto, fondo), 0);
        gl.glColor3f(r, g, b);
        drawCubeProcedural(gl);
        gl.glPopMatrix();

        // Cornisa simple
        gl.glPushMatrix();
        gl.glMultMatrixf(createTranslationMatrix(0, alto + 0.08f, 0), 0);
        gl.glMultMatrixf(createScaleMatrix(ancho * 1.08f, 0.12f, fondo * 1.08f), 0);
        gl.glColor3f(r * 0.8f, g * 0.8f, b * 0.8f);
        drawCubeProcedural(gl);
        gl.glPopMatrix();

        // Ventanas frontales (procedurales con cubos finos)
        int filasVent = Math.max(2, (int) (alto * 1.8f));
        int colsVent = Math.max(2, (int) (ancho * 2.2f));
        float pasoY = (alto * 0.75f) / filasVent;
        float pasoX = (ancho * 0.7f) / colsVent;
        for (int fy = 0; fy < filasVent; fy++) {
            for (int fx = 0; fx < colsVent; fx++) {
                float wx = -ancho * 0.35f + pasoX * 0.5f + fx * pasoX;
                float wy = alto * 0.2f + fy * pasoY;
                gl.glPushMatrix();
                gl.glMultMatrixf(createTranslationMatrix(wx, wy, fondo * 0.5f + 0.01f), 0);
                gl.glMultMatrixf(createScaleMatrix(pasoX * 0.35f, pasoY * 0.45f, 0.03f), 0);
                gl.glColor3f(0.95f, 0.92f, 0.55f);
                drawCubeProcedural(gl);
                gl.glPopMatrix();
            }
        }
    }

    private void drawCasa(GL2 gl, float ancho, float alto, float fondo) {
        // Base de casa
        gl.glPushMatrix();
        gl.glMultMatrixf(createTranslationMatrix(0, alto * 0.45f, 0), 0);
        gl.glMultMatrixf(createScaleMatrix(ancho, alto * 0.9f, fondo), 0);
        gl.glColor3f(0.82f, 0.73f, 0.60f);
        drawCubeProcedural(gl);
        gl.glPopMatrix();

        // Tejado piramidal lowpoly.
        gl.glPushMatrix();
        gl.glMultMatrixf(createTranslationMatrix(0, alto * 0.9f, 0), 0);
        gl.glColor3f(0.68f, 0.22f, 0.20f);
        drawPiramideTejado(gl, ancho * 1.05f, fondo * 1.05f, alto * 0.55f);
        gl.glPopMatrix();

        // Puerta
        gl.glPushMatrix();
        gl.glMultMatrixf(createTranslationMatrix(0, alto * 0.28f, fondo * 0.5f + 0.01f), 0);
        gl.glMultMatrixf(createScaleMatrix(ancho * 0.2f, alto * 0.35f, 0.03f), 0);
        gl.glColor3f(0.30f, 0.20f, 0.12f);
        drawCubeProcedural(gl);
        gl.glPopMatrix();
    }

    private void drawPiramideTejado(GL2 gl, float ancho, float fondo, float altura) {
        float x = ancho * 0.5f;
        float z = fondo * 0.5f;
        float apexY = altura;

        float[] vertices = {
            // Front
            -x, 0.0f, z,    x, 0.0f, z,    0.0f, apexY, 0.0f,
            // Right
             x, 0.0f, z,    x, 0.0f,-z,    0.0f, apexY, 0.0f,
            // Back
             x, 0.0f,-z,   -x, 0.0f,-z,    0.0f, apexY, 0.0f,
            // Left
            -x, 0.0f,-z,   -x, 0.0f, z,    0.0f, apexY, 0.0f
        };

        float[] normales = {
            0.0f, 0.55f, 1.0f,   0.0f, 0.55f, 1.0f,   0.0f, 0.55f, 1.0f, // Front
            1.0f, 0.55f, 0.0f,   1.0f, 0.55f, 0.0f,   1.0f, 0.55f, 0.0f, // Right
            0.0f, 0.55f,-1.0f,   0.0f, 0.55f,-1.0f,   0.0f, 0.55f,-1.0f, // Back
           -1.0f, 0.55f, 0.0f,  -1.0f, 0.55f, 0.0f,  -1.0f, 0.55f, 0.0f  // Left
        };

        java.nio.FloatBuffer vBuf = com.jogamp.common.nio.Buffers.newDirectFloatBuffer(vertices);
        java.nio.FloatBuffer nBuf = com.jogamp.common.nio.Buffers.newDirectFloatBuffer(normales);

        gl.glEnableClientState(GL2.GL_VERTEX_ARRAY);
        gl.glEnableClientState(GL2.GL_NORMAL_ARRAY);

        gl.glVertexPointer(3, GL2.GL_FLOAT, 0, vBuf);
        gl.glNormalPointer(GL2.GL_FLOAT, 0, nBuf);

        gl.glDrawArrays(GL2.GL_TRIANGLES, 0, 12);

        gl.glDisableClientState(GL2.GL_VERTEX_ARRAY);
        gl.glDisableClientState(GL2.GL_NORMAL_ARRAY);
    }

    private void drawContenedorBasura(GL2 gl) {
        gl.glPushMatrix();
        gl.glMultMatrixf(createTranslationMatrix(0, 0.45f, 0), 0);
        gl.glMultMatrixf(createScaleMatrix(0.85f, 0.9f, 0.55f), 0);
        gl.glColor3f(0.10f, 0.45f, 0.22f);
        drawCubeProcedural(gl);
        gl.glPopMatrix();

        // Tapa
        gl.glPushMatrix();
        gl.glMultMatrixf(createTranslationMatrix(0, 0.95f, 0), 0);
        gl.glMultMatrixf(createScaleMatrix(0.95f, 0.08f, 0.65f), 0);
        gl.glColor3f(0.08f, 0.32f, 0.16f);
        drawCubeProcedural(gl);
        gl.glPopMatrix();
    }

    private void drawPapelera(GL2 gl) {
        gl.glPushMatrix();
        gl.glMultMatrixf(createTranslationMatrix(0, 0.0f, 0), 0);
        gl.glColor3f(0.18f, 0.18f, 0.20f);
        drawConoTruncado(gl, 0.14f, 0.11f, 0.55f, 8);
        gl.glPopMatrix();
    }

    // Dibuja árboles y conos alrededor del parking (coordenadas relativas al tablero)
    private void dibujarDecorado(GL2 gl) {
        int cols = tablero.getColumnas();
        int rows = tablero.getFilas();

        // Conos en las 4 esquinas del tablero
        colocarCono(gl, -0.6f, 0f, -0.6f);
        colocarCono(gl, cols + 0.6f, 0f, -0.6f);
        colocarCono(gl, -0.6f, 0f, rows + 0.6f);
        colocarCono(gl, cols + 0.6f, 0f, rows + 0.6f);

        // Conos a lo largo de los bordes cada 2 celdas
        for (int j = 1; j < cols; j += 2) {
            colocarCono(gl, j + 0.5f, 0f, -0.7f);
            colocarCono(gl, j + 0.5f, 0f, rows + 0.7f);
        }
        for (int i = 1; i < rows; i += 2) {
            colocarCono(gl, -0.7f, 0f, i + 0.5f);
            colocarCono(gl, cols + 0.7f, 0f, i + 0.5f);
        }

        // Árboles más alejados, cada 3 unidades alrededor del perímetro
        for (int j = 0; j <= cols; j += 3) {
            colocaArbol(gl, j + 0.5f, 0f, -2.8f);
            colocaArbol(gl, j + 0.5f, 0f, rows + 2.8f);
        }
        for (int i = 0; i <= rows; i += 3) {
            colocaArbol(gl, -2.8f, 0f, i + 0.5f);
            colocaArbol(gl, cols + 2.8f, 0f, i + 0.5f);
        }

        // Edificios y casas alrededor, sin invadir la zona jugable.
        colocarEdificio(gl, -5.2f, 0f, -1.8f, 1.6f, 3.6f, 1.3f, 0.30f, 0.36f, 0.44f);
        colocarEdificio(gl, cols + 5.2f, 0f, -1.4f, 1.8f, 4.2f, 1.4f, 0.26f, 0.30f, 0.38f);
        colocarEdificio(gl, -5.4f, 0f, rows + 1.8f, 1.7f, 3.2f, 1.5f, 0.38f, 0.34f, 0.30f);
        colocarEdificio(gl, cols + 5.0f, 0f, rows + 2.0f, 2.0f, 4.5f, 1.6f, 0.32f, 0.35f, 0.40f);

        colocarCasa(gl, -3.8f, 0f, rows * 0.35f, 1.3f, 1.4f, 1.1f);
        colocarCasa(gl, cols + 3.8f, 0f, rows * 0.60f, 1.4f, 1.5f, 1.1f);

        // Elementos urbanos del parking: contenedores y papeleras.
        colocarContenedor(gl, cols * 0.22f, 0f, -1.5f);
        colocarContenedor(gl, cols * 0.76f, 0f, rows + 1.5f);
        colocarPapelera(gl, -1.4f, 0f, rows * 0.50f);
        colocarPapelera(gl, cols + 1.4f, 0f, rows * 0.48f);
    }

    private void colocaArbol(GL2 gl, float x, float y, float z) {
        gl.glPushMatrix();
        gl.glMultMatrixf(createTranslationMatrix(x, y, z), 0);
        drawArbol(gl);
        gl.glPopMatrix();
    }

    private void colocarCono(GL2 gl, float x, float y, float z) {
        gl.glPushMatrix();
        gl.glMultMatrixf(createTranslationMatrix(x, y, z), 0);
        drawConoDeTráfico(gl);
        gl.glPopMatrix();
    }

    private void colocarEdificio(GL2 gl, float x, float y, float z, float ancho, float alto, float fondo, float r, float g, float b) {
        gl.glPushMatrix();
        gl.glMultMatrixf(createTranslationMatrix(x, y, z), 0);
        drawEdificio(gl, ancho, alto, fondo, r, g, b);
        gl.glPopMatrix();
    }

    private void colocarCasa(GL2 gl, float x, float y, float z, float ancho, float alto, float fondo) {
        gl.glPushMatrix();
        gl.glMultMatrixf(createTranslationMatrix(x, y, z), 0);
        drawCasa(gl, ancho, alto, fondo);
        gl.glPopMatrix();
    }

    private void colocarContenedor(GL2 gl, float x, float y, float z) {
        gl.glPushMatrix();
        gl.glMultMatrixf(createTranslationMatrix(x, y, z), 0);
        drawContenedorBasura(gl);
        gl.glPopMatrix();
    }

    private void colocarPapelera(GL2 gl, float x, float y, float z) {
        gl.glPushMatrix();
        gl.glMultMatrixf(createTranslationMatrix(x, y, z), 0);
        drawPapelera(gl);
        gl.glPopMatrix();
    }

    // ==================== HUD 2D (superposición OpenGL) ====================

    private void drawHUD(GL2 gl) {
        int w = viewWidth;
        int h = viewHeight == 0 ? 1 : viewHeight;

        // Cambiar a proyección ortogonal 2D
        gl.glMatrixMode(GL2.GL_PROJECTION);
        gl.glPushMatrix();
        gl.glLoadIdentity();
        gl.glOrtho(0, w, 0, h, -1, 1);

        gl.glMatrixMode(GL2.GL_MODELVIEW);
        gl.glPushMatrix();
        gl.glLoadIdentity();

        gl.glDisable(GL2.GL_DEPTH_TEST);
        gl.glDisable(GL2.GL_LIGHTING);
        gl.glDisable(GL2.GL_TEXTURE_2D);

        // --- Barra superior (título + puntuación) ---
        drawHUDPanel(gl, 0, h - HUD_TOP_PX, w, HUD_TOP_PX, 0.10f, 0.10f, 0.15f);
        gl.glColor3f(1.0f, 1.0f, 1.0f);
        int topTextY = h - HUD_TOP_PX + 18;
        drawHUDText(gl, 14, topTextY, hudNivelTitulo, HUD_FONT);
        int totalWidth = glut.glutBitmapLength(HUD_FONT, hudMovimientosTotales);
        int movWidth = glut.glutBitmapLength(HUD_FONT, hudMovimientos);
        int totalX = w - 14 - totalWidth;
        int movX = totalX - 30 - movWidth;
        drawHUDText(gl, movX, topTextY, hudMovimientos, HUD_FONT);
        drawHUDText(gl, totalX, topTextY, hudMovimientosTotales, HUD_FONT);

        // --- Barra inferior (botones) ---
        drawHUDPanel(gl, 0, 0, w, HUD_BOT_PX, 0.10f, 0.10f, 0.15f);

        // Calcular posición X centrada para los 6 botones
        int totalBtns = BTN_LABELS.length;
        int totalW = totalBtns * BTN_W + (totalBtns - 1) * 10;
        int startX = (w - totalW) / 2;
        for (int i = 0; i < totalBtns; i++) {
            btnX[i] = startX + i * (BTN_W + 10);
            // Fondo del botón
            drawHUDPanel(gl, btnX[i], BTN_Y_OGL, BTN_W, BTN_H, 0.25f, 0.35f, 0.55f);
            // Borde del botón
            drawHUDBorder(gl, btnX[i], BTN_Y_OGL, BTN_W, BTN_H, 0.50f, 0.70f, 1.0f);
            // Texto centrado en el botón
            gl.glColor3f(1.0f, 1.0f, 1.0f);
            int labelWidth = glut.glutBitmapLength(HUD_FONT, BTN_LABELS[i]);
            int labelX = btnX[i] + (BTN_W - labelWidth) / 2;
            int labelY = BTN_Y_OGL + (BTN_H / 2) - 5;
            drawHUDText(gl, labelX, labelY, BTN_LABELS[i], HUD_FONT);
        }
        if (modalActivo) {
            drawModalOverlay(gl, w, h);
        }

        // Restaurar estado
        gl.glEnable(GL2.GL_DEPTH_TEST);
        gl.glEnable(GL2.GL_LIGHTING);

        gl.glMatrixMode(GL2.GL_PROJECTION);
        gl.glPopMatrix();
        gl.glMatrixMode(GL2.GL_MODELVIEW);
        gl.glPopMatrix();
    }

    // Panel sólido en coordenadas de pantalla (y desde abajo)
    private void drawHUDPanel(GL2 gl, int x, int y, int w, int h,
                               float r, float g, float b) {
        gl.glColor3f(r, g, b);
        gl.glBegin(GL2.GL_QUADS);
        gl.glVertex2i(x,     y);
        gl.glVertex2i(x + w, y);
        gl.glVertex2i(x + w, y + h);
        gl.glVertex2i(x,     y + h);
        gl.glEnd();
    }

    // Borde de un panel (marco)
    private void drawHUDBorder(GL2 gl, int x, int y, int w, int h,
                                float r, float g, float b) {
        gl.glColor3f(r, g, b);
        gl.glBegin(GL2.GL_LINE_LOOP);
        gl.glVertex2i(x,     y);
        gl.glVertex2i(x + w, y);
        gl.glVertex2i(x + w, y + h);
        gl.glVertex2i(x,     y + h);
        gl.glEnd();
    }

    // Texto bitmap en coordenadas de pantalla
    private void drawHUDText(GL2 gl, int x, int y, String texto, int fuente) {
        gl.glRasterPos2i(x, y);
        glut.glutBitmapString(fuente, texto);
    }

    private void drawModalOverlay(GL2 gl, int w, int h) {
        modalW = Math.min((int) (w * 0.82f), 1020);
        modalH = Math.max(300, 220 + modalLineas.length * 34);
        modalX = (w - modalW) / 2;
        modalY = (h - modalH) / 2;
        modalBtnW = 300;
        modalBtnH = 64;
        modalBtnX = modalX + (modalW - modalBtnW) / 2;
        modalBtnY = modalY + 30;

        drawHUDPanel(gl, 0, 0, w, h, 0.03f, 0.03f, 0.03f);
        drawHUDPanel(gl, modalX, modalY, modalW, modalH, 0.08f, 0.10f, 0.16f);
        drawHUDBorder(gl, modalX, modalY, modalW, modalH, 0.50f, 0.70f, 1.0f);

        gl.glColor3f(1.0f, 1.0f, 1.0f);
        drawHUDText(gl, modalX + 30, modalY + modalH - 48, modalTitulo, MODAL_TITLE_FONT);
        int textY = modalY + modalH - 92;
        for (String linea : modalLineas) {
            drawHUDText(gl, modalX + 30, textY, linea, MODAL_TEXT_FONT);
            textY -= 28;
        }

        drawHUDPanel(gl, modalBtnX, modalBtnY, modalBtnW, modalBtnH, 0.25f, 0.35f, 0.55f);
        drawHUDBorder(gl, modalBtnX, modalBtnY, modalBtnW, modalBtnH, 0.50f, 0.70f, 1.0f);
        int btnTextWidth = glut.glutBitmapLength(MODAL_BUTTON_FONT, modalBotonTexto);
        drawHUDText(gl, modalBtnX + (modalBtnW - btnTextWidth) / 2, modalBtnY + (modalBtnH / 2) - 5, modalBotonTexto, MODAL_BUTTON_FONT);
    }

    private void mostrarModal(String titulo, String mensaje, String botonTexto, Runnable alConfirmar) {
        this.modalTitulo = titulo;
        this.modalLineas = mensaje.split("\\n");
        this.modalBotonTexto = botonTexto;
        this.modalAccionConfirmar = alConfirmar;
        this.modalActivo = true;
    }

    public void mostrarModalInformativo(String titulo, String mensaje) {
        mostrarModal(titulo, mensaje, "Aceptar", null);
    }

    // Comprueba si el clic del ratón cae sobre algún botón del HUD
    // mouseY en coordenadas AWT (0 = arriba)
    private boolean handleHUDClick(int mouseX, int mouseY) {
        int h = viewHeight;
        int oglY = h - mouseY; // Convertir AWT→OpenGL (y desde abajo)

        // ¿Está en la barra de botones inferior?
        if (oglY >= BTN_Y_OGL && oglY <= BTN_Y_OGL + BTN_H) {
            for (int i = 0; i < BTN_LABELS.length; i++) {
                if (mouseX >= btnX[i] && mouseX <= btnX[i] + BTN_W) {
                    if (interfazRef != null) {
                        switch (i) {
                            case 0: interfazRef.nuevaPartida(); break;
                            case 1: interfazRef.cargarPartida(); break;
                            case 2: interfazRef.guardarPartida(); break;
                            case 3: interfazRef.deshacer(); break;
                            case 4: interfazRef.reiniciarNivel(); break;
                            case 5: interfazRef.cerrarJuego(); break;
                        }
                    }
                    return true;
                }
            }
        }
        return false;
    }

    private boolean handleModalClick(int mouseX, int mouseY) {
        int oglY = viewHeight - mouseY;
        boolean clickEnBoton = mouseX >= modalBtnX && mouseX <= modalBtnX + modalBtnW
                && oglY >= modalBtnY && oglY <= modalBtnY + modalBtnH;
        if (!clickEnBoton) {
            return true;
        }
        Runnable accion = modalAccionConfirmar;
        modalActivo = false;
        modalAccionConfirmar = null;
        if (accion != null) {
            accion.run();
        }
        return true;
    }

    // Comprueba si el clic está en el área del HUD (no en la escena 3D)
    private boolean enZonaHUD(int mouseX, int mouseY) {
        int h = viewHeight;
        int oglY = h - mouseY;
        return oglY <= HUD_BOT_PX || oglY >= h - HUD_TOP_PX;
    }

    // ==================== LÓGICA DE SELECCIÓN DE VEHÍCULOS ====================

    private Vehiculo encontraVehiculo(Point p) {
        if (p == null) return null;
        int dondehagoclickx = p.x / TAMDECUADRADOS;
        int dondehagoclicky = p.y / TAMDECUADRADOS;
        for (Vehiculo v : vehiculos) {
            int coordx = v.getColumna();
            int coordy = v.getFila();
            int tamvehiculo = v.getTamano();
            if (v.getOrientacion() == Orientacion.HORIZONTAL) {
                if (dondehagoclicky == coordy && dondehagoclickx >= coordx && dondehagoclickx < coordx + tamvehiculo) {
                    return v;
                }
            } else {
                if (dondehagoclickx == coordx && dondehagoclicky >= coordy && dondehagoclicky < coordy + tamvehiculo) {
                    return v;
                }
            }
        }
        return null;
    }

    @Override
    public void mouseClicked(MouseEvent mouseEvent) {}

    @Override
    public void mousePressed(MouseEvent e) {
        if (modalActivo) {
            handleModalClick(e.getX(), e.getY());
            return;
        }
        // Primero comprobar si el clic es sobre el HUD
        if (enZonaHUD(e.getX(), e.getY())) {
            handleHUDClick(e.getX(), e.getY());
            return;
        }
        Point pointEnTablero = obtenerCoordenadasTablero(e.getX(), e.getY());
        vehiculoseleccionado = encontraVehiculo(pointEnTablero);
        if (vehiculoseleccionado != null && pointEnTablero != null) {
            movimientos.push(new Vehiculo(vehiculoseleccionado));
            dondeclick = new Point(pointEnTablero.x - vehiculoseleccionado.getColumna() * TAMDECUADRADOS,
                                   pointEnTablero.y - vehiculoseleccionado.getFila() * TAMDECUADRADOS);
            log.info("Haciendo click en vehiculo: " + vehiculoseleccionado.getId());
        } else {
            lastMousePosCam = new Point(e.getX(), e.getY());
        }
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        // Si no había vehículo seleccionado (arrastre de cámara o clic en HUD),
        // no contar como movimiento
        if (vehiculoseleccionado == null) {
            lastMousePosCam = null;
            return;
        }
        if (!movimientos.isEmpty()) {
            Vehiculo vehiculoAnterior = movimientos.peek();
            Point pointEnTablero = obtenerCoordenadasTablero(e.getX(), e.getY());
            Vehiculo vehiculoActual = encontraVehiculo(pointEnTablero);
            if (vehiculoActual != null && vehiculoActual.getFila() == vehiculoAnterior.getFila() &&
                vehiculoActual.getColumna() == vehiculoAnterior.getColumna()) {
                movimientos.pop();
            } else {
                incrementarPuntuacion();
                Interfaz.actualizarPuntuacion(puntuacion, 0, false, false, false);
                log.info("Movimiento guardado");
            }
        }
        if (!completado && cocheRojoEnSalida()) {
            procesarNivelCompletado();
        }
        vehiculoseleccionado = null;
    }

    @Override
    public void mouseEntered(MouseEvent mouseEvent) {}

    @Override
    public void mouseExited(MouseEvent mouseEvent) {}

    @Override
    public void mouseWheelMoved(MouseEvent e) {
        float[] rotation = e.getRotation();
        float delta = 0.0f;
        if (rotation != null && rotation.length > 1) {
            delta = rotation[1] * e.getRotationScale();
        }
        zoomCam -= delta;
        if (zoomCam > -5.0f)  zoomCam = -5.0f;
        if (zoomCam < -50.0f) zoomCam = -50.0f;
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        if (vehiculoseleccionado == null) {
            if (lastMousePosCam != null) {
                int dx = e.getX() - lastMousePosCam.x;
                int dy = e.getY() - lastMousePosCam.y;
                rotCamY += dx * 0.5f;
                rotCamX += dy * 0.5f;
                if (rotCamX < 10.0f) rotCamX = 10.0f;
                if (rotCamX > 85.0f) rotCamX = 85.0f;
                lastMousePosCam = new Point(e.getX(), e.getY());
            }
            return;
        }
        Point pointEnTablero = obtenerCoordenadasTablero(e.getX(), e.getY());
        if (pointEnTablero == null) return;
        int columnaActual = vehiculoseleccionado.getColumna();
        int filaActual    = vehiculoseleccionado.getFila();
        Orientacion orientacion = vehiculoseleccionado.getOrientacion();
        int nuevacolumna = columnaActual;
        int nuevafila    = filaActual;
        int offsetX = pointEnTablero.x - dondeclick.x;
        int offsetY = pointEnTablero.y - dondeclick.y;
        if (orientacion == Orientacion.HORIZONTAL) {
            nuevacolumna = offsetX / TAMDECUADRADOS;
        } else {
            nuevafila = offsetY / TAMDECUADRADOS;
        }
        Vehiculo prueba = new Vehiculo(vehiculoseleccionado);
        prueba.setColumna(nuevacolumna);
        prueba.setFila(nuevafila);
        if (posicionValida(prueba)) {
            actualizarTablero(vehiculoseleccionado, true);
            vehiculoseleccionado.setColumna(nuevacolumna);
            vehiculoseleccionado.setFila(nuevafila);
            actualizarTablero(vehiculoseleccionado, false);
            if (vehiculoseleccionado.getId().equals("*") && cocheRojoEnSalida() && !completado) {
                procesarNivelCompletado();
            }
        }
    }

    private void procesarNivelCompletado() {
        completado = true;
        log.info("Nivel completado!");
        incrementarPuntuacion();
        movimientos.clear();
        Puntuaciones puntosNivel = new Puntuaciones(this.nombreNivel, puntuacion);
        Interfaz.puntuacionTotal.add(puntosNivel);
        Interfaz.actualizarPuntuacion(puntuacion, 0, false, false, false);
        PanelJuego.setPuntuacion(0);
        mostrarModal(
                "Felicidades",
                this.nombreNivel + " completado!\nPuntuacion: " + puntuacion + " movimientos actuales."
                        + "\nPuntuación Total: " + Interfaz.sumaPuntuacionFinal(Interfaz.puntuacionTotal) + " movimientos totales.",
                "Siguiente Nivel",
                this::cargarSiguienteONotificarFin
        );
    }

    private void cargarSiguienteONotificarFin() {
        Interfaz interfaz = interfazRef;
        if (interfaz == null) {
            return;
        }
        File archivoActual = interfaz.getArchivoNivelActual();
        Juego siguienteJuego = null;
        try {
            siguienteJuego = CargarNivel.cargarSiguienteNivel(archivoActual);
        } catch (es.upm.pproject.parkingjam.exceptions.FormatoNivelException ex) {
            siguienteJuego = buscarSiguienteNivelValido(archivoActual, interfaz);
            if (siguienteJuego == null) {
                mostrarFinDelJuego();
                return;
            }
        }
        if (siguienteJuego == null) {
            mostrarFinDelJuego();
            return;
        }
        completado = false;
        int siguienteNivel = Integer.parseInt(siguienteJuego.getNombre().replaceAll("[^0-9]", ""));
        interfaz.setJuego(siguienteJuego);
        interfaz.setArchivoNivelActual(Paths.get("src", "main", "resources", "niveles", "level_" + siguienteNivel + ".txt").toFile());
        interfaz.recargarPanelJuego();
        try {
            Interfaz.actualizarTituloDesdeArchivo(Paths.get("src", "main", "resources", "niveles", "level_" + siguienteNivel + ".txt").toFile());
        } catch (IOException ex) {
            log.error("No se pudo actualizar el título: " + ex.getMessage());
        }
    }

    private Juego buscarSiguienteNivelValido(File archivoActual, Interfaz interfaz) {
        int nivel = Integer.parseInt(archivoActual.getName().replaceAll("\\D", ""));
        while (true) {
            nivel++;
            File siguienteArchivo = new File("src/main/resources/niveles/level_" + nivel + ".txt");
            if (!siguienteArchivo.exists()) {
                return null;
            }
            try {
                Juego siguienteJuego = CargarNivel.cargarNivel(siguienteArchivo);
                interfaz.setArchivoNivelActual(siguienteArchivo);
                return siguienteJuego;
            } catch (es.upm.pproject.parkingjam.exceptions.FormatoNivelException ex) { /* intentar siguiente */ }
        }
    }

    private void mostrarFinDelJuego() {
        StringBuilder mensaje = new StringBuilder();
        mensaje.append("Puntuaciones:\n");
        for (Puntuaciones entry : Interfaz.puntuacionTotal) {
            mensaje.append("- ").append(entry.getNombre())
                    .append(": ").append(entry.getPuntuacion()).append(" movimientos\n");
        }
        mensaje.append("\nPuntuación Total: ")
                .append(Interfaz.sumaPuntuacionFinal(Interfaz.puntuacionTotal))
                .append(" movimientos.");
        mostrarModal("Felicidades!", "Has completado todos los niveles!\n" + mensaje, "Aceptar", null);
    }

    private boolean cocheRojoEnSalida() {
        Vehiculo cocheRojo = null;
        for (Vehiculo v : vehiculos) {
            if (v.getId().equals("*")) {
                cocheRojo = v;
                break;
            }
        }
        if (cocheRojo == null) return false;
        Posicion salida = tablero.getSalida();
        if (cocheRojo.getOrientacion() == Orientacion.HORIZONTAL) {
            int extremoDerecho = cocheRojo.getColumna() + cocheRojo.getTamano() - 1;
            if (cocheRojo.getFila() == salida.getFila() && extremoDerecho == salida.getColumna()) return true;
        } else {
            int extremoInferior = cocheRojo.getFila() + cocheRojo.getTamano() - 1;
            if (cocheRojo.getColumna() == salida.getColumna() && extremoInferior == salida.getFila()) return true;
        }
        return false;
    }

    private void actualizarTablero(Vehiculo vehiculo, boolean borrar) {
        char[][] celdas = tablero.getCeldas();
        int fila    = vehiculo.getFila();
        int columna = vehiculo.getColumna();
        int tamano  = vehiculo.getTamano();
        char marca  = borrar ? ' ' : vehiculo.getId().charAt(0);
        if (vehiculo.getOrientacion() == Orientacion.HORIZONTAL) {
            for (int c = columna; c < columna + tamano; c++) celdas[fila][c] = marca;
        } else {
            for (int f = fila; f < fila + tamano; f++) celdas[f][columna] = marca;
        }
    }

    private boolean posicionValida(Vehiculo vehiculo) {
        if (vehiculo.getColumna() < 0 || vehiculo.getFila() < 0 ||
                vehiculo.getColumna() + (vehiculo.getOrientacion() == Orientacion.HORIZONTAL ? vehiculo.getTamano() : 1) > tablero.getColumnas() ||
                vehiculo.getFila() + (vehiculo.getOrientacion() == Orientacion.VERTICAL ? vehiculo.getTamano() : 1) > tablero.getFilas()) {
            return false;
        }
        Vehiculo vehiculoOriginal = null;
        for (Vehiculo v : vehiculos) {
            if (v.getId().equals(vehiculo.getId())) {
                vehiculoOriginal = v;
                break;
            }
        }
        if (vehiculoOriginal == null) return false;
        char[][] celdas = tablero.getCeldas();
        if (vehiculo.getOrientacion() == Orientacion.HORIZONTAL) {
            int inicioCol, finCol;
            if (vehiculo.getColumna() > vehiculoOriginal.getColumna()) {
                inicioCol = vehiculoOriginal.getColumna() + vehiculoOriginal.getTamano();
                finCol    = vehiculo.getColumna() + vehiculo.getTamano() - 1;
            } else {
                inicioCol = vehiculo.getColumna();
                finCol    = vehiculoOriginal.getColumna() - 1;
            }
            for (int c = inicioCol; c <= finCol; c++) {
                if (c >= 0 && c < tablero.getColumnas()) {
                    char celda = celdas[vehiculo.getFila()][c];
                    if ((celda == '@' && vehiculo.getId().equals("*"))) continue;
                    if (celda == '+' || Character.isLetter(celda) || celda == '*' || celda == '@') return false;
                }
            }
        } else {
            int inicioFila, finFila;
            if (vehiculo.getFila() > vehiculoOriginal.getFila()) {
                inicioFila = vehiculoOriginal.getFila() + vehiculoOriginal.getTamano();
                finFila    = vehiculo.getFila() + vehiculo.getTamano() - 1;
            } else {
                inicioFila = vehiculo.getFila();
                finFila    = vehiculoOriginal.getFila() - 1;
            }
            for (int f = inicioFila; f <= finFila; f++) {
                if (f >= 0 && f < tablero.getFilas()) {
                    char celda = celdas[f][vehiculo.getColumna()];
                    if ((celda == '@' && vehiculo.getId().equals("*"))) continue;
                    if (celda == '+' || Character.isLetter(celda) || celda == '*' || celda == '@') return false;
                }
            }
        }
        if (vehiculo.getOrientacion() == Orientacion.HORIZONTAL) {
            for (int c = vehiculo.getColumna(); c < vehiculo.getColumna() + vehiculo.getTamano(); c++) {
                char celda = celdas[vehiculo.getFila()][c];
                if (celda == '+' || (Character.isLetter(celda) && celda != vehiculo.getId().charAt(0)) || (celda == '*' && !vehiculo.getId().equals("*"))) return false;
                if (celda == '@' && !vehiculo.getId().equals("*")) return false;
            }
        } else {
            for (int f = vehiculo.getFila(); f < vehiculo.getFila() + vehiculo.getTamano(); f++) {
                char celda = celdas[f][vehiculo.getColumna()];
                if (celda == '+' || (Character.isLetter(celda) && celda != vehiculo.getId().charAt(0)) || (celda == '*' && !vehiculo.getId().equals("*"))) return false;
                if (celda == '@' && !vehiculo.getId().equals("*")) return false;
            }
        }
        return true;
    }

    public void undo() {
        if (!movimientos.isEmpty()) {
            Vehiculo estadoAnterior = movimientos.pop();
            for (Vehiculo v : vehiculos) {
                if (v.getId().equals(estadoAnterior.getId())) {
                    actualizarTablero(v, true);
                    v.setFila(estadoAnterior.getFila());
                    v.setColumna(estadoAnterior.getColumna());
                    actualizarTablero(v, false);
                    break;
                }
            }
            log.info("Se ha deshecho un movimiento");
        } else {
            log.info("No se pueden deshacer mas movimientos");
        }
    }

    @Override
    public void mouseMoved(MouseEvent mouseEvent) {}

    private class Particula {
        float x, y, z;
        float life;
        float maxLife;
        float vx, vy, vz;
        public Particula(float x, float y, float z) {
            this.x = x; this.y = y; this.z = z;
            this.maxLife = 20 + (float) Math.random() * 15;
            this.life = this.maxLife;
            this.vx = (float) (Math.random() - 0.5) * 0.015f;
            this.vy = 0.02f + (float) Math.random() * 0.02f;
            this.vz = (float) (Math.random() - 0.5) * 0.015f;
        }
    }
}
