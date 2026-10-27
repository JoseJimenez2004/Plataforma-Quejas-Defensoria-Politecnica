package ipn.escom.defensoria.primercontacto.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import ipn.escom.defensoria.primercontacto.entity.EstatusExpediente;
import ipn.escom.defensoria.primercontacto.entity.ExpedientePrimerContacto;
import ipn.escom.defensoria.primercontacto.entity.QuejaReferencia;
import ipn.escom.defensoria.primercontacto.exception.RecursoNoEncontradoException;
import ipn.escom.defensoria.primercontacto.repository.ExpedientePrimerContactoRepository;
import ipn.escom.defensoria.primercontacto.repository.QuejaReferenciaRepository;

/** Pruebas unitarias de {@link TransicionExpedienteService}: estatus del expediente y de la queja. */
@ExtendWith(MockitoExtension.class)
@DisplayName("TransicionExpedienteService — sincronización de estatus")
class TransicionExpedienteServiceTest {

    private static final String FOLIO_QUEJA = "FOL-ABCD1234";

    @Mock
    private ExpedientePrimerContactoRepository expedientes;
    @Mock
    private QuejaReferenciaRepository quejas;

    private ExpedientePrimerContacto expediente;

    @BeforeEach
    void setUp() {
        expediente = new ExpedientePrimerContacto();
        expediente.setFolio("PC-1");
        expediente.setFolioOrigen(FOLIO_QUEJA);
    }

    @Test
    void cambiarEstatus_sincronizando_actualizaTambienLaQueja() {
        TransicionExpedienteService servicio = new TransicionExpedienteService(expedientes, quejas, true);
        QuejaReferencia queja = new QuejaReferencia();
        when(expedientes.save(expediente)).thenReturn(expediente);
        when(quejas.findByNumeroFolio(FOLIO_QUEJA)).thenReturn(Optional.of(queja));

        servicio.cambiarEstatus(expediente, EstatusExpediente.PROCEDENTE);

        assertEquals(EstatusExpediente.PROCEDENTE, queja.getEstatus(), "La queja ve el mismo estatus");
        assertNotNull(expediente.getFechaActualizacion(), "Se registra la fecha");
        verify(quejas).save(queja);
    }

    @Test
    void cambiarEstatus_sinSincronizar_noTocaLaQueja() {
        TransicionExpedienteService servicio = new TransicionExpedienteService(expedientes, quejas, false);
        when(expedientes.save(expediente)).thenReturn(expediente);
        servicio.cambiarEstatus(expediente, EstatusExpediente.IMPROCEDENTE);
        verifyNoInteractions(quejas);
    }

    @Test
    void cambiarEstatus_quejaNoExiste_noFalla() {
        TransicionExpedienteService servicio = new TransicionExpedienteService(expedientes, quejas, true);
        when(expedientes.save(expediente)).thenReturn(expediente);
        when(quejas.findByNumeroFolio(FOLIO_QUEJA)).thenReturn(Optional.empty());
        servicio.cambiarEstatus(expediente, EstatusExpediente.PROCEDENTE);
        verify(quejas, never()).save(any());
    }

    @Test
    void cambiarEstatus_errorAlSincronizar_noRevierteElExpediente() {
        TransicionExpedienteService servicio = new TransicionExpedienteService(expedientes, quejas, true);
        when(expedientes.save(expediente)).thenReturn(expediente);
        when(quejas.findByNumeroFolio(FOLIO_QUEJA)).thenThrow(new RuntimeException("BD de quejas caída"));
        assertDoesNotThrow(() -> servicio.cambiarEstatus(expediente, EstatusExpediente.PROCEDENTE),
                "El expediente avanza aunque falle la otra tabla");
        assertEquals(EstatusExpediente.PROCEDENTE, expediente.getEstatus(), "Estatus aplicado");
    }

    @Test
    void obtenerPorFolio_inexistente_lanzaExcepcion() {
        TransicionExpedienteService servicio = new TransicionExpedienteService(expedientes, quejas, true);
        when(expedientes.findByFolio("PC-X")).thenReturn(Optional.empty());
        assertThrows(RecursoNoEncontradoException.class, () -> servicio.obtenerPorFolio("PC-X"), "Folio inexistente");
    }
}
