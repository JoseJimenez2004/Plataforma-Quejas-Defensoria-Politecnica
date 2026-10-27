package ipn.escom.defensoria.primercontacto.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import ipn.escom.defensoria.primercontacto.dto.CitaDTO;
import ipn.escom.defensoria.primercontacto.dto.CrearCitaDTO;
import ipn.escom.defensoria.primercontacto.dto.ReagendarCitaDTO;
import ipn.escom.defensoria.primercontacto.entity.CitaPrimerContacto;
import ipn.escom.defensoria.primercontacto.entity.EstatusExpediente;
import ipn.escom.defensoria.primercontacto.entity.ExpedientePrimerContacto;
import ipn.escom.defensoria.primercontacto.entity.PersonalAdministrativo;
import ipn.escom.defensoria.primercontacto.exception.OperacionInvalidaException;
import ipn.escom.defensoria.primercontacto.exception.RecursoNoEncontradoException;
import ipn.escom.defensoria.primercontacto.repository.CitaPrimerContactoRepository;
import ipn.escom.defensoria.primercontacto.repository.ExpedientePrimerContactoRepository;

/** Pruebas unitarias de {@link CitaPrimerContactoService}: agenda de citas de Primer Contacto. */
@ExtendWith(MockitoExtension.class)
@DisplayName("CitaPrimerContactoService — agenda de citas")
class CitaPrimerContactoServiceTest {

    private static final long HORAS_RESPUESTA = 48;
    private static final long DURACION_MINUTOS = 60;
    private static final String FOLIO = "PC-A1B2C3D4";
    private static final Long EXPEDIENTE_ID = 1L;
    private static final Long ANALISTA_ID = 20L;
    private static final LocalDate FECHA = LocalDate.of(2026, 11, 3);
    private static final String CORREO_QUEJOSO = "quejoso@ipn.mx";

    @Mock
    private CitaPrimerContactoRepository citas;
    @Mock
    private ExpedientePrimerContactoRepository expedientes;
    @Mock
    private NotificacionQuejosoService notificaciones;

    private CitaPrimerContactoService servicio;
    private ExpedientePrimerContacto expediente;
    private PersonalAdministrativo analista;

    @BeforeEach
    void setUp() {
        servicio = new CitaPrimerContactoService(citas, expedientes, notificaciones, HORAS_RESPUESTA,
                DURACION_MINUTOS, "https://portal.test/");
        expediente = new ExpedientePrimerContacto();
        expediente.setId(EXPEDIENTE_ID);
        expediente.setFolio(FOLIO);
        expediente.setFolioOrigen("FOL-ABCD1234");
        expediente.setQuejosoNombre("Ana López");
        expediente.setQuejosoCorreo(CORREO_QUEJOSO);
        expediente.setEstatus(EstatusExpediente.EN_ANALISIS);
        analista = new PersonalAdministrativo();
        analista.setId(ANALISTA_ID);
        analista.setNombreCompleto("Analista PC");
        lenient().when(citas.save(any(CitaPrimerContacto.class))).thenAnswer(inv -> inv.getArgument(0));
        lenient().when(expedientes.findById(EXPEDIENTE_ID)).thenReturn(Optional.of(expediente));
    }

    private static CrearCitaDTO crear(String hora, String categoria) {
        return CrearCitaDTO.builder().folio(FOLIO).fechaCita(FECHA.toString()).horaCita(hora)
                .tipoCita("PRESENCIAL").categoria(categoria).motivo("Comparecer").build();
    }

    private static CitaPrimerContacto citaExistente(long id, String hora, String estatus) {
        return CitaPrimerContacto.builder().id(id).expedienteId(EXPEDIENTE_ID).folio("PC-OTRO")
                .fechaCita(FECHA).horaCita(LocalTime.parse(hora)).estatus(estatus).build();
    }

    private void expedienteSinCita() {
        when(expedientes.findByFolio(FOLIO)).thenReturn(Optional.of(expediente));
        when(citas.existsByExpedienteIdAndEstatusNotIn(eq(EXPEDIENTE_ID), any())).thenReturn(false);
    }

    @Nested
    @DisplayName("Crear cita")
    class Crear {

        @Test
        void crearCita_horarioLibre_quedaProgramadaConPlazoYToken() {
            expedienteSinCita();
            when(citas.findByAnalistaIdAndFechaCitaAndEstatusIn(eq(ANALISTA_ID), eq(FECHA), any())).thenReturn(List.of());

            CitaDTO r = servicio.crearCita(crear("10:00", "seguimiento"), analista);

            assertEquals(CitaPrimerContactoService.PROGRAMADA, r.getEstatus(), "Nace PROGRAMADA");
            assertEquals("SEGUIMIENTO", r.getCategoria(), "Categoría normalizada en mayúsculas");
            ArgumentCaptor<CitaPrimerContacto> guardada = ArgumentCaptor.forClass(CitaPrimerContacto.class);
            verify(citas).save(guardada.capture());
            assertNotNull(guardada.getValue().getTokenRespuesta(), "Token para responder por enlace");
            assertTrue(guardada.getValue().getFechaLimiteRespuesta().isAfter(LocalDateTime.now().plusHours(HORAS_RESPUESTA - 1)),
                    "Plazo de 48 h para responder");
            verify(notificaciones).notificar(eq(expediente), eq(NotificacionQuejosoService.TIPO_CITA), anyString(),
                    anyString(), anyString());
        }

        @ParameterizedTest(name = "{0} -> {1}")
        @CsvSource(nullValues = "NULO", value = {"NULO, COMPARECENCIA", "' ', COMPARECENCIA", "Revisión, REVISION",
                "REVISION, REVISION", "otra cosa, COMPARECENCIA"})
        void crearCita_normalizaLaCategoria(String entrada, String esperada) {
            expedienteSinCita();
            when(citas.findByAnalistaIdAndFechaCitaAndEstatusIn(any(), any(), any())).thenReturn(List.of());
            assertEquals(esperada, servicio.crearCita(crear("10:00", entrada), analista).getCategoria(),
                    "Categoría para '" + entrada + "'");
        }

        @Test
        void crearCita_expedienteCerrado_lanzaExcepcion() {
            expediente.setEstatus(EstatusExpediente.PROCEDENTE);
            when(expedientes.findByFolio(FOLIO)).thenReturn(Optional.of(expediente));
            OperacionInvalidaException ex = assertThrows(OperacionInvalidaException.class,
                    () -> servicio.crearCita(crear("10:00", null), analista));
            assertEquals("Solo se agendan citas para expedientes en análisis.", ex.getMessage(), "Expediente cerrado");
        }

        @Test
        void crearCita_yaTieneCitaActiva_lanzaExcepcion() {
            when(expedientes.findByFolio(FOLIO)).thenReturn(Optional.of(expediente));
            when(citas.existsByExpedienteIdAndEstatusNotIn(eq(EXPEDIENTE_ID), any())).thenReturn(true);
            assertThrows(OperacionInvalidaException.class, () -> servicio.crearCita(crear("10:00", null), analista),
                    "Una cita activa por expediente");
        }

        @Test
        void crearCita_folioInexistente_lanzaExcepcion() {
            when(expedientes.findByFolio(FOLIO)).thenReturn(Optional.empty());
            assertThrows(RecursoNoEncontradoException.class, () -> servicio.crearCita(crear("10:00", null), analista),
                    "Folio inexistente");
        }

        @Test
        void crearCita_traslapeDe59Minutos_lanzaExcepcion() {
            expedienteSinCita();
            when(citas.findByAnalistaIdAndFechaCitaAndEstatusIn(any(), any(), any()))
                    .thenReturn(List.of(citaExistente(9, "10:00", CitaPrimerContactoService.PROGRAMADA)));
            OperacionInvalidaException ex = assertThrows(OperacionInvalidaException.class,
                    () -> servicio.crearCita(crear("10:59", null), analista));
            assertTrue(ex.getMessage().startsWith("Ya tienes una cita ese día a las 10:00 h"), "Indica el empalme");
            verify(citas, never()).save(any());
        }

        @Test
        void crearCita_separacionDeExactamente60Minutos_esValida() {
            expedienteSinCita();
            when(citas.findByAnalistaIdAndFechaCitaAndEstatusIn(any(), any(), any()))
                    .thenReturn(List.of(citaExistente(9, "10:00", CitaPrimerContactoService.PROGRAMADA)));
            assertEquals(CitaPrimerContactoService.PROGRAMADA, servicio.crearCita(crear("11:00", null), analista).getEstatus(),
                    "60 minutos es el borde válido");
        }
    }

    @Nested
    @DisplayName("Reagendar, confirmar y cancelar")
    class Cambios {

        private CitaPrimerContacto activa;

        @BeforeEach
        void citaActiva() {
            activa = citaExistente(5, "10:00", CitaPrimerContactoService.PROGRAMADA);
            activa.setCategoria("REVISION");
            activa.setTipoCita("VIRTUAL");
            activa.setMotivo("Original");
            lenient().when(citas.findById(5L)).thenReturn(Optional.of(activa));
        }

        @Test
        void reagendar_dejaLaAnteriorEnHistorialYCreaNuevaConSuCategoria() {
            when(citas.findByAnalistaIdAndFechaCitaAndEstatusIn(any(), any(), any())).thenReturn(List.of(activa));
            ReagendarCitaDTO dto = ReagendarCitaDTO.builder().fechaCita(FECHA.toString()).horaCita("10:30").build();

            CitaDTO nueva = servicio.reagendarCita(5L, dto, analista);

            assertEquals(CitaPrimerContactoService.REAGENDADA, activa.getEstatus(), "La anterior queda REAGENDADA");
            assertEquals("REVISION", nueva.getCategoria(), "Conserva la categoría anterior");
            assertEquals(5L, nueva.getCitaAnteriorId(), "Apunta a la cita anterior");
            assertEquals("Original", nueva.getMotivo(), "Conserva el motivo");
        }

        @Test
        void confirmarCita_yaConfirmada_lanzaExcepcion() {
            activa.setEstatus(CitaPrimerContactoService.CONFIRMADA);
            assertThrows(OperacionInvalidaException.class, () -> servicio.confirmarCita(5L, analista), "Ya confirmada");
        }

        @Test
        void cancelarCita_cancelada_lanzaExcepcion() {
            activa.setEstatus(CitaPrimerContactoService.CANCELADA);
            OperacionInvalidaException ex = assertThrows(OperacionInvalidaException.class,
                    () -> servicio.cancelarCita(5L, analista));
            assertEquals("La cita ya está cancelada.", ex.getMessage(), "No se cancela dos veces");
        }

        @Test
        void registrarCancelacionQuejoso_sinMotivo_lanzaExcepcion() {
            OperacionInvalidaException ex = assertThrows(OperacionInvalidaException.class,
                    () -> servicio.registrarCancelacionQuejoso(5L, "  ", analista));
            assertEquals("Indica el motivo de la cancelación.", ex.getMessage(), "Motivo obligatorio");
        }

        @Test
        void cancelarPorToken_plazoVencido_lanzaExcepcion() {
            activa.setFechaLimiteRespuesta(LocalDateTime.now().minusMinutes(1));
            when(citas.findByTokenRespuesta("tok")).thenReturn(Optional.of(activa));
            OperacionInvalidaException ex = assertThrows(OperacionInvalidaException.class,
                    () -> servicio.cancelarPorToken(" tok ", "No puedo"));
            assertTrue(ex.getMessage().startsWith("El plazo para responder esta cita ya venció"), "Plazo vencido");
        }

        @Test
        void confirmarPorToken_enPlazo_quedaConfirmadaPorElQuejoso() {
            activa.setFechaLimiteRespuesta(LocalDateTime.now().plusHours(1));
            when(citas.findByTokenRespuesta("tok")).thenReturn(Optional.of(activa));
            servicio.confirmarPorToken("tok");
            assertEquals(CitaPrimerContactoService.CONFIRMADA, activa.getEstatus(), "Confirmada");
            assertEquals(CitaPrimerContactoService.RESPUESTA_QUEJOSO, activa.getRespuestaRegistradaPor(), "La registró el quejoso");
        }

        @Test
        void citaPorToken_vacio_lanzaExcepcion() {
            assertThrows(RecursoNoEncontradoException.class, () -> servicio.citaPorToken(" "), "Token vacío");
        }

        @Test
        void confirmarPorQuejoso_citaDeOtroQuejoso_respondeComoNoEncontrada() {
            ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                    () -> servicio.confirmarPorQuejoso(5L, "otro@ipn.mx"));
            assertEquals(404, ex.getStatusCode().value(), "No revela que la cita existe");
        }

        @Test
        void marcarCitasSinRespuesta_pasaLasVencidasASinRespuesta() {
            when(citas.findByEstatusAndFechaLimiteRespuestaBefore(eq(CitaPrimerContactoService.PROGRAMADA), any()))
                    .thenReturn(List.of(activa));
            servicio.marcarCitasSinRespuesta();
            assertEquals(CitaPrimerContactoService.SIN_RESPUESTA, activa.getEstatus(), "Sin respuesta");
        }
    }

    @Nested
    @DisplayName("Agenda por periodo")
    class Rango {

        @Test
        void citasEnRango_finAntesDelInicio_lanzaExcepcion() {
            assertThrows(OperacionInvalidaException.class, () -> servicio.citasEnRango("2026-11-10", "2026-11-09"),
                    "Rango invertido");
        }

        @Test
        void citasEnRango_400Dias_esElMaximo() {
            when(citas.findByFechaCitaBetweenOrderByFechaCitaAscHoraCitaAsc(any(), any())).thenReturn(List.of());
            assertTrue(servicio.citasEnRango("2026-01-01", "2027-02-05").isEmpty(), "400 días es válido");
        }

        @Test
        void citasEnRango_401Dias_lanzaExcepcion() {
            OperacionInvalidaException ex = assertThrows(OperacionInvalidaException.class,
                    () -> servicio.citasEnRango("2026-01-01", "2027-02-06"));
            assertEquals("Consulta como máximo un año de citas a la vez.", ex.getMessage(), "Más de 400 días");
        }

        @Test
        void citasEnRango_ocultaLasReagendadas() {
            when(citas.findByFechaCitaBetweenOrderByFechaCitaAscHoraCitaAsc(any(), any())).thenReturn(List.of(
                    citaExistente(1, "09:00", CitaPrimerContactoService.REAGENDADA),
                    citaExistente(2, "10:00", CitaPrimerContactoService.PROGRAMADA)));
            List<CitaDTO> r = servicio.citasEnRango("2026-11-01", "2026-11-30");
            assertEquals(1, r.size(), "La reagendada no se muestra");
            assertFalse(r.stream().anyMatch(c -> CitaPrimerContactoService.REAGENDADA.equals(c.getEstatus())), "Solo vigentes");
        }
    }
}
