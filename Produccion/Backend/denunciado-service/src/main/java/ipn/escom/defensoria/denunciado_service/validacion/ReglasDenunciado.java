package ipn.escom.defensoria.denunciado_service.validacion;

/**
 * Reglas de la respuesta del denunciado. Son las MISMAS que las del quejoso
 * (queja-service/validacion/ReglasQueja) para que ambas partes se capturen igual.
 * Espejo en el frontend: Frontend/src/app/core/validaciones/reglas-queja.ts.
 */
public final class ReglasDenunciado {

    private ReglasDenunciado() {
    }

    /** Folio de la queja: FOL- seguido de 8 letras mayúsculas o dígitos. */
    public static final String REGEX_FOLIO_QUEJA = "^FOL-[A-Z0-9]{8}$";

    public static final String REGEX_NOMBRE =
            "^[A-Za-zÁÉÍÓÚÜÑáéíóúüñ]+(?:[ '\\-][A-Za-zÁÉÍÓÚÜÑáéíóúüñ]+)*$";
    public static final int NOMBRE_LONGITUD_MINIMA = 2;
    public static final int NOMBRE_LONGITUD_MAXIMA = 50;

    public static final String TIPO_ALUMNO = "ALUMNO";
    public static final String TIPO_EMPLEADO = "EMPLEADO";
    public static final String REGEX_NUMERO_IDENTIFICACION = "^\\d{1,10}$";

    public static final int CLAVE_UNIDAD_LONGITUD_MAXIMA = 40;

    public static final int DESCRIPCION_LONGITUD_MINIMA = 20;
    public static final int DESCRIPCION_LONGITUD_MAXIMA = 4000;

    /** Credencial: solo JPG/PNG, 3 MB, 1 o 2 imágenes (frente y reverso). */
    public static final long CREDENCIAL_TAMANIO_MAXIMO = 3L * 1024 * 1024;
    public static final int CREDENCIAL_CANTIDAD_MINIMA = 1;
    public static final int CREDENCIAL_CANTIDAD_MAXIMA = 2;

    /** Evidencias: PDF/JPG/PNG/MP4/MP3, 30 MB cada una, hasta 10, 95 MB entre todas. */
    public static final long EVIDENCIA_TAMANIO_MAXIMO = 30L * 1024 * 1024;
    public static final int EVIDENCIA_CANTIDAD_MAXIMA = 10;
    public static final long EVIDENCIA_TAMANIO_TOTAL_MAXIMO = 95L * 1024 * 1024;

    public static final String AVISO_PRIVACIDAD_VERSION = "1.0";
}
