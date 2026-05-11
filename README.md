# Parking Jam

Juego de puzzles y aparcamiento desarrollado en Java con Maven y JOGL.

## Estructura mínima del proyecto

La copia del proyecto que se va a entregar solo necesita incluir:

- `pom.xml`
- `src/` con todo su contenido

No es necesario incluir `target/` ni otros archivos generados.

## Punto de entrada

La clase principal del proyecto es `Interfaz.java`.

### Ejecutar desde el IDE

Abre `src/main/java/es/upm/pproject/parkingjam/view/Interfaz.java` y ejecútala como clase principal.

### Ejecutar con Maven

Desde la carpeta raíz del proyecto:

```bash
mvn exec:java
```

Si tu IDE no detecta automáticamente la configuración del plugin, también puedes indicar la clase principal manualmente:

```bash
mvn exec:java -Dexec.mainClass="es.upm.pproject.parkingjam.view.Interfaz"
```

## Controles

- Usa el ratón para interactuar con el tablero.
- El botón izquierdo permite seleccionar y arrastrar vehículos.

## Niveles

Los niveles se cargan desde ficheros de texto ubicados en `src/main/resources/niveles/`.

Cada nivel incluye:

- el nombre o título del nivel,
- las dimensiones del tablero,
- y la representación del mapa con paredes, vehículos y salida.

## Autor

- Adrián Sacks Nogal

## Notas

- El proyecto está preparado para ejecutarse con Java 11 o superior.
- Las dependencias se gestionan con Maven.
