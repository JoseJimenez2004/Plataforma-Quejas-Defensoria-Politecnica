package ipn.escom.defensoria.queja_service.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import ipn.escom.defensoria.queja_service.dto.EditarQuejaRequest;
import ipn.escom.defensoria.queja_service.dto.RegistroQuejaPublicaRequest;
import ipn.escom.defensoria.queja_service.entity.Queja;
import ipn.escom.defensoria.queja_service.entity.QuejaEvidencia;
import ipn.escom.defensoria.queja_service.repository.CuentaQuejosoRepository;
import ipn.escom.defensoria.queja_service.repository.QuejaEvidenciaRepository;
import ipn.escom.defensoria.queja_service.repository.QuejaRepository;
import ipn.escom.defensoria.queja_service.validacion.ReglasQueja;
import ipn.escom.defensoria.queja_service.validacion.ValidacionException;
import ipn.escom.defensoria.queja_service.validacion.ValidadorArchivos;
import ipn.escom.defensoria.queja_service.validacion.ValidadorQuejaPublica;

/** Pruebas unitarias de {@link QuejaService} (CU-Q01, CU-Q02, CU-Q06, CU-Q07). */
@ExtendWith(MockitoExtension.class)
@DisplayName("QuejaService — ciclo de la queja del lado del quejoso")
class QuejaServiceTest {

    private static final String FOLIO = "FOL-ABCD1234";
    private static final String CORREO = "quejoso@ipn.mx";
    private static final String PATRON_FOLIO = "FOL-[0-9A-F]{8}";
    private static final byte[] PDF = {'%', 'P', 'D', 'F', '-', '1', '.', '7', 0x0A, 0, 0, 0};

    @Mock
    private QuejaRepository quejaRepository;
    @Mock
    private QuejaEvidenciaRepository quejaEvidenciaRepository;
    @Mock
    private NotificacionClienteService notificaciones;
    @Mock
    private ValidadorQuejaPublica validadorQuejaPublica;
    @Mock
    private CuentaQuejosoRepository cuentaQuejosoRepository;

    @InjectMocks
    private QuejaService servicio;

    private Queja queja;

    @BeforeEach
    void setUp() {
        queja = new Queja();
        queja.setNumeroFolio(FOLIO);
        queja.setCorreoInstitucional(CORREO);
        queja.setEstatus("RECIBIDA");
    }

    private void quejaExiste() {
        when(quejaRepository.findByNumeroFolioAndCorreoInstitucional(FOLIO, CORREO)).thenReturn(Optional.of(queja));
    }

    private void guardarDevuelveArgumento() {
        when(quejaRepository.save(any(Queja.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    private static QuejaEvidencia evidencia(long id, String tipo) {
        QuejaEvidencia ev = new QuejaEvidencia();
        ev.setId(id);
        ev.setTipo(tipo);
        return ev;
    }

    @Nested
    @DisplayName("Consulta")
    class Consulta {

        @Test
        void obtenerPorFolioYCorreo_normalizaFolioYCorreo() {
            when(quejaRepository.findByNumeroFolioAndCorreoInstitucional(FOLIO, CORREO)).thenReturn(Optional.of(queja));
            Queja r = servicio.obtenerPorFolioYCorreo("  fol-abcd1234 ", " Quejoso@IPN.mx ");
            assertEquals(FOLIO, r.getNumeroFolio(), "Folio en minúsculas debe encontrar la queja");
        }

        @Test
        void obtenerPorFolioYCorreo_noExiste_lanzaExcepcion() {
            when(quejaRepository.findByNumeroFolioAndCorreoInstitucional(anyString(), anyString())).thenReturn(Optional.empty());
            RuntimeException ex = assertThrows(RuntimeException.class, () -> servicio.obtenerPorFolioYCorreo(FOLIO, CORREO));
            assertTrue(ex.getMessage().contains("no existe"), "Mensaje de folio inexistente");
        }

        @Test
        void validarFolioYCorreo_sinCoincidencia_regresaFalse() {
            when(quejaRepository.findByNumeroFolioAndCorreoInstitucional(FOLIO, CORREO)).thenReturn(Optional.empty());
            assertFalse(servicio.validarFolioYCorreo(FOLIO, CORREO), "Sin coincidencia no valida");
        }

        @Test
        void obtenerMiQueja_deOtraCuenta_lanzaExcepcion() {
            when(quejaRepository.findByNumeroFolioAndCorreoInstitucional(FOLIO, "otro@ipn.mx")).thenReturn(Optional.empty());
            assertThrows(RuntimeException.class, () -> servicio.obtenerMiQueja(FOLIO, "otro@ipn.mx"),
                    "Un quejoso no puede ver la queja de otro");
        }
    }

    @Nested
    @DisplayName("Edición y corrección")
    class Edicion {

        @Test
        void editarMiQueja_recibida_aplicaSoloCamposNoNulos() {
            quejaExiste();
            guardarDevuelveArgumento();
            queja.setDescripcion("Original");
            queja.setUnidadAcademicaClave("ESCOM");
            EditarQuejaRequest cambios = new EditarQuejaRequest();
            cambios.setDescripcion("Nueva descripción");
            cambios.setNombreDenunciado("  ");

            Queja r = servicio.editarMiQueja(FOLIO, CORREO, cambios);

            assertEquals("Nueva descripción", r.getDescripcion(), "Se actualiza la descripción");
            assertEquals("ESCOM", r.getUnidadAcademicaClave(), "Lo que no viene no se toca");
            assertNull(r.getNombreDenunciado(), "Un denunciado en blanco se guarda como null");
        }

        @Test
        void editarMiQueja_estatusNulo_seTrataComoRecibida() {
            queja.setEstatus(null);
            quejaExiste();
            guardarDevuelveArgumento();
            servicio.editarMiQueja(FOLIO, CORREO, new EditarQuejaRequest());
            verify(quejaRepository).save(queja);
        }

        @Test
        void editarMiQueja_enRevision_lanzaExcepcionYNoGuarda() {
            queja.setEstatus("EN_VALIDACION");
            quejaExiste();
            RuntimeException ex = assertThrows(RuntimeException.class,
                    () -> servicio.editarMiQueja(FOLIO, CORREO, new EditarQuejaRequest()));
            assertTrue(ex.getMessage().contains("ya está en revisión"), "No se edita fuera de RECIBIDA");
            verify(quejaRepository, never()).save(any());
        }

        @Test
        void editarMiQueja_descripcionEnBlanco_lanzaExcepcion() {
            quejaExiste();
            EditarQuejaRequest cambios = new EditarQuejaRequest();
            cambios.setDescripcion("   ");
            RuntimeException ex = assertThrows(RuntimeException.class, () -> servicio.editarMiQueja(FOLIO, CORREO, cambios));
            assertEquals("La descripción no puede quedar vacía.", ex.getMessage(), "Descripción vacía");
        }

        @Test
        void corregirMiQueja_rechazada_pasaACorregidaYNotificaAmbos() {
            queja.setEstatus("RECHAZADA");
            queja.setValidadoPor("recep@ipn.mx");
            quejaExiste();
            guardarDevuelveArgumento();

            Queja r = servicio.corregirMiQueja(FOLIO, CORREO, new EditarQuejaRequest());

            assertEquals("CORREGIDA", r.getEstatus(), "Vuelve a la bandeja como CORREGIDA");
            verify(notificaciones).notificarCambioEstatus(eq(CORREO), eq(FOLIO), anyString(), anyString());
            verify(notificaciones).notificarPersonal(eq("recep@ipn.mx"), anyString(), anyString());
        }

        @Test
        void corregirMiQueja_noRechazada_lanzaExcepcion() {
            quejaExiste();
            RuntimeException ex = assertThrows(RuntimeException.class,
                    () -> servicio.corregirMiQueja(FOLIO, CORREO, new EditarQuejaRequest()));
            assertTrue(ex.getMessage().startsWith("Solo se puede corregir una queja que fue rechazada"),
                    "Solo aplica a RECHAZADA");
        }
    }

    @Nested
    @DisplayName("Cancelación y evidencias")
    class CancelacionYEvidencias {

        @Test
        void cancelarMiQueja_recibida_quedaCanceladaSinBorrarse() {
            quejaExiste();
            guardarDevuelveArgumento();
            Queja r = servicio.cancelarMiQueja(FOLIO, CORREO);
            assertEquals("CANCELADA", r.getEstatus(), "La queja se marca, no se borra");
            verify(quejaRepository, never()).delete(any());
        }

        @Test
        void cancelarMiQueja_turnada_lanzaValidacionException() {
            queja.setEstatus("TURNADA");
            quejaExiste();
            assertThrows(ValidacionException.class, () -> servicio.cancelarMiQueja(FOLIO, CORREO),
                    "No se puede retirar una queja en revisión");
        }

        @Test
        void eliminarEvidencia_unicaIdentificacion_lanzaExcepcion() {
            queja.getEvidencias().add(evidencia(1L, "IDENTIFICACION"));
            quejaExiste();
            ValidacionException ex = assertThrows(ValidacionException.class,
                    () -> servicio.eliminarEvidencia(FOLIO, CORREO, 1L));
            assertTrue(ex.getMessage().contains("al menos una"), "Debe conservar una identificación");
            verify(quejaEvidenciaRepository, never()).delete(any());
        }

        @Test
        void eliminarEvidencia_identificacionConOtraDisponible_seElimina() {
            QuejaEvidencia aBorrar = evidencia(1L, "IDENTIFICACION");
            queja.getEvidencias().add(aBorrar);
            queja.getEvidencias().add(evidencia(2L, "IDENTIFICACION"));
            quejaExiste();

            servicio.eliminarEvidencia(FOLIO, CORREO, 1L);

            verify(quejaEvidenciaRepository).delete(aBorrar);
            assertEquals(1, queja.getEvidencias().size(), "Queda una identificación");
        }

        @Test
        void eliminarEvidencia_idAjeno_lanzaExcepcion() {
            queja.getEvidencias().add(evidencia(1L, "EVIDENCIA"));
            quejaExiste();
            ValidacionException ex = assertThrows(ValidacionException.class,
                    () -> servicio.eliminarEvidencia(FOLIO, CORREO, 99L));
            assertTrue(ex.getMessage().contains("no pertenece a esta queja"), "No se borra evidencia de otra queja");
        }

        @Test
        void agregarEvidencias_pdfValido_seAgregaComoEvidencia() {
            quejaExiste();
            guardarDevuelveArgumento();
            List<MultipartFile> archivos = List.of(new MockMultipartFile("archivos", "acta.pdf", "application/pdf", PDF));

            servicio.agregarEvidencias(FOLIO, CORREO, archivos);

            assertEquals(1, queja.getEvidencias().size(), "Se agregó una evidencia");
            assertEquals("EVIDENCIA", queja.getEvidencias().get(0).getTipo(), "Tipo EVIDENCIA");
        }
    }

    @Nested
    @DisplayName("Registro")
    class Registro {

        @Test
        void registrarQueja_sinMotivo_lanzaExcepcion() {
            RuntimeException ex = assertThrows(RuntimeException.class,
                    () -> servicio.registrarQueja(" ", "desc", CORREO, "ESCOM", LocalDate.now(), null, null, null));
            assertEquals("Faltan datos obligatorios de la queja.", ex.getMessage(), "Motivo obligatorio");
        }

        @Test
        void registrarQueja_autenticado_generaFolioYCopiaDatosDeQuejaAnterior() {
            Queja anterior = new Queja();
            anterior.setNombreQuejoso("Ana");
            anterior.setNumeroIdentificacionQuejoso("2021630001");
            when(quejaRepository.findFirstByCorreoInstitucionalIgnoreCaseAndNombreQuejosoIsNotNullOrderByFechaCreacionDesc(CORREO))
                    .thenReturn(Optional.of(anterior));
            guardarDevuelveArgumento();

            Queja r = servicio.registrarQueja("Motivo", "Descripción", CORREO, "ESCOM", LocalDate.now(), null, null, null);

            assertTrue(r.getNumeroFolio().matches(PATRON_FOLIO), "Folio con formato FOL-XXXXXXXX");
            assertEquals("AUTENTICADO", r.getOrigenRegistro(), "Origen autenticado");
            assertEquals("Ana", r.getNombreQuejoso(), "Nombre copiado de la queja anterior");
            verify(notificaciones).notificarQuejaCreada(CORREO, r.getNumeroFolio());
            verify(cuentaQuejosoRepository, never()).findFirstByCorreoInstitucionalIgnoreCase(anyString());
        }

        @Test
        void registrarQuejaPublica_conTutor_guardaTutorYNotificaAlTutor() {
            RegistroQuejaPublicaRequest datos = new RegistroQuejaPublicaRequest();
            datos.setCorreo(CORREO);
            datos.setNombre("Luis");
            datos.setApellido1("Pérez");
            datos.setUnidadAcademicaClave("CECYT9");
            datos.setTutorNombre("Rosa");
            datos.setTutorCorreo("tutor@correo.com");
            when(validadorQuejaPublica.validar(datos))
                    .thenReturn(new ValidadorArchivos.ArchivosClasificados(List.of(), List.of()));
            guardarDevuelveArgumento();

            Queja r = servicio.registrarQuejaPublica(datos);

            assertEquals("PUBLICO", r.getOrigenRegistro(), "Origen público");
            assertEquals("Queja en CECYT9", r.getMotivo(), "Motivo armado con la unidad");
            assertEquals(ReglasQueja.AVISO_PRIVACIDAD_VERSION, r.getAvisoPrivacidadVersion(), "Versión por defecto del aviso");
            assertEquals("Rosa", r.getTutor().getNombre(), "Se guarda el tutor");
            ArgumentCaptor<String> menor = ArgumentCaptor.forClass(String.class);
            verify(notificaciones).notificarTutorQuejaCreada(eq("tutor@correo.com"), eq("Rosa"), menor.capture(), anyString());
            assertEquals("Luis Pérez", menor.getValue(), "Nombre del menor en la notificación");
        }

        @Test
        void registrarQuejaPublica_sinTutor_noNotificaTutor() {
            RegistroQuejaPublicaRequest datos = new RegistroQuejaPublicaRequest();
            datos.setCorreo(CORREO);
            datos.setAvisoPrivacidadVersion("2.0");
            when(validadorQuejaPublica.validar(datos))
                    .thenReturn(new ValidadorArchivos.ArchivosClasificados(List.of(), List.of()));
            guardarDevuelveArgumento();

            Queja r = servicio.registrarQuejaPublica(datos);

            assertNull(r.getTutor(), "Sin tutor");
            assertEquals("2.0", r.getAvisoPrivacidadVersion(), "Respeta la versión enviada");
            verify(notificaciones, never()).notificarTutorQuejaCreada(any(), any(), any(), any());
        }

        @Test
        void registrarQuejaPublica_validacionFalla_noGuarda() {
            RegistroQuejaPublicaRequest datos = new RegistroQuejaPublicaRequest();
            when(validadorQuejaPublica.validar(datos)).thenThrow(new ValidacionException("Falta el nombre."));
            assertThrows(ValidacionException.class, () -> servicio.registrarQuejaPublica(datos), "Propaga la validación");
            verify(quejaRepository, never()).save(any());
        }
    }
}
