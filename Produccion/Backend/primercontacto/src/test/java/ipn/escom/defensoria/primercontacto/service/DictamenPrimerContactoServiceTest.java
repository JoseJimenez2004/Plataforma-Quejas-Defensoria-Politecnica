package ipn.escom.defensoria.primercontacto.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import ipn.escom.defensoria.primercontacto.dto.CompetenciaDTO;
import ipn.escom.defensoria.primercontacto.dto.DictamenDTO;
import ipn.escom.defensoria.primercontacto.dto.ExpedienteEntranteRequest;
import ipn.escom.defensoria.primercontacto.dto.ImprocedenciaDTO;
import ipn.escom.defensoria.primercontacto.dto.SubdefensoriaIngresoResponse;
import ipn.escom.defensoria.primercontacto.entity.AcuerdoConciliacion;
import ipn.escom.defensoria.primercontacto.entity.DictamenPrimerContacto;
import ipn.escom.defensoria.primercontacto.entity.EstatusExpediente;
import ipn.escom.defensoria.primercontacto.entity.ExpedientePrimerContacto;
import ipn.escom.defensoria.primercontacto.entity.PersonalAdministrativo;
import ipn.escom.defensoria.primercontacto.exception.OperacionInvalidaException;
import ipn.escom.defensoria.primercontacto.repository.AcuerdoConciliacionRepository;
import ipn.escom.defensoria.primercontacto.repository.DictamenPrimerContactoRepository;
import ipn.escom.defensoria.primercontacto.repository.ExpedientePrimerContactoRepository;

/** Pruebas unitarias de {@link DictamenPrimerContactoService}: dictamen competente / improcedente. */
@ExtendWith(MockitoExtension.class)
@DisplayName("DictamenPrimerContactoService — dictamen de Primer Contacto")
class DictamenPrimerContactoServiceTest {

    private static final String FOLIO = "PC-A1B2C3D4";
    private static final String FOLIO_SUB = "SUB-0001";
    private static final String ABOGADO = "Lic. Laura Méndez";

    @Mock
    private DictamenPrimerContactoRepository dictamenes;
    @Mock
    private ExpedientePrimerContactoRepository expedientes;
    @Mock
    private AcuerdoConciliacionRepository acuerdos;
    @Mock
    private SubdefensoriaClientService subdefensoria;
    @Mock
    private TransicionExpedienteService transicion;
    @Mock
    private NotificacionQuejosoService notificaciones;

    @InjectMocks
    private DictamenPrimerContactoService servicio;

    private ExpedientePrimerContacto expediente;
    private PersonalAdministrativo analista;

    @BeforeEach
    void setUp() {
        expediente = new ExpedientePrimerContacto();
        expediente.setId(1L);
        expediente.setFolio(FOLIO);
        expediente.setFolioOrigen("FOL-ABCD1234");
        expediente.setEstatus(EstatusExpediente.EN_ANALISIS);
        analista = new PersonalAdministrativo();
        analista.setId(20L);
        analista.setNombreCompleto("Analista PC");
        lenient().when(transicion.obtenerPorFolio(FOLIO)).thenReturn(expediente);
        lenient().when(dictamenes.save(any())).thenAnswer(inv -> inv.getArgument(0));
        lenient().when(acuerdos.findByNumeroFolioAndFechaEmisionGreaterThanEqualOrderByFechaEmisionDesc(any(), any()))
                .thenReturn(List.of());
    }

    private static CompetenciaDTO competencia() {
        return CompetenciaDTO.builder().folio(FOLIO).justificacion("Es competencia de la DDP")
                .responsableTurno(ABOGADO).observaciones("Urgente").build();
    }

    @Test
    void registrarCompetencia_pasaAProcedenteYEnviaASubdefensoria() {
        when(subdefensoria.enviarExpediente(any())).thenReturn(SubdefensoriaIngresoResponse.builder().folio(FOLIO_SUB).build());

        DictamenDTO r = servicio.registrarCompetencia(competencia(), analista);

        assertEquals(DictamenPrimerContactoService.RESULTADO_COMPETENTE, r.getResultado(), "Resultado competente");
        verify(transicion).cambiarEstatus(expediente, EstatusExpediente.PROCEDENTE);
        ArgumentCaptor<ExpedienteEntranteRequest> solicitud = ArgumentCaptor.forClass(ExpedienteEntranteRequest.class);
        verify(subdefensoria).enviarExpediente(solicitud.capture());
        assertEquals(FOLIO, solicitud.getValue().getFolioOrigen(), "Se relaciona por folio PC, no por id");
        assertEquals(ABOGADO, solicitud.getValue().getAbogadoAsesorNombre(), "Abogado elegido");
        assertEquals(FOLIO_SUB, expediente.getFolioSubdefensoria(), "Guarda el folio de Subdefensoría");
    }

    @Test
    void registrarCompetencia_subdefensoriaCaida_elDictamenSeConserva() {
        when(subdefensoria.enviarExpediente(any())).thenThrow(new RuntimeException("Connection refused"));

        DictamenDTO r = servicio.registrarCompetencia(competencia(), analista);

        assertEquals(DictamenPrimerContactoService.RESULTADO_COMPETENTE, r.getResultado(), "El dictamen queda guardado");
        verify(transicion).cambiarEstatus(expediente, EstatusExpediente.PROCEDENTE);
        assertNull(expediente.getFolioSubdefensoria(), "Queda pendiente de reenvío");
    }

    @Test
    void registrarCompetencia_yaTieneDictamen_lanzaExcepcion() {
        when(dictamenes.existsByExpedienteId(1L)).thenReturn(true);
        OperacionInvalidaException ex = assertThrows(OperacionInvalidaException.class,
                () -> servicio.registrarCompetencia(competencia(), analista));
        assertTrue(ex.getMessage().contains("ya cuenta con un dictamen"), "Un solo dictamen por expediente");
        verify(dictamenes, never()).save(any());
    }

    @Test
    void registrarCompetencia_expedienteYaNoEnAnalisis_lanzaExcepcion() {
        expediente.setEstatus(EstatusExpediente.REMITIDA);
        assertThrows(OperacionInvalidaException.class, () -> servicio.registrarCompetencia(competencia(), analista),
                "Solo se dictamina un expediente abierto");
    }

    @Test
    void registrarCompetencia_conciliacionPendiente_lanzaExcepcion() {
        AcuerdoConciliacion pendiente = new AcuerdoConciliacion();
        pendiente.setEstado(AcuerdoConciliacion.PENDIENTE);
        when(acuerdos.findByNumeroFolioAndFechaEmisionGreaterThanEqualOrderByFechaEmisionDesc(any(), any()))
                .thenReturn(List.of(pendiente));
        OperacionInvalidaException ex = assertThrows(OperacionInvalidaException.class,
                () -> servicio.registrarCompetencia(competencia(), analista));
        assertTrue(ex.getMessage().startsWith("Hay un acuerdo de conciliación esperando"), "Esperar al quejoso");
    }

    @Test
    void registrarImprocedencia_pasaAImprocedenteYNotifica() {
        DictamenDTO r = servicio.registrarImprocedencia(
                ImprocedenciaDTO.builder().folio(FOLIO).justificacion("No es competencia").build(), analista);

        assertEquals(DictamenPrimerContactoService.RESULTADO_IMPROCEDENTE, r.getResultado(), "Resultado improcedente");
        verify(transicion).cambiarEstatus(expediente, EstatusExpediente.IMPROCEDENTE);
        verify(notificaciones).notificar(eq(expediente), eq(NotificacionQuejosoService.TIPO_CAMBIO_ESTATUS),
                anyString(), anyString());
        verify(subdefensoria, never()).enviarExpediente(any());
    }

    @Test
    void reenviarASubdefensoria_noProcedente_lanzaExcepcion() {
        assertThrows(OperacionInvalidaException.class, () -> servicio.reenviarASubdefensoria(FOLIO),
                "Solo se reenvía un procedente");
    }

    @Test
    void reenviarASubdefensoria_yaRecibido_lanzaExcepcion() {
        expediente.setEstatus(EstatusExpediente.PROCEDENTE);
        expediente.setFolioSubdefensoria(FOLIO_SUB);
        OperacionInvalidaException ex = assertThrows(OperacionInvalidaException.class,
                () -> servicio.reenviarASubdefensoria(FOLIO));
        assertTrue(ex.getMessage().contains(FOLIO_SUB), "Indica el folio ya asignado");
    }

    @Test
    void reenviarASubdefensoria_sigueSinResponder_lanzaExcepcion() {
        expediente.setEstatus(EstatusExpediente.PROCEDENTE);
        when(dictamenes.findByExpedienteId(1L)).thenReturn(Optional.of(DictamenPrimerContacto.builder().build()));
        when(subdefensoria.enviarExpediente(any())).thenThrow(new RuntimeException("timeout"));
        OperacionInvalidaException ex = assertThrows(OperacionInvalidaException.class,
                () -> servicio.reenviarASubdefensoria(FOLIO));
        assertTrue(ex.getMessage().startsWith("Subdefensoría sigue sin responder"), "Se puede reintentar después");
    }
}
