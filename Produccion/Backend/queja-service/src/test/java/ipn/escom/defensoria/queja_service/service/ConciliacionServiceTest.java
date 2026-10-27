package ipn.escom.defensoria.queja_service.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import ipn.escom.defensoria.queja_service.dto.RespuestaConciliacionRequest;
import ipn.escom.defensoria.queja_service.entity.AcuerdoConciliacion;
import ipn.escom.defensoria.queja_service.repository.AcuerdoConciliacionRepository;

/** CU-Q08 — el quejoso responde un acuerdo de conciliación. */
@ExtendWith(MockitoExtension.class)
@DisplayName("ConciliacionService — respuesta del quejoso")
class ConciliacionServiceTest {

    private static final Long ID = 7L;
    private static final String CORREO = "quejoso@ipn.mx";

    @Mock
    private AcuerdoConciliacionRepository repositorio;

    @InjectMocks
    private ConciliacionService servicio;

    private AcuerdoConciliacion acuerdo;

    @BeforeEach
    void setUp() {
        acuerdo = new AcuerdoConciliacion();
        acuerdo.setCorreoInstitucional(CORREO);
        acuerdo.setEstado("PENDIENTE");
    }

    private RespuestaConciliacionRequest respuesta(String estado) {
        RespuestaConciliacionRequest r = new RespuestaConciliacionRequest();
        r.setEstado(estado);
        r.setComentario("De acuerdo");
        return r;
    }

    @Test
    void responder_aceptadoEnMinusculas_guardaEnMayusculasConFecha() {
        when(repositorio.findById(ID)).thenReturn(Optional.of(acuerdo));
        when(repositorio.save(any())).thenAnswer(inv -> inv.getArgument(0));

        AcuerdoConciliacion r = servicio.responder(ID, "QUEJOSO@ipn.mx", respuesta("aceptado"));

        assertEquals("ACEPTADO", r.getEstado(), "El estado se guarda en mayúsculas");
        assertEquals("De acuerdo", r.getComentarioQuejoso(), "Se guarda el comentario");
        assertNotNull(r.getFechaRespuesta(), "Se registra la fecha de respuesta");
    }

    @Test
    void responder_acuerdoInexistente_lanzaExcepcion() {
        when(repositorio.findById(ID)).thenReturn(Optional.empty());
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> servicio.responder(ID, CORREO, respuesta("ACEPTADO")));
        assertEquals("No se encontró el acuerdo de conciliación indicado.", ex.getMessage(), "Acuerdo inexistente");
    }

    @Test
    void responder_acuerdoDeOtroQuejoso_lanzaExcepcionYNoGuarda() {
        when(repositorio.findById(ID)).thenReturn(Optional.of(acuerdo));
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> servicio.responder(ID, "otro@ipn.mx", respuesta("ACEPTADO")));
        assertEquals("Ese acuerdo no corresponde a tu cuenta.", ex.getMessage(), "Acuerdo ajeno");
        verify(repositorio, never()).save(any());
    }

    @Test
    void responder_acuerdoYaRespondido_lanzaExcepcion() {
        acuerdo.setEstado("RECHAZADO");
        when(repositorio.findById(ID)).thenReturn(Optional.of(acuerdo));
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> servicio.responder(ID, CORREO, respuesta("ACEPTADO")));
        assertEquals("Este acuerdo ya fue respondido anteriormente.", ex.getMessage(), "La decisión es definitiva");
    }

    @Test
    void responder_estadoNoValido_lanzaExcepcion() {
        when(repositorio.findById(ID)).thenReturn(Optional.of(acuerdo));
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> servicio.responder(ID, CORREO, respuesta("TAL VEZ")));
        assertEquals("La respuesta debe ser ACEPTADO o RECHAZADO.", ex.getMessage(), "Solo dos respuestas válidas");
    }
}
