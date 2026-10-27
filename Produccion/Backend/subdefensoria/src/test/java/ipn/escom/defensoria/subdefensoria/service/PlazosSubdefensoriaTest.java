package ipn.escom.defensoria.subdefensoria.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import ipn.escom.defensoria.subdefensoria.dto.AlertaVencimientoDTO;
import ipn.escom.defensoria.subdefensoria.dto.ControlPlazoDTO;
import ipn.escom.defensoria.subdefensoria.entity.EstatusOficio;
import ipn.escom.defensoria.subdefensoria.entity.ExpedienteInvestigacion;
import ipn.escom.defensoria.subdefensoria.entity.OficioInformacion;
import ipn.escom.defensoria.subdefensoria.exception.OperacionInvalidaException;
import ipn.escom.defensoria.subdefensoria.repository.ExpedienteInvestigacionRepository;
import ipn.escom.defensoria.subdefensoria.repository.OficioInformacionRepository;

/** Semáforo de plazos (P14), alertas de vencimiento (P17) y validación del archivo de respuesta. */
@DisplayName("Subdefensoría — plazos, alertas y archivos de respuesta")
class PlazosSubdefensoriaTest {

    private static final int DIAS_PRIMERA = 10;
    private static final int DIAS_RECORDATORIO = 5;

    private static OficioInformacion oficio(String estatus, LocalDate limite, String tipoPlazo) {
        return OficioInformacion.builder().id(7L).expedienteId(1L).folio("SUB-1").numeroOficio("OF-1")
                .fase("PRIMERA").estatus(estatus).fechaLimite(limite).fechaEnvio(LocalDate.now().minusDays(1))
                .tipoPlazo(tipoPlazo).build();
    }

    @Nested
    @ExtendWith(MockitoExtension.class)
    @DisplayName("ControlPlazoService — semáforo")
    class Semaforo {

        @Mock
        private ExpedienteInvestigacionRepository expedientes;
        @Mock
        private OficioInformacionRepository oficios;

        private ControlPlazoService servicio() {
            return new ControlPlazoService(expedientes, oficios, DIAS_PRIMERA, DIAS_RECORDATORIO);
        }

        private void unExpedienteConOficio(OficioInformacion o) {
            ExpedienteInvestigacion e = ExpedienteInvestigacion.builder().id(1L).folio("SUB-1").build();
            when(expedientes.findByEstatusInOrderByFechaAdmisionAsc(any())).thenReturn(List.of(e));
            when(oficios.findFirstByExpedienteIdAndEstatusNotOrderByFechaEnvioDesc(1L, EstatusOficio.RESPONDIDO))
                    .thenReturn(Optional.ofNullable(o));
        }

        @Test
        void obtenerSemaforo_primeraSolicitud_usaDiezDias() {
            unExpedienteConOficio(oficio(EstatusOficio.EN_ESPERA, LocalDate.now().plusDays(5), "PRIMERA"));
            List<ControlPlazoDTO> r = servicio().obtenerSemaforo();
            assertEquals(DIAS_PRIMERA, r.get(0).getDiasLimite(), "10 días hábiles en la primera solicitud");
            assertEquals(EstatusOficio.EN_ESPERA, r.get(0).getEstatusOficio(), "Sigue en espera");
        }

        @Test
        void obtenerSemaforo_recordatorio_usaCincoDias() {
            unExpedienteConOficio(oficio(EstatusOficio.EN_ESPERA, LocalDate.now().plusDays(2), "RECORDATORIO"));
            assertEquals(DIAS_RECORDATORIO, servicio().obtenerSemaforo().get(0).getDiasLimite(), "5 días en recordatorio");
        }

        @Test
        void obtenerSemaforo_limiteVencidoAyer_loMarcaVencido() {
            OficioInformacion o = oficio(EstatusOficio.EN_ESPERA, LocalDate.now().minusDays(1), "PRIMERA");
            unExpedienteConOficio(o);
            when(oficios.save(o)).thenReturn(o);
            assertEquals(EstatusOficio.VENCIDO, servicio().obtenerSemaforo().get(0).getEstatusOficio(), "Vencido");
        }

        @Test
        void obtenerSemaforo_limiteHoy_aunNoVence() {
            OficioInformacion o = oficio(EstatusOficio.EN_ESPERA, LocalDate.now(), "PRIMERA");
            unExpedienteConOficio(o);
            servicio().obtenerSemaforo();
            verify(oficios, never()).save(any());
        }

        @Test
        void obtenerSemaforo_expedienteSinOficioVigente_noAparece() {
            unExpedienteConOficio(null);
            assertTrue(servicio().obtenerSemaforo().isEmpty(), "Sin oficio no hay fila");
        }
    }

    @Nested
    @ExtendWith(MockitoExtension.class)
    @DisplayName("AlertaService — plazos vencidos")
    class Alertas {

        @Mock
        private OficioInformacionRepository oficios;
        @InjectMocks
        private AlertaService servicio;

        @Test
        void obtenerVencidos_maduraLosPendientesAntesDeListar() {
            OficioInformacion vencido = oficio(EstatusOficio.EN_ESPERA, LocalDate.now().minusDays(3), "PRIMERA");
            OficioInformacion vigente = oficio(EstatusOficio.EN_ESPERA, LocalDate.now().plusDays(3), "PRIMERA");
            when(oficios.findByEstatusOrderByFechaLimiteAsc(EstatusOficio.EN_ESPERA)).thenReturn(List.of(vencido, vigente));
            when(oficios.findByEstatusOrderByFechaLimiteAsc(EstatusOficio.VENCIDO)).thenReturn(List.of(vencido));

            List<AlertaVencimientoDTO> r = servicio.obtenerVencidos();

            assertEquals(EstatusOficio.VENCIDO, vencido.getEstatus(), "El vencido se marca");
            assertEquals(EstatusOficio.EN_ESPERA, vigente.getEstatus(), "El vigente no cambia");
            verify(oficios, never()).save(vigente);
            assertEquals(1, r.size(), "Solo un vencido");
        }
    }

    @Nested
    @DisplayName("ArchivoRespuestaValidador — respuesta del director")
    class ArchivoRespuesta {

        private static final byte[] PDF = {'%', 'P', 'D', 'F', '-', '1', '.', '7'};
        private static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A};
        private static final byte[] DOCX = {'P', 'K', 3, 4, 0, 0, 0, 0};

        @Test
        void validar_pdf_regresaTipoRealYNombreSeguro() {
            ArchivoRespuestaValidador.Validado v = ArchivoRespuestaValidador.validar(
                    new MockMultipartFile("archivo", "resp:uesta?.pdf", "text/plain", PDF));
            assertEquals("application/pdf", v.tipo(), "Tipo por firma, no por Content-Type");
            assertEquals("resp_uesta_.pdf", v.nombre(), "Se reemplazan caracteres no válidos");
        }

        @Test
        void validar_png_esAceptado() {
            assertEquals("image/png", ArchivoRespuestaValidador.validar(
                    new MockMultipartFile("archivo", "foto.png", "image/png", PNG)).tipo(), "PNG válido");
        }

        @Test
        void validar_word_lanzaExcepcion() {
            OperacionInvalidaException ex = assertThrows(OperacionInvalidaException.class,
                    () -> ArchivoRespuestaValidador.validar(new MockMultipartFile("archivo", "r.pdf", "application/pdf", DOCX)));
            assertEquals("Solo se aceptan archivos PDF, JPG o PNG.", ex.getMessage(), "Un .docx renombrado se rechaza");
        }

        @Test
        void validar_vacio_lanzaExcepcion() {
            assertThrows(OperacionInvalidaException.class,
                    () -> ArchivoRespuestaValidador.validar(new MockMultipartFile("archivo", new byte[0])), "Vacío");
        }

        @Test
        void validar_diezMBExactos_esValido() {
            byte[] contenido = Arrays.copyOf(PDF, Math.toIntExact(ArchivoRespuestaValidador.TAMANO_MAXIMO));
            assertEquals("application/pdf", ArchivoRespuestaValidador.validar(
                    new MockMultipartFile("archivo", "r.pdf", null, contenido)).tipo(), "10 MB es el borde válido");
        }

        @Test
        void validar_diezMBMasUnByte_lanzaExcepcion() {
            byte[] contenido = Arrays.copyOf(PDF, Math.toIntExact(ArchivoRespuestaValidador.TAMANO_MAXIMO + 1));
            OperacionInvalidaException ex = assertThrows(OperacionInvalidaException.class,
                    () -> ArchivoRespuestaValidador.validar(new MockMultipartFile("archivo", "r.pdf", null, contenido)));
            assertEquals("El archivo excede el tamaño máximo de 10 MB.", ex.getMessage(), "Excede 10 MB");
        }
    }
}
