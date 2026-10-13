package ipn.escom.defensoria.queja_service.validacion;

import java.util.ArrayList;
import java.util.List;

import org.springframework.web.multipart.MultipartFile;

/**
 * Valida los archivos que llegan con una queja pública.
 *
 * Se distinguen dos grupos por el prefijo del nombre (ver ReglasQueja.PREFIJO_IDENTIFICACION):
 *
 *  - Credencial oficial: SOLO imágenes JPG/PNG, máximo 3 MB cada una, entre 1 y 2 archivos.
 *    Ya no se acepta PDF.
 *  - Resto de evidencias: se conservan los tipos que ya se aceptaban (PDF, JPG, PNG, MP4,
 *    MP3) con el tope de 30 MB por archivo.
 *
 * En ambos casos el tipo se determina por los primeros bytes del archivo, no por la
 * extensión: renombrar "script.exe" a "foto.jpg" no lo convierte en imagen y este endpoint es
 * público (sin sesión), así que es el único control real que hay.
 */
public final class ValidadorArchivos {

    private ValidadorArchivos() {
    }

    /** Resultado de separar los archivos recibidos en sus dos grupos. */
    public record ArchivosClasificados(List<MultipartFile> identificaciones,
                                       List<MultipartFile> evidencias) {
    }

    public static ArchivosClasificados validarYClasificar(List<MultipartFile> archivos) {
        List<MultipartFile> identificaciones = new ArrayList<>();
        List<MultipartFile> evidencias = new ArrayList<>();

        for (MultipartFile archivo : archivos == null ? List.<MultipartFile>of() : archivos) {
            if (archivo == null || archivo.isEmpty()) {
                continue;
            }
            if (esIdentificacion(archivo)) {
                identificaciones.add(archivo);
            } else {
                evidencias.add(archivo);
            }
        }

        validarIdentificaciones(identificaciones);
        evidencias.forEach(ValidadorArchivos::validarEvidencia);

        return new ArchivosClasificados(identificaciones, evidencias);
    }

    /**
     * Valida archivos que se agregan a una queja YA registrada (CU-Q07). A diferencia del
     * registro inicial, aquí no se exige la credencial: esa ya se entregó al presentar la
     * queja. Solo se comprueban tipo y tamaño de cada evidencia nueva.
     */
    public static void validarEvidenciasAdicionales(List<MultipartFile> archivos) {
        if (archivos == null || archivos.stream().allMatch(a -> a == null || a.isEmpty())) {
            throw new ValidacionException("No se recibió ningún archivo.");
        }
        for (MultipartFile archivo : archivos) {
            if (archivo == null || archivo.isEmpty()) {
                continue;
            }
            if (esIdentificacion(archivo)) {
                throw new ValidacionException(
                        "La identificación oficial solo se puede adjuntar al presentar la queja.");
            }
            validarEvidencia(archivo);
        }
    }

    public static boolean esIdentificacion(MultipartFile archivo) {
        String nombre = archivo.getOriginalFilename();
        return nombre != null && nombre.startsWith(ReglasQueja.PREFIJO_IDENTIFICACION);
    }

    private static void validarIdentificaciones(List<MultipartFile> identificaciones) {
        if (identificaciones.size() < ReglasQueja.IDENTIFICACION_CANTIDAD_MINIMA) {
            throw new ValidacionException(
                    "Adjunta tu identificación oficial de la comunidad politécnica.");
        }
        if (identificaciones.size() > ReglasQueja.IDENTIFICACION_CANTIDAD_MAXIMA) {
            throw new ValidacionException("Solo puedes adjuntar hasta "
                    + ReglasQueja.IDENTIFICACION_CANTIDAD_MAXIMA
                    + " imágenes de tu identificación oficial.");
        }
        identificaciones.forEach(ValidadorArchivos::validarIdentificacion);
    }

    private static void validarIdentificacion(MultipartFile archivo) {
        String nombre = nombreVisible(archivo);

        if (archivo.getSize() > ReglasQueja.IDENTIFICACION_TAMANIO_MAXIMO) {
            throw new ValidacionException("La imagen \"" + nombre + "\" pesa "
                    + formatear(archivo.getSize())
                    + ". La identificación oficial no puede exceder 3 MB.");
        }

        TipoArchivoDetectado tipo = DetectorTipoArchivo.detectar(archivo);
        if (!tipo.esImagen()) {
            throw new ValidacionException("La identificación oficial debe ser una imagen JPG o PNG. "
                    + "El archivo \"" + nombre + "\" no lo es.");
        }
    }

    private static void validarEvidencia(MultipartFile archivo) {
        String nombre = nombreVisible(archivo);

        if (archivo.getSize() > ReglasQueja.EVIDENCIA_TAMANIO_MAXIMO) {
            throw new ValidacionException("El archivo \"" + nombre + "\" pesa "
                    + formatear(archivo.getSize()) + ". El máximo por archivo es 30 MB.");
        }

        TipoArchivoDetectado tipo = DetectorTipoArchivo.detectar(archivo);
        if (tipo == TipoArchivoDetectado.DESCONOCIDO) {
            throw new ValidacionException("El archivo \"" + nombre
                    + "\" no es de un tipo permitido. Se aceptan PDF, JPG, PNG, MP4 y MP3.");
        }
    }

    /** Nombre sin el prefijo interno, para que el mensaje de error sea el que el usuario ve. */
    private static String nombreVisible(MultipartFile archivo) {
        String nombre = archivo.getOriginalFilename();
        if (nombre == null) {
            return "archivo";
        }
        return nombre.startsWith(ReglasQueja.PREFIJO_IDENTIFICACION)
                ? nombre.substring(ReglasQueja.PREFIJO_IDENTIFICACION.length())
                : nombre;
    }

    private static String formatear(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        }
        if (bytes < 1024 * 1024) {
            return String.format("%.1f KB", bytes / 1024.0);
        }
        return String.format("%.1f MB", bytes / (1024.0 * 1024.0));
    }
}
