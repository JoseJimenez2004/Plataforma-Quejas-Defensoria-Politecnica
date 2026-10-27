package ipn.escom.defensoria.queja_service.validacion;

import java.util.Arrays;

import org.springframework.mock.web.MockMultipartFile;

/** Fábrica de archivos de prueba con la firma binaria (magic bytes) de cada tipo. */
final class ArchivosPrueba {

    static final byte[] JPEG = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0, 0x10, 'J', 'F', 'I', 'F', 0, 1};
    static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 0, 0, 0, 0x0D};
    static final byte[] PDF = {'%', 'P', 'D', 'F', '-', '1', '.', '7', 0x0A, 0, 0, 0};
    static final byte[] MP4 = {0, 0, 0, 0x18, 'f', 't', 'y', 'p', 'm', 'p', '4', '2'};
    static final byte[] MP3_ID3 = {'I', 'D', '3', 3, 0, 0, 0, 0, 0, 0, 0, 0};
    static final byte[] MP3_FRAME = {(byte) 0xFF, (byte) 0xFB, (byte) 0x90, 0x64, 0, 0, 0, 0, 0, 0, 0, 0};
    static final byte[] EXE = {'M', 'Z', (byte) 0x90, 0, 3, 0, 0, 0, 4, 0, 0, 0};

    private ArchivosPrueba() {
    }

    static MockMultipartFile archivo(String nombre, byte[] firma) {
        return new MockMultipartFile("archivos", nombre, "application/octet-stream", firma);
    }

    /** Archivo con la firma indicada y relleno hasta el tamaño pedido. */
    static MockMultipartFile archivoDeTamanio(String nombre, byte[] firma, long tamanio) {
        byte[] contenido = Arrays.copyOf(firma, Math.toIntExact(tamanio));
        return new MockMultipartFile("archivos", nombre, "application/octet-stream", contenido);
    }

    static MockMultipartFile identificacion(String nombre, byte[] firma) {
        return archivo(ReglasQueja.PREFIJO_IDENTIFICACION + nombre, firma);
    }
}
