package ipn.escom.defensoria.denunciado_service.validacion;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

import org.springframework.web.multipart.MultipartFile;

/** Misma detección por firma binaria que queja-service: renombrar un .exe a .jpg no basta. */
public final class DetectorTipoArchivo {

    private static final int BYTES_CABECERA = 12;
    private static final byte[] FIRMA_JPEG = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF};
    private static final byte[] FIRMA_PNG = {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};
    private static final byte[] FIRMA_PDF = {0x25, 0x50, 0x44, 0x46};
    private static final byte[] FIRMA_FTYP = {0x66, 0x74, 0x79, 0x70};
    private static final byte[] FIRMA_ID3 = {0x49, 0x44, 0x33};

    private DetectorTipoArchivo() {
    }

    public static TipoArchivoDetectado detectar(MultipartFile archivo) {
        byte[] c = leerCabecera(archivo);
        if (c.length < 3) {
            return TipoArchivoDetectado.DESCONOCIDO;
        }
        if (coincide(c, 0, FIRMA_JPEG)) {
            return TipoArchivoDetectado.JPEG;
        }
        if (coincide(c, 0, FIRMA_PNG)) {
            return TipoArchivoDetectado.PNG;
        }
        if (coincide(c, 0, FIRMA_PDF)) {
            return TipoArchivoDetectado.PDF;
        }
        if (c.length >= 8 && coincide(c, 4, FIRMA_FTYP)) {
            return TipoArchivoDetectado.MP4;
        }
        if (coincide(c, 0, FIRMA_ID3) || ((c[0] & 0xFF) == 0xFF && (c[1] & 0xE0) == 0xE0)) {
            return TipoArchivoDetectado.MP3;
        }
        return TipoArchivoDetectado.DESCONOCIDO;
    }

    private static byte[] leerCabecera(MultipartFile archivo) {
        try (InputStream entrada = archivo.getInputStream()) {
            return entrada.readNBytes(BYTES_CABECERA);
        } catch (IOException e) {
            throw new ValidacionException("No se pudo leer el archivo \"" + archivo.getOriginalFilename() + "\".");
        }
    }

    private static boolean coincide(byte[] datos, int desde, byte[] firma) {
        return datos.length >= desde + firma.length
                && Arrays.equals(datos, desde, desde + firma.length, firma, 0, firma.length);
    }
}
