package ipn.escom.defensoria.denunciado_service.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import ipn.escom.defensoria.denunciado_service.dto.RegistroRespuestaRequest;
import ipn.escom.defensoria.denunciado_service.dto.RespuestaRegistradaModel;
import ipn.escom.defensoria.denunciado_service.entity.RespuestaDenunciado;
import ipn.escom.defensoria.denunciado_service.entity.RespuestaDenunciadoArchivo;
import ipn.escom.defensoria.denunciado_service.repository.RespuestaDenunciadoArchivoRepository;
import ipn.escom.defensoria.denunciado_service.repository.RespuestaDenunciadoRepository;
import ipn.escom.defensoria.denunciado_service.validacion.ReglasDenunciado;
import ipn.escom.defensoria.denunciado_service.validacion.ValidacionException;

/** Pruebas unitarias de {@link RespuestaDenunciadoService}: registro de la versión del denunciado. */
@ExtendWith(MockitoExtension.class)
@DisplayName("RespuestaDenunciadoService — versión del denunciado")
class RespuestaDenunciadoServiceTest {

    private static final byte[] JPEG = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0, 0x10, 'J', 'F', 'I', 'F', 0, 1};
    private static final byte[] PDF = {'%', 'P', 'D', 'F', '-', '1', '.', '7', 0x0A, 0, 0, 0};
    private static final byte[] EXE = {'M', 'Z', (byte) 0x90, 0, 3, 0, 0, 0, 4, 0, 0, 0};
    private static final int DESCRIPCION_MIN = ReglasDenunciado.DESCRIPCION_LONGITUD_MINIMA;
    private static final int DESCRIPCION_MAX = ReglasDenunciado.DESCRIPCION_LONGITUD_MAXIMA;
    private static final int EVIDENCIAS_MAX = ReglasDenunciado.EVIDENCIA_CANTIDAD_MAXIMA;
    private static final long CREDENCIAL_MAX = ReglasDenunciado.CREDENCIAL_TAMANIO_MAXIMO;
    private static final long MB = 1024L * 1024;

    @Mock
    private RespuestaDenunciadoRepository respuestas;
    @Mock
    private RespuestaDenunciadoArchivoRepository archivos;

    @InjectMocks
    private RespuestaDenunciadoService servicio;

    private RegistroRespuestaRequest datos;

    @BeforeEach
    void setUp() {
        datos = new RegistroRespuestaRequest();
        datos.setFolioQueja(" fol-abcd1234 ");
        datos.setNombre("  Juan   Carlos ");
        datos.setApellido1("Pérez");
        datos.setUnidadProcedenciaClave("ESCOM");
        datos.setTipoIdentificacion("empleado");
        datos.setNumeroIdentificacion("12345");
        datos.setDescripcionHechos("Mi versión de los hechos es la siguiente.");
        datos.setAvisoPrivacidadAceptado(Boolean.TRUE);
        datos.setCredencial(List.of(archivo("credencial.jpg", JPEG)));
        lenient().when(respuestas.save(any(RespuestaDenunciado.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    private static MockMultipartFile archivo(String nombre, byte[] firma) {
        return new MockMultipartFile("archivo", nombre, "application/octet-stream", firma);
    }

    private static MockMultipartFile archivoDeTamanio(String nombre, byte[] firma, long tamanio) {
        return new MockMultipartFile("archivo", nombre, "application/octet-stream",
                Arrays.copyOf(firma, Math.toIntExact(tamanio)));
    }

    private String mensajeAlRegistrar() {
        return assertThrows(ValidacionException.class, () -> servicio.registrar(datos)).getMessage();
    }

    @Test
    void registrar_datosValidos_normalizaYGeneraFolioRD() {
        RespuestaRegistradaModel r = servicio.registrar(datos);

        assertTrue(r.folioRespuesta().matches("RD-[0-9A-F]{8}"), "Folio con formato RD-XXXXXXXX");
        assertEquals("FOL-ABCD1234", r.folioQueja(), "Folio de queja en mayúsculas");
        assertEquals("Juan Carlos Pérez", r.nombreCompleto(), "Nombre normalizado");
        assertEquals(1, r.credenciales(), "Una credencial");

        ArgumentCaptor<RespuestaDenunciado> guardada = ArgumentCaptor.forClass(RespuestaDenunciado.class);
        verify(respuestas).save(guardada.capture());
        assertEquals("EMPLEADO", guardada.getValue().getTipoIdentificacion(), "Tipo en mayúsculas");
        assertEquals(ReglasDenunciado.AVISO_PRIVACIDAD_VERSION, guardada.getValue().getAvisoPrivacidadVersion(),
                "Versión del aviso por defecto");
        RespuestaDenunciadoArchivo cred = guardada.getValue().getArchivos().get(0);
        assertEquals(RespuestaDenunciadoArchivo.TIPO_IDENTIFICACION, cred.getTipo(), "La credencial se marca como tal");
        assertEquals("image/jpeg", cred.getTipoMime(), "El MIME sale de la firma, no del navegador");
    }

    @Test
    void registrar_folioRepetido_generaOtro() {
        when(respuestas.existsByFolioRespuesta(anyString())).thenReturn(true, false);
        servicio.registrar(datos);
        verify(respuestas, times(2)).existsByFolioRespuesta(anyString());
    }

    @ParameterizedTest
    @ValueSource(strings = {"FOL-123", "FOLABCD1234", "XYZ-ABCD1234", "FOL-ABCD12345"})
    void registrar_folioConFormatoInvalido_lanzaExcepcion(String folio) {
        datos.setFolioQueja(folio);
        assertEquals("El folio de la queja no es válido (formato FOL-XXXXXXXX).", mensajeAlRegistrar(), "Folio " + folio);
    }

    @Test
    void registrar_sinNombre_lanzaExcepcion() {
        datos.setNombre("   ");
        assertEquals("Escribe el nombre.", mensajeAlRegistrar(), "Nombre obligatorio");
    }

    @Test
    void registrar_apellidoConNumeros_lanzaExcepcion() {
        datos.setApellido1("Pér3z");
        assertTrue(mensajeAlRegistrar().startsWith("El primer apellido solo admite letras"), "Solo letras");
    }

    @Test
    void registrar_nombreDeUnaLetra_lanzaExcepcion() {
        datos.setNombre("J");
        assertTrue(mensajeAlRegistrar().contains("entre 2 y 50"), "Longitud mínima de 2");
    }

    @Test
    void registrar_sinUnidad_lanzaExcepcion() {
        datos.setUnidadProcedenciaClave(null);
        assertEquals("Selecciona tu unidad de procedencia.", mensajeAlRegistrar(), "Unidad obligatoria");
    }

    @Test
    void registrar_unidadDemasiadoLarga_lanzaExcepcion() {
        datos.setUnidadProcedenciaClave("X".repeat(ReglasDenunciado.CLAVE_UNIDAD_LONGITUD_MAXIMA + 1));
        assertEquals("La unidad de procedencia no es válida.", mensajeAlRegistrar(), "Máximo 40 caracteres");
    }

    @Test
    void registrar_tipoDesconocido_lanzaExcepcion() {
        datos.setTipoIdentificacion("visitante");
        assertEquals("Indica si eres alumno o empleado.", mensajeAlRegistrar(), "Tipo de identificación");
    }

    @Test
    void registrar_boletaConLetras_mensajeDeAlumno() {
        datos.setTipoIdentificacion("alumno");
        datos.setNumeroIdentificacion("20A1");
        assertEquals("El número de boleta solo admite dígitos (máximo 10).", mensajeAlRegistrar(), "Boleta inválida");
    }

    @Test
    void registrar_descripcionEnLimites_esValida() {
        datos.setDescripcionHechos("x".repeat(DESCRIPCION_MAX));
        assertDoesNotThrow(() -> servicio.registrar(datos), "4000 caracteres es el máximo válido");
        datos.setDescripcionHechos("x".repeat(DESCRIPCION_MIN));
        assertDoesNotThrow(() -> servicio.registrar(datos), "20 caracteres es el mínimo válido");
    }

    @Test
    void registrar_descripcionCorta_lanzaExcepcion() {
        datos.setDescripcionHechos("x".repeat(DESCRIPCION_MIN - 1));
        assertTrue(mensajeAlRegistrar().contains("al menos 20"), "19 caracteres no alcanza");
    }

    @Test
    void registrar_descripcionLarga_lanzaExcepcion() {
        datos.setDescripcionHechos("x".repeat(DESCRIPCION_MAX + 1));
        assertTrue(mensajeAlRegistrar().contains("no puede exceder 4000"), "4001 caracteres excede");
    }

    @Test
    void registrar_sinAviso_lanzaExcepcion() {
        datos.setAvisoPrivacidadAceptado(null);
        assertEquals("Debes leer y aceptar el aviso de privacidad.", mensajeAlRegistrar(), "Aviso obligatorio");
        verify(respuestas, never()).save(any());
    }

    @Test
    void registrar_sinCredencial_lanzaExcepcion() {
        datos.setCredencial(null);
        assertTrue(mensajeAlRegistrar().startsWith("Adjunta la imagen de tu credencial"), "Credencial obligatoria");
    }

    @Test
    void registrar_tresCredenciales_lanzaExcepcion() {
        datos.setCredencial(List.of(archivo("1.jpg", JPEG), archivo("2.jpg", JPEG), archivo("3.jpg", JPEG)));
        assertTrue(mensajeAlRegistrar().contains("máximo 2"), "Máximo frente y reverso");
    }

    @Test
    void registrar_credencialPdf_lanzaExcepcion() {
        datos.setCredencial(List.of(archivo("ine.pdf", PDF)));
        assertTrue(mensajeAlRegistrar().contains("debe ser una imagen JPG o PNG"), "Solo imágenes");
    }

    @Test
    void registrar_credencialDeTresMBMasUno_lanzaExcepcion() {
        datos.setCredencial(List.of(archivoDeTamanio("ine.jpg", JPEG, CREDENCIAL_MAX + 1)));
        assertTrue(mensajeAlRegistrar().contains("pesa más de 3 MB"), "Límite de 3 MB");
    }

    @Test
    void registrar_diezEvidencias_esElMaximo() {
        List<MultipartFile> evidencias = new ArrayList<>();
        for (int i = 0; i < EVIDENCIAS_MAX; i++) {
            evidencias.add(archivo("e" + i + ".pdf", PDF));
        }
        datos.setEvidencias(evidencias);
        RespuestaRegistradaModel r = servicio.registrar(datos);
        assertEquals(EVIDENCIAS_MAX, r.evidencias(), "10 evidencias es el borde válido");
    }

    @Test
    void registrar_onceEvidencias_lanzaExcepcion() {
        List<MultipartFile> evidencias = new ArrayList<>();
        for (int i = 0; i <= EVIDENCIAS_MAX; i++) {
            evidencias.add(archivo("e" + i + ".pdf", PDF));
        }
        datos.setEvidencias(evidencias);
        assertTrue(mensajeAlRegistrar().contains("máximo 10 evidencias"), "11 evidencias excede");
    }

    @Test
    void registrar_evidenciasSumanMasDe95MB_lanzaExcepcion() {
        long tamanio = 24 * MB;
        datos.setEvidencias(List.of(archivoDeTamanio("a.pdf", PDF, tamanio), archivoDeTamanio("b.pdf", PDF, tamanio),
                archivoDeTamanio("c.pdf", PDF, tamanio), archivoDeTamanio("d.pdf", PDF, tamanio)));
        assertTrue(mensajeAlRegistrar().contains("suman más de 95 MB"), "96 MB en total excede");
    }

    @Test
    void registrar_evidenciaEjecutable_lanzaExcepcion() {
        datos.setEvidencias(List.of(archivo("documento.pdf", EXE)));
        assertTrue(mensajeAlRegistrar().contains("no es un tipo de archivo permitido"), "Rechaza ejecutable");
    }

    @Test
    void registrar_nombreDeArchivoConRuta_seGuardaSinRuta() {
        datos.setEvidencias(List.of(archivo("C:\\Users\\juan\\acta\".pdf", PDF)));
        servicio.registrar(datos);
        ArgumentCaptor<RespuestaDenunciado> guardada = ArgumentCaptor.forClass(RespuestaDenunciado.class);
        verify(respuestas).save(guardada.capture());
        assertEquals("acta.pdf", guardada.getValue().getArchivos().get(1).getNombreArchivo(),
                "Se quitan rutas y comillas del nombre");
    }

    @Test
    void listarPorFolioQueja_folioVacio_lanzaExcepcion() {
        ValidacionException ex = assertThrows(ValidacionException.class, () -> servicio.listarPorFolioQueja(" "));
        assertEquals("Indica el folio de la queja.", ex.getMessage(), "Folio obligatorio");
    }

    @Test
    void listarPorFolioQueja_buscaEnMayusculas() {
        when(respuestas.findByFolioQuejaOrderByFechaRegistroDesc("FOL-ABCD1234")).thenReturn(List.of());
        assertTrue(servicio.listarPorFolioQueja("fol-abcd1234").isEmpty(), "Sin respuestas");
    }

    @Test
    void obtenerArchivo_inexistente_lanzaExcepcion() {
        when(archivos.findById(9L)).thenReturn(Optional.empty());
        assertThrows(ValidacionException.class, () -> servicio.obtenerArchivo(9L), "Archivo inexistente");
    }
}
