package ipn.escom.defensoria.subdefensoria.service;

import ipn.escom.defensoria.subdefensoria.exception.OperacionInvalidaException;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/**
 * Valida el archivo de la respuesta del director: PDF, JPG o PNG según su firma real (no la
 * extensión ni lo que diga el navegador), y de máximo 10 MB.
 */
final class ArchivoRespuestaValidador {

    static final long TAMANO_MAXIMO = 10L * 1024 * 1024;

    private ArchivoRespuestaValidador() {
    }

    /** Devuelve el contenido y su tipo real; lanza OperacionInvalidaException si no sirve. */
    static Validado validar(MultipartFile archivo) {
        if (archivo == null || archivo.isEmpty()) {
            throw new OperacionInvalidaException("El archivo está vacío.");
        }
        if (archivo.getSize() > TAMANO_MAXIMO) {
            throw new OperacionInvalidaException("El archivo excede el tamaño máximo de 10 MB.");
        }
        byte[] contenido;
        try {
            contenido = archivo.getBytes();
        } catch (IOException ex) {
            throw new OperacionInvalidaException("No se pudo leer el archivo.");
        }
        String tipo = tipoReal(contenido);
        if (tipo == null) {
            throw new OperacionInvalidaException("Solo se aceptan archivos PDF, JPG o PNG.");
        }
        String nombre = archivo.getOriginalFilename() == null || archivo.getOriginalFilename().isBlank()
                ? "respuesta"
                : archivo.getOriginalFilename().replaceAll("[\\\\/:*?\"<>|]", "_");
        return new Validado(nombre.length() > 255 ? nombre.substring(0, 255) : nombre, tipo, contenido);
    }

    record Validado(String nombre, String tipo, byte[] contenido) {
    }

    private static String tipoReal(byte[] b) {
        if (b.length >= 4 && b[0] == 0x25 && b[1] == 0x50 && b[2] == 0x44 && b[3] == 0x46) {
            return "application/pdf";
        }
        if (b.length >= 3 && (b[0] & 0xFF) == 0xFF && (b[1] & 0xFF) == 0xD8 && (b[2] & 0xFF) == 0xFF) {
            return "image/jpeg";
        }
        if (b.length >= 8 && (b[0] & 0xFF) == 0x89 && b[1] == 0x50 && b[2] == 0x4E && b[3] == 0x47
                && b[4] == 0x0D && b[5] == 0x0A && b[6] == 0x1A && b[7] == 0x0A) {
            return "image/png";
        }
        return null;
    }
}
