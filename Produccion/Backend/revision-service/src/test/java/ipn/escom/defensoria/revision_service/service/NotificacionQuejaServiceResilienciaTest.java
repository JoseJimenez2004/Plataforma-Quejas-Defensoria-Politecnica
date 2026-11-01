package ipn.escom.defensoria.revision_service.service;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

/**
 * RNF-R04 y RNF-Q04 (Resiliencia / Fiabilidad): si falla el envío del correo de rechazo, el
 * rechazo en sí debe completarse igualmente. Como el rechazo ya se guardó antes de llamar a
 * notificaciones-service, basta con garantizar que ninguna llamada de notificación propaga
 * la excepción hacia RevisionQuejaService.
 */
@Tag("RNF-R04")
@ExtendWith(MockitoExtension.class)
class NotificacionQuejaServiceResilienciaTest {

    private static final String FOLIO = "FOL-12345678";

    @Mock
    private RestTemplate restTemplate;

    private NotificacionQuejaService servicio;

    @BeforeEach
    void setUp() {
        servicio = new NotificacionQuejaService(restTemplate);
        ReflectionTestUtils.setField(servicio, "notificacionesServiceUrl", "http://notificaciones-caido:8085");
        when(restTemplate.postForObject(anyString(), any(), eq(String.class)))
                .thenThrow(new ResourceAccessException("Connection refused"));
    }

    @Test
    @DisplayName("RNF-R04: correo de rechazo con notificaciones caído no tumba el rechazo")
    void enviarCorreoRechazo_servicioCaido_noPropagaError() {
        assertThatCode(() -> servicio.enviarCorreoRechazo("q@alumno.ipn.mx", "Quejoso", FOLIO, "Falta evidencia"))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("RNF-Q04: aviso de cambio de estatus con notificaciones caído")
    void registrarCambioEstatus_servicioCaido_noPropagaError() {
        assertThatCode(() -> servicio.registrarCambioEstatus("q@alumno.ipn.mx", FOLIO, "Turnada", "Mensaje"))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("RNF-Q04: aviso de acuerdo de conciliación con notificaciones caído")
    void registrarConciliacion_servicioCaido_noPropagaError() {
        assertThatCode(() -> servicio.registrarConciliacion("q@alumno.ipn.mx", FOLIO, "Asunto"))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("RNF-Q04: aviso al personal con enlace nulo y notificaciones caído")
    void notificarPersonal_enlaceNuloYServicioCaido_noPropagaError() {
        assertThatCode(() -> servicio.notificarPersonal("recep@ipn.mx", "Título", "Mensaje", null))
                .doesNotThrowAnyException();
    }
}
