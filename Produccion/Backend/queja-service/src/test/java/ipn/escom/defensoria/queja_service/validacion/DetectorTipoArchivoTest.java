package ipn.escom.defensoria.queja_service.validacion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.web.multipart.MultipartFile;

@DisplayName("DetectorTipoArchivo — tipo real por firma binaria")
class DetectorTipoArchivoTest {

    static Stream<Arguments> firmas() {
        return Stream.of(
                Arguments.of(ArchivosPrueba.JPEG, TipoArchivoDetectado.JPEG),
                Arguments.of(ArchivosPrueba.PNG, TipoArchivoDetectado.PNG),
                Arguments.of(ArchivosPrueba.PDF, TipoArchivoDetectado.PDF),
                Arguments.of(ArchivosPrueba.MP4, TipoArchivoDetectado.MP4),
                Arguments.of(ArchivosPrueba.MP3_ID3, TipoArchivoDetectado.MP3),
                Arguments.of(ArchivosPrueba.MP3_FRAME, TipoArchivoDetectado.MP3),
                Arguments.of(ArchivosPrueba.EXE, TipoArchivoDetectado.DESCONOCIDO));
    }

    @ParameterizedTest(name = "{1}")
    @MethodSource("firmas")
    void detectar_segunFirma_regresaTipoCorrecto(byte[] firma, TipoArchivoDetectado esperado) {
        assertEquals(esperado, DetectorTipoArchivo.detectar(ArchivosPrueba.archivo("x.bin", firma)),
                "El tipo se decide por los primeros bytes");
    }

    @Test
    void detectar_ejecutableRenombradoComoJpg_noEsImagen() {
        TipoArchivoDetectado tipo = DetectorTipoArchivo.detectar(ArchivosPrueba.archivo("foto.jpg", ArchivosPrueba.EXE));
        assertFalse(tipo.esImagen(), "Cambiar la extensión no convierte el archivo en imagen");
    }

    @Test
    void detectar_menosDeTresBytes_esDesconocido() {
        byte[] dosBytes = {(byte) 0xFF, (byte) 0xD8};
        assertEquals(TipoArchivoDetectado.DESCONOCIDO, DetectorTipoArchivo.detectar(ArchivosPrueba.archivo("a", dosBytes)),
                "Con menos de 3 bytes no se puede reconocer la firma");
    }

    @Test
    void detectar_errorDeLectura_lanzaValidacionException() throws IOException {
        MultipartFile roto = mock(MultipartFile.class);
        when(roto.getInputStream()).thenThrow(new IOException("disco"));
        when(roto.getOriginalFilename()).thenReturn("roto.pdf");

        ValidacionException ex = assertThrows(ValidacionException.class, () -> DetectorTipoArchivo.detectar(roto));
        assertTrue(ex.getMessage().contains("roto.pdf"), "El mensaje debe nombrar el archivo");
    }

    @Test
    void tipos_soloJpegYPngSonImagen() {
        assertTrue(TipoArchivoDetectado.JPEG.esImagen(), "JPEG es imagen");
        assertTrue(TipoArchivoDetectado.PNG.esImagen(), "PNG es imagen");
        assertFalse(TipoArchivoDetectado.PDF.esImagen(), "PDF no es imagen");
        assertEquals("application/pdf", TipoArchivoDetectado.PDF.mime(), "MIME del PDF");
    }
}
