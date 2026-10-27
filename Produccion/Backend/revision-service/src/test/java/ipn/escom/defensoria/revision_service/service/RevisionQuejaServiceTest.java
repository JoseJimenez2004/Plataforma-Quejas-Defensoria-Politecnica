package ipn.escom.defensoria.revision_service.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import ipn.escom.defensoria.revision_service.entity.PersonalAdministrativo;
import ipn.escom.defensoria.revision_service.entity.Queja;
import ipn.escom.defensoria.revision_service.model.DatosVentanillaRequest;
import ipn.escom.defensoria.revision_service.model.PrimerContactoIngresoResponse;
import ipn.escom.defensoria.revision_service.repository.PersonalAdministrativoRepository;
import ipn.escom.defensoria.revision_service.repository.QuejaEvidenciaRepository;
import ipn.escom.defensoria.revision_service.repository.QuejaRepository;

/**
 * Pruebas unitarias de {@link RevisionQuejaService} — CU-R03 (rechazar), CU-R04 (turnar),
 * CU-R05 (registro manual) y el bloqueo de una queja mientras una recepcionista la revisa
 * (RN-R01: máquina de estados).
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("RevisionQuejaService — Recepción")
class RevisionQuejaServiceTest {

    private static final String FOLIO = "FOL-ABCD1234";
    private static final String YO = "recep1@ipn.mx";
    private static final String OTRA = "recep2@ipn.mx";
    private static final String DEFENSOR = "Lic. Laura Méndez";
    private static final String FOLIO_PC = "PC-A1B2C3D4";
    private static final int TAMANIO_MAXIMO = 50;

    @Mock
    private QuejaRepository quejaRepository;
    @Mock
    private QuejaEvidenciaRepository evidenciaRepository;
    @Mock
    private NotificacionQuejaService notificaciones;
    @Mock
    private PrimerContactoClientService primerContacto;
    @Mock
    private PersonalAdministrativoRepository personalRepository;
    @Mock
    private IndicadoresBandejaService indicadores;

    @InjectMocks
    private RevisionQuejaService servicio;

    private Queja queja;

    @BeforeEach
    void setUp() {
        queja = new Queja();
        queja.setId(1L);
        queja.setNumeroFolio(FOLIO);
        queja.setCorreoInstitucional("quejoso@ipn.mx");
        queja.setNombreQuejoso("Ana");
        queja.setApellido1Quejoso("López");
        queja.setEstatus(RevisionQuejaService.RECIBIDA);
        lenient().when(quejaRepository.findByNumeroFolio(FOLIO)).thenReturn(Optional.of(queja));
        lenient().when(quejaRepository.save(any(Queja.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    /** Deja la queja tomada (EN_VALIDACION) por la persona indicada. */
    private void tomadaPor(String correo) {
        queja.setEstatus(RevisionQuejaService.EN_VALIDACION);
        queja.setRevisandoPor(correo);
        queja.setRevisandoPorNombre("Nombre de " + correo);
        queja.setEstatusPrevioRevision(RevisionQuejaService.RECIBIDA);
        queja.setFechaInicioRevision(LocalDateTime.now());
    }

    @Nested
    @DisplayName("Toma de la queja (concurrencia entre recepcionistas)")
    class Toma {

        @Test
        void detalle_quejaLibre_laTomaDeFormaAtomica() {
            when(quejaRepository.findByEstatusAndFechaInicioRevisionBefore(eq(RevisionQuejaService.EN_VALIDACION), any()))
                    .thenReturn(List.of());
            when(personalRepository.findByCorreoInstitucional(YO)).thenReturn(Optional.empty());

            servicio.detalle(FOLIO, YO);

            verify(quejaRepository).tomarParaRevision(eq(FOLIO), eq(YO), eq(YO), any(LocalDateTime.class));
        }

        @Test
        void detalle_tomadaPorOtra_lanzaExcepcionConSuNombre() {
            tomadaPor(OTRA);
            when(quejaRepository.findByEstatusAndFechaInicioRevisionBefore(any(), any())).thenReturn(List.of());

            RuntimeException ex = assertThrows(RuntimeException.class, () -> servicio.detalle(FOLIO, YO));

            assertTrue(ex.getMessage().contains("la está atendiendo Nombre de " + OTRA), "Indica quién la tiene");
            verify(quejaRepository, never()).renovarRevision(anyString(), anyString(), any());
        }

        @Test
        void detalle_tomadaPorMi_renuevaElTiempo() {
            tomadaPor(YO);
            when(quejaRepository.findByEstatusAndFechaInicioRevisionBefore(any(), any())).thenReturn(List.of());

            servicio.detalle(FOLIO, YO.toUpperCase());

            verify(quejaRepository).renovarRevision(eq(FOLIO), eq(YO.toUpperCase()), any(LocalDateTime.class));
        }

        @Test
        void detalle_revisionVencida_seLiberaAntesDeConsultar() {
            Queja vencida = new Queja();
            vencida.setEstatus(RevisionQuejaService.EN_VALIDACION);
            vencida.setRevisandoPor(OTRA);
            vencida.setEstatusPrevioRevision(RevisionQuejaService.CORREGIDA);
            when(quejaRepository.findByEstatusAndFechaInicioRevisionBefore(any(), any())).thenReturn(List.of(vencida));
            when(personalRepository.findByCorreoInstitucional(YO)).thenReturn(Optional.empty());

            servicio.detalle(FOLIO, YO);

            assertEquals(RevisionQuejaService.CORREGIDA, vencida.getEstatus(), "Regresa a su estatus anterior");
            assertNull(vencida.getRevisandoPor(), "Ya nadie la tiene");
            verify(quejaRepository).saveAll(List.of(vencida));
        }

        @Test
        void liberar_mia_regresaAlEstatusPrevio() {
            tomadaPor(YO);
            servicio.liberar(FOLIO, YO);
            assertEquals(RevisionQuejaService.RECIBIDA, queja.getEstatus(), "Vuelve a la cola");
            assertNull(queja.getRevisandoPor(), "Se limpia quién la revisaba");
        }

        @Test
        void liberar_quejaNoEnValidacion_noHaceNada() {
            servicio.liberar(FOLIO, YO);
            verify(quejaRepository, never()).save(any());
        }

        @Test
        void liberar_deOtra_lanzaExcepcion() {
            tomadaPor(OTRA);
            assertThrows(RuntimeException.class, () -> servicio.liberar(FOLIO, YO), "No se suelta la queja ajena");
        }

        @Test
        void renovar_quejaYaNoEsMia_lanzaExcepcion() {
            tomadaPor(OTRA);
            RuntimeException ex = assertThrows(RuntimeException.class, () -> servicio.renovar(FOLIO, YO));
            assertEquals("Esta queja ya no está a tu nombre. Regresa a la bandeja.", ex.getMessage(), "Mensaje de pérdida");
        }
    }

    @Nested
    @DisplayName("Rechazo (CU-R03)")
    class Rechazo {

        @Test
        void rechazar_conMotivosYObservaciones_guardaTextoYNotifica() {
            tomadaPor(YO);

            Queja r = servicio.rechazar(FOLIO, List.of("Falta identificación", "Ilegible"), "Sube la INE", YO);

            assertEquals(RevisionQuejaService.RECHAZADA, r.getEstatus(), "Pasa a RECHAZADA");
            assertEquals("Falta identificación; Ilegible\n\nSube la INE", r.getMotivoRechazo(), "Motivos + observaciones");
            assertEquals(YO, r.getValidadoPor(), "Queda quién la rechazó (RNF-R01)");
            assertNull(r.getRevisandoPor(), "Se libera el bloqueo");
            verify(notificaciones).enviarCorreoRechazo(eq("quejoso@ipn.mx"), eq("Ana López"), eq(FOLIO), anyString());
        }

        @Test
        void rechazar_soloObservaciones_esValido() {
            tomadaPor(YO);
            Queja r = servicio.rechazar(FOLIO, null, "Solo observación", YO);
            assertEquals("Solo observación", r.getMotivoRechazo(), "Sin separador cuando no hay motivos");
        }

        @Test
        void rechazar_sinMotivosNiObservaciones_lanzaExcepcion() {
            tomadaPor(YO);
            RuntimeException ex = assertThrows(RuntimeException.class, () -> servicio.rechazar(FOLIO, List.of(), "  ", YO));
            assertEquals("Selecciona al menos un motivo o escribe una observación.", ex.getMessage(), "RN-R03");
            verify(quejaRepository, never()).save(any());
        }

        @Test
        void rechazar_sinHaberlaTomado_lanzaExcepcion() {
            RuntimeException ex = assertThrows(RuntimeException.class,
                    () -> servicio.rechazar(FOLIO, List.of("Motivo"), null, YO));
            assertTrue(ex.getMessage().contains("desde la Bandeja para tomarla"), "Hay que tomarla primero");
        }

        @Test
        void rechazar_tomadaPorOtra_lanzaExcepcionYNoNotifica() {
            tomadaPor(OTRA);
            assertThrows(RuntimeException.class, () -> servicio.rechazar(FOLIO, List.of("Motivo"), null, YO),
                    "Dos recepcionistas no pueden cerrar la misma queja");
            verify(notificaciones, never()).enviarCorreoRechazo(any(), any(), any(), any());
        }
    }

    @Nested
    @DisplayName("Turnado (CU-R04)")
    class Turnado {

        private PrimerContactoIngresoResponse respuestaPc(String folio) {
            PrimerContactoIngresoResponse r = new PrimerContactoIngresoResponse();
            r.setFolio(folio);
            return r;
        }

        @Test
        void turnar_quejaMia_creaExpedienteEnPrimerContacto() {
            tomadaPor(YO);
            when(primerContacto.enviarAPrimerContacto(queja)).thenReturn(respuestaPc(FOLIO_PC));
            PersonalAdministrativo defensor = new PersonalAdministrativo();
            defensor.setCorreoInstitucional("laura@ipn.mx");
            when(personalRepository.findFirstByNombreCompletoAndActivoTrue(DEFENSOR)).thenReturn(Optional.of(defensor));

            Queja r = servicio.turnar(FOLIO, DEFENSOR, "Urgente", YO);

            assertEquals(RevisionQuejaService.TURNADA, r.getEstatus(), "Pasa a TURNADA");
            assertEquals(FOLIO_PC, r.getFolioPrimerContacto(), "Guarda el folio de Primer Contacto");
            assertEquals("Primer Contacto", r.getAreaTurnada(), "Área fija");
            assertEquals(DEFENSOR, r.getDefensorAsignado(), "Defensor asignado");
            verify(notificaciones).notificarPersonal(eq("laura@ipn.mx"), anyString(), anyString(), eq(""));
        }

        @Test
        void turnar_sinDefensor_lanzaExcepcion() {
            RuntimeException ex = assertThrows(RuntimeException.class, () -> servicio.turnar(FOLIO, " ", null, YO));
            assertEquals("Selecciona el defensor responsable antes de turnar.", ex.getMessage(), "RN-R04");
            verify(primerContacto, never()).enviarAPrimerContacto(any());
        }

        @Test
        void turnar_yaTurnadaConFolio_esIdempotente() {
            queja.setEstatus("EN_ANALISIS");
            queja.setFolioPrimerContacto(FOLIO_PC);

            Queja r = servicio.turnar(FOLIO, DEFENSOR, null, YO);

            assertSame(queja, r, "Regresa la misma queja sin cambios");
            verify(primerContacto, never()).enviarAPrimerContacto(any());
            verify(quejaRepository, never()).save(any());
        }

        @Test
        void turnar_quejaRechazada_lanzaExcepcion() {
            queja.setEstatus(RevisionQuejaService.RECHAZADA);
            RuntimeException ex = assertThrows(RuntimeException.class, () -> servicio.turnar(FOLIO, DEFENSOR, null, YO));
            assertEquals("Una queja rechazada no puede ser turnada.", ex.getMessage(), "RN-R01");
        }

        @Test
        void turnar_primerContactoFalla_noGuardaLaQueja() {
            tomadaPor(YO);
            when(primerContacto.enviarAPrimerContacto(queja)).thenThrow(new RuntimeException("Primer Contacto no responde"));
            assertThrows(RuntimeException.class, () -> servicio.turnar(FOLIO, DEFENSOR, null, YO), "Propaga el error");
            verify(quejaRepository, never()).save(any());
            assertEquals(RevisionQuejaService.EN_VALIDACION, queja.getEstatus(), "La queja sigue en validación");
        }

        @Test
        void turnar_quejaInexistente_lanzaExcepcion() {
            when(quejaRepository.findByNumeroFolio("FOL-NOEXISTE")).thenReturn(Optional.empty());
            RuntimeException ex = assertThrows(RuntimeException.class,
                    () -> servicio.turnar("FOL-NOEXISTE", DEFENSOR, null, YO));
            assertTrue(ex.getMessage().contains("FOL-NOEXISTE"), "Menciona el folio");
        }
    }

    @Nested
    @DisplayName("Registro manual y ventanilla (CU-R05)")
    class Manual {

        @Test
        void registrarManual_sinCorreo_usaCorreoDeReferenciaInterno() {
            Queja r = servicio.registrarManual("Ana", "López", null, "externo", "ESCOM", "OF-1",
                    LocalDate.of(2026, 10, 1), "ESCRITO", "Descripción del asunto", "Archivo 3", null, YO, null, null);

            assertEquals("MANUAL", r.getOrigenRegistro(), "RN-R05: origen MANUAL");
            assertEquals(RevisionQuejaService.RECIBIDA, r.getEstatus(), "Entra como RECIBIDA");
            assertTrue(r.getCorreoInstitucional().startsWith("registro-manual+fol-"), "Correo interno de referencia");
            assertEquals("externo", r.getTipoUsuarioManual(), "Admite tipo externo");
        }

        @Test
        void registrarManual_conCorreoDeContacto_loUsa() {
            Queja r = servicio.registrarManual("Ana", "López", null, "alumno", "ESCOM", null,
                    null, "ESCRITO", "Descripción", null, null, YO, " ana@correo.com ", "5555555555");
            assertEquals("ana@correo.com", r.getCorreoInstitucional(), "Se usa el correo capturado");
        }

        @Test
        void registrarManual_sinDescripcion_lanzaExcepcion() {
            assertThrows(RuntimeException.class, () -> servicio.registrarManual("Ana", "López", null, null, null, null,
                    null, null, " ", null, null, YO, null, null), "Descripción obligatoria");
        }

        @Test
        void marcarVentanilla_quejaReciente_laMarcaComoManual() {
            queja.setFechaCreacion(LocalDateTime.now().minusMinutes(5));
            servicio.marcarVentanilla(FOLIO, new DatosVentanillaRequest(null, " OF-9 ", "2026-10-20", null), YO);
            assertEquals("MANUAL", queja.getOrigenRegistro(), "Se marca como manual");
            assertEquals("FORMATO_QUEJA", queja.getTipoDocumentoFisico(), "Tipo por defecto");
            assertEquals("OF-9", queja.getNumeroOficio(), "Oficio sin espacios");
            assertEquals(LocalDate.of(2026, 10, 20), queja.getFechaRecepcionFisica(), "Fecha física");
        }

        @Test
        void marcarVentanilla_quejaDeMasDeDosHoras_lanzaExcepcion() {
            queja.setFechaCreacion(LocalDateTime.now().minusHours(2).minusMinutes(1));
            assertThrows(RuntimeException.class,
                    () -> servicio.marcarVentanilla(FOLIO, new DatosVentanillaRequest(null, null, null, null), YO),
                    "Pasadas 2 horas ya no se reclasifica");
        }

        @Test
        void marcarVentanilla_fechaInvalida_lanzaExcepcion() {
            queja.setFechaCreacion(LocalDateTime.now());
            RuntimeException ex = assertThrows(RuntimeException.class,
                    () -> servicio.marcarVentanilla(FOLIO, new DatosVentanillaRequest(null, null, "20/10/2026", null), YO));
            assertTrue(ex.getMessage().contains("AAAA-MM-DD"), "Indica el formato esperado");
        }
    }

    @Nested
    @DisplayName("Historial")
    class Historial {

        @Test
        void historialPagina_tamanioMayorAlMaximo_seLimitaA50() {
            Page<Queja> vacia = new PageImpl<>(List.of());
            when(quejaRepository.buscarHistorial(any(), eq(false), anyString(), any(), any(), anyString(), anyString(), any()))
                    .thenReturn(vacia);
            when(quejaRepository.resumenHistorial(any(), eq(false), anyString(), any(), any(), anyString(), anyString()))
                    .thenReturn(List.of());

            servicio.historialPagina(YO, null, "RECHAZADO", null, null, false, -3, 500);

            ArgumentCaptor<Pageable> pagina = ArgumentCaptor.forClass(Pageable.class);
            ArgumentCaptor<List<String>> estatus = ArgumentCaptor.captor();
            verify(quejaRepository).buscarHistorial(estatus.capture(), eq(false), anyString(), any(), any(), anyString(),
                    anyString(), pagina.capture());
            assertEquals(TAMANIO_MAXIMO, pagina.getValue().getPageSize(), "Máximo 50 por página");
            assertEquals(0, pagina.getValue().getPageNumber(), "Página negativa se corrige a 0");
            assertEquals(List.of(RevisionQuejaService.RECHAZADA), estatus.getValue(), "Filtro solo rechazadas");
        }
    }
}
