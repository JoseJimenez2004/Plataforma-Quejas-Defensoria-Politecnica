package ipn.escom.defensoria.queja_service.validacion;

import java.time.LocalDate;

/**
 * Reglas de negocio de CU-Q01 (registro público de quejas) en un solo lugar.
 *
 * Todos estos valores son la FUENTE DE VERDAD: el frontend replica los mismos límites para
 * dar retroalimentación inmediata, pero el servidor es quien realmente decide. Si un valor
 * cambia aquí, hay que cambiarlo también en el frontend
 * (Frontend/src/app/core/validaciones/reglas-queja.ts).
 */
public final class ReglasQueja {

    private ReglasQueja() {
    }

    // ---- Nombres y apellidos -------------------------------------------------------------
    /**
     * Letras (incluidos acentos y ñ), permitiendo espacio interno, apóstrofe y guion entre
     * palabras. Deliberadamente NO es "solo A-Z": eso rechazaría nombres reales como
     * "María José", "D'Angelo" o "Pérez-Gómez".
     */
    public static final String REGEX_NOMBRE =
            "^[A-Za-zÁÉÍÓÚÜÑáéíóúüñ]+(?:[ '\\-][A-Za-zÁÉÍÓÚÜÑáéíóúüñ]+)*$";
    public static final int NOMBRE_LONGITUD_MINIMA = 2;
    public static final int NOMBRE_LONGITUD_MAXIMA = 50;

    // ---- Boleta / número de empleado ----------------------------------------------------
    /** Solo dígitos, máximo 10. Se guarda como texto: los ceros a la izquierda importan. */
    public static final String REGEX_NUMERO_IDENTIFICACION = "^\\d{1,10}$";
    public static final int NUMERO_IDENTIFICACION_LONGITUD_MAXIMA = 10;

    // ---- Fechas --------------------------------------------------------------------------
    public static final LocalDate FECHA_NACIMIENTO_MINIMA = LocalDate.of(1920, 1, 1);
    /** Zona con la que se calcula "hoy" y "el año en curso" — no la del servidor por default. */
    public static final String ZONA_HORARIA = "America/Mexico_City";

    // ---- Descripción ---------------------------------------------------------------------
    public static final int DESCRIPCION_LONGITUD_MINIMA = 20;
    public static final int DESCRIPCION_LONGITUD_MAXIMA = 4000;

    // ---- Archivos ------------------------------------------------------------------------
    /**
     * Prefijo con el que el frontend marca la credencial oficial dentro de la lista de
     * archivos. Es lo que permite aplicarle reglas más estrictas que al resto de evidencias
     * sin cambiar el contrato multipart del endpoint.
     */
    public static final String PREFIJO_IDENTIFICACION = "IDENTIFICACION_";

    /** Credencial oficial: solo imágenes, 3 MB máximo, hasta 2 archivos. Ya NO se acepta PDF. */
    public static final long IDENTIFICACION_TAMANIO_MAXIMO = 3L * 1024 * 1024;
    public static final int IDENTIFICACION_CANTIDAD_MAXIMA = 2;
    public static final int IDENTIFICACION_CANTIDAD_MINIMA = 1;

    /** Evidencias de la queja: se mantienen los tipos y el tope de 30 MB que ya existían. */
    public static final long EVIDENCIA_TAMANIO_MAXIMO = 30L * 1024 * 1024;

    // ---- Aviso de privacidad -------------------------------------------------------------
    /** Versión vigente del aviso. Al cambiar el texto hay que subir este número. */
    public static final String AVISO_PRIVACIDAD_VERSION = "1.0";
}
