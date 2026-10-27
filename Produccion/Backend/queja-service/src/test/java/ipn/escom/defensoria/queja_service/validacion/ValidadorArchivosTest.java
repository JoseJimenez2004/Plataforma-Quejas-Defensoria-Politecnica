package ipn.escom.defensoria.queja_service.validacion;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.multipart.MultipartFile;

@DisplayName("ValidadorArchivos — credencial oficial y evidencias")
class ValidadorArchivosTest {

    private static final long LIMITE_IDENTIFICACION = ReglasQueja.IDENTIFICACION_TAMANIO_MAXIMO;
    private static final long LIMITE_EVIDENCIA = ReglasQueja.EVIDENCIA_TAMANIO_MAXIMO;

    @Test
    void validarYClasificar_unaIdentificacionYUnaEvidencia_separaEnDosGrupos() {
        List<MultipartFile> archivos = List.of(
                ArchivosPrueba.identificacion("ine.jpg", ArchivosPrueba.JPEG),
                ArchivosPrueba.archivo("acta.pdf", ArchivosPrueba.PDF));

        ValidadorArchivos.ArchivosClasificados r = ValidadorArchivos.validarYClasificar(archivos);

        assertEquals(1, r.identificaciones().size(), "Debe haber una identificación");
        assertEquals(1, r.evidencias().size(), "Debe haber una evidencia");
    }

    @Test
    void validarYClasificar_sinIdentificacion_lanzaExcepcion() {
        List<MultipartFile> soloEvidencia = List.of(ArchivosPrueba.archivo("acta.pdf", ArchivosPrueba.PDF));
        ValidacionException ex = assertThrows(ValidacionException.class,
                () -> ValidadorArchivos.validarYClasificar(soloEvidencia));
        assertEquals("Adjunta tu identificación oficial de la comunidad politécnica.", ex.getMessage(),
                "La credencial es obligatoria");
    }

    @Test
    void validarYClasificar_listaNula_lanzaFaltaIdentificacion() {
        assertThrows(ValidacionException.class, () -> ValidadorArchivos.validarYClasificar(null),
                "Sin archivos no hay identificación");
    }

    @Test
    void validarYClasificar_archivosVaciosSeIgnoran() {
        List<MultipartFile> archivos = new ArrayList<>();
        archivos.add(null);
        archivos.add(ArchivosPrueba.archivo("vacio.pdf", new byte[0]));
        archivos.add(ArchivosPrueba.identificacion("ine.png", ArchivosPrueba.PNG));

        ValidadorArchivos.ArchivosClasificados r = ValidadorArchivos.validarYClasificar(archivos);

        assertTrue(r.evidencias().isEmpty(), "Los archivos nulos o vacíos no cuentan como evidencia");
    }

    @Test
    void validarYClasificar_dosIdentificaciones_esElMaximoPermitido() {
        List<MultipartFile> archivos = List.of(
                ArchivosPrueba.identificacion("frente.jpg", ArchivosPrueba.JPEG),
                ArchivosPrueba.identificacion("reverso.jpg", ArchivosPrueba.JPEG));
        assertDoesNotThrow(() -> ValidadorArchivos.validarYClasificar(archivos), "2 imágenes es el borde válido");
    }

    @Test
    void validarYClasificar_tresIdentificaciones_lanzaExcepcion() {
        List<MultipartFile> archivos = List.of(
                ArchivosPrueba.identificacion("1.jpg", ArchivosPrueba.JPEG),
                ArchivosPrueba.identificacion("2.jpg", ArchivosPrueba.JPEG),
                ArchivosPrueba.identificacion("3.jpg", ArchivosPrueba.JPEG));
        ValidacionException ex = assertThrows(ValidacionException.class,
                () -> ValidadorArchivos.validarYClasificar(archivos));
        assertTrue(ex.getMessage().contains("hasta 2"), "Debe indicar el máximo de 2");
    }

    @Test
    void validarYClasificar_identificacionEnPdf_lanzaExcepcion() {
        List<MultipartFile> archivos = List.of(ArchivosPrueba.identificacion("ine.pdf", ArchivosPrueba.PDF));
        ValidacionException ex = assertThrows(ValidacionException.class,
                () -> ValidadorArchivos.validarYClasificar(archivos));
        assertTrue(ex.getMessage().contains("\"ine.pdf\""), "El mensaje muestra el nombre sin el prefijo interno");
        assertFalse(ex.getMessage().contains(ReglasQueja.PREFIJO_IDENTIFICACION), "No debe filtrarse el prefijo");
    }

    @Test
    void validarYClasificar_identificacionDeExactamenteTresMB_esValida() {
        List<MultipartFile> archivos = List.of(
                ArchivosPrueba.archivoDeTamanio(ReglasQueja.PREFIJO_IDENTIFICACION + "ine.jpg",
                        ArchivosPrueba.JPEG, LIMITE_IDENTIFICACION));
        assertDoesNotThrow(() -> ValidadorArchivos.validarYClasificar(archivos), "3 MB exactos es el borde válido");
    }

    @Test
    void validarYClasificar_identificacionDeTresMBMasUnByte_lanzaExcepcion() {
        List<MultipartFile> archivos = List.of(
                ArchivosPrueba.archivoDeTamanio(ReglasQueja.PREFIJO_IDENTIFICACION + "ine.jpg",
                        ArchivosPrueba.JPEG, LIMITE_IDENTIFICACION + 1));
        ValidacionException ex = assertThrows(ValidacionException.class,
                () -> ValidadorArchivos.validarYClasificar(archivos));
        assertTrue(ex.getMessage().contains("no puede exceder 3 MB"), "Debe rechazar por tamaño");
    }

    @Test
    void validarYClasificar_evidenciaDeTipoDesconocido_lanzaExcepcion() {
        List<MultipartFile> archivos = List.of(
                ArchivosPrueba.identificacion("ine.jpg", ArchivosPrueba.JPEG),
                ArchivosPrueba.archivo("virus.pdf", ArchivosPrueba.EXE));
        ValidacionException ex = assertThrows(ValidacionException.class,
                () -> ValidadorArchivos.validarYClasificar(archivos));
        assertTrue(ex.getMessage().contains("no es de un tipo permitido"), "Debe rechazar el ejecutable");
    }

    @Test
    void validarYClasificar_evidenciaArribaDe30MB_lanzaExcepcion() {
        List<MultipartFile> archivos = List.of(
                ArchivosPrueba.identificacion("ine.jpg", ArchivosPrueba.JPEG),
                ArchivosPrueba.archivoDeTamanio("video.mp4", ArchivosPrueba.MP4, LIMITE_EVIDENCIA + 1));
        ValidacionException ex = assertThrows(ValidacionException.class,
                () -> ValidadorArchivos.validarYClasificar(archivos));
        assertTrue(ex.getMessage().contains("El máximo por archivo es 30 MB"), "Debe rechazar por tamaño");
    }

    @Test
    void validarEvidenciasAdicionales_listaVacia_lanzaExcepcion() {
        ValidacionException ex = assertThrows(ValidacionException.class,
                () -> ValidadorArchivos.validarEvidenciasAdicionales(List.of()));
        assertEquals("No se recibió ningún archivo.", ex.getMessage(), "Sin archivos nuevos");
    }

    @Test
    void validarEvidenciasAdicionales_conIdentificacion_lanzaExcepcion() {
        List<MultipartFile> archivos = List.of(ArchivosPrueba.identificacion("ine.jpg", ArchivosPrueba.JPEG));
        ValidacionException ex = assertThrows(ValidacionException.class,
                () -> ValidadorArchivos.validarEvidenciasAdicionales(archivos));
        assertTrue(ex.getMessage().contains("solo se puede adjuntar al presentar la queja"),
                "La credencial no se agrega después");
    }

    @Test
    void validarEvidenciasAdicionales_evidenciaValida_noLanza() {
        List<MultipartFile> archivos = List.of(ArchivosPrueba.archivo("audio.mp3", ArchivosPrueba.MP3_ID3));
        assertDoesNotThrow(() -> ValidadorArchivos.validarEvidenciasAdicionales(archivos), "MP3 es evidencia válida");
    }
}
