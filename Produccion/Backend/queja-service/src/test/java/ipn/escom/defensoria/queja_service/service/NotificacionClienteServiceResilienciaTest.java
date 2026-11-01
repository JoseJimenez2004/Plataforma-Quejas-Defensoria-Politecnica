package ipn.escom.defensoria.queja_service.service;

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
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

/**
 * RNF-Q04 (Resiliencia / Fiabilidad): un fallo en el envío de correo o en el registro de una
 * notificación nunca debe impedir que la operación principal se complete; el fallo se degrada
 * a un registro en la bitácora del servidor.
 *
 * <p>Se simula notificaciones-service caído (sin conexión) y respondiendo 500.
 */
@Tag("RNF-Q04")
@ExtendWith(MockitoExtension.class)
class NotificacionClienteServiceResilienciaTest {

    private static final String CORREO = "quejoso@alumno.ipn.mx";
    private static final String FOLIO = "FOL-12345678";

    @Mock
    private RestTemplate restTemplate;

    private NotificacionClienteService servicio;

    @BeforeEach
    void setUp() {
        servicio = new NotificacionClienteService(restTemplate, "http://notificaciones-caido:8085");
    }

    private void notificacionesSinConexion() {
        when(restTemplate.postForObject(anyString(), any(), eq(String.class)))
                .thenThrow(new ResourceAccessException("Connection refused"));
    }

    private void notificacionesConError500() {
        when(restTemplate.postForObject(anyString(), any(), eq(String.class)))
                .thenThrow(new HttpServerErrorException(HttpStatus.INTERNAL_SERVER_ERROR));
    }

    @Test
    @DisplayName("Queja creada: notificaciones sin conexión no propaga la excepción")
    void notificarQuejaCreada_servicioCaido_noPropagaError() {
        notificacionesSinConexion();
        assertThatCode(() -> servicio.notificarQuejaCreada(CORREO, FOLIO)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Aviso al tutor de un menor: error 500 no propaga la excepción")
    void notificarTutorQuejaCreada_error500_noPropagaError() {
        notificacionesConError500();
        assertThatCode(() -> servicio.notificarTutorQuejaCreada("tutor@gmail.com", "Tutor", "Menor", FOLIO))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Cambio de estatus: notificaciones sin conexión no propaga la excepción")
    void notificarCambioEstatus_servicioCaido_noPropagaError() {
        notificacionesSinConexion();
        assertThatCode(() -> servicio.notificarCambioEstatus(CORREO, FOLIO, "Título", "Mensaje"))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Aviso al personal: notificaciones sin conexión no propaga la excepción")
    void notificarPersonal_servicioCaido_noPropagaError() {
        notificacionesSinConexion();
        assertThatCode(() -> servicio.notificarPersonal("recep@ipn.mx", "Título", "Mensaje"))
                .doesNotThrowAnyException();
    }
}
