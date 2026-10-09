package ipn.escom.defensoria.queja_service.validacion;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

import org.springframework.web.multipart.MultipartFile;

/**
 * Identifica el tipo real de un archivo leyendo su firma binaria (magic bytes).
 *
 * Solo se leen los primeros bytes del stream, no el archivo completo: el contenido íntegro se
 * lee después, una sola vez, al convertirlo en evidencia.
 */
public final class DetectorTipoArchivo {

    private static final int BYTES_CABECERA = 12;

    private static final byte[] FIRMA_JPEG = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF};
    private static final byte[] FIRMA_PNG =
            {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};
    private static final byte[] FIRMA_PDF = {0x25, 0x50, 0x44, 0x46};          // %PDF
    private static final byte[] FIRMA_FTYP = {0x66, 0x74, 0x79, 0x70};         // "ftyp" en 4..8
    private static final byte[] FIRMA_ID3 = {0x49, 0x44, 0x33};                // ID3

    private DetectorTipoArchivo() {
    }

    public static TipoArchivoDetectado detectar(MultipartFile archivo) {
        byte[] cabecera = leerCabecera(archivo);
        if (cabecera.length < 3) {
            return TipoArchivoDetectado.DESCONOCIDO;
        }
        if (empiezaCon(cabecera, FIRMA_JPEG)) {
            return TipoArchivoDetectado.JPEG;
        }
        if (empiezaCon(cabecera, FIRMA_PNG)) {
            return TipoArchivoDetectado.PNG;
        }
        if (empiezaCon(cabecera, FIRMA_PDF)) {
            return TipoArchivoDetectado.PDF;
        }
        if (cabecera.length >= 8 && coincideEn(cabecera, 4, FIRMA_FTYP)) {
            return TipoArchivoDetectado.MP4;
        }
        if (esMp3(cabecera)) {
            return TipoArchivoDetectado.MP3;
        }
        return TipoArchivoDetectado.DESCONOCIDO;
    }

    /** MP3 con etiqueta ID3, o frame MPEG crudo (0xFF seguido de 0xE0 en los 3 bits altos). */
    private static boolean esMp3(byte[] cabecera) {
        if (empiezaCon(cabecera, FIRMA_ID3)) {
            return true;
        }
        return (cabecera[0] & 0xFF) == 0xFF && (cabecera[1] & 0xE0) == 0xE0;
    }

    private static byte[] leerCabecera(MultipartFile archivo) {
        try (InputStream entrada = archivo.getInputStream()) {
            return entrada.readNBytes(BYTES_CABECERA);
        } catch (IOException e) {
            throw new ValidacionException(
                    "No se pudo leer el archivo \"" + archivo.getOriginalFilename() + "\".");
        }
    }

    private static boolean empiezaCon(byte[] datos, byte[] firma) {
        return coincideEn(datos, 0, firma);
    }

    private static boolean coincideEn(byte[] datos, int desde, byte[] firma) {
        if (datos.length < desde + firma.length) {
            return false;
        }
        return Arrays.equals(datos, desde, desde + firma.length, firma, 0, firma.length);
    }
}
