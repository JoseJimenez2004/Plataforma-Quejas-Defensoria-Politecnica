package ipn.escom.defensoria.revision_service.service;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

/** Le pide a notificaciones-service (POST /api/notificaciones/enviar) que mande el correo
 * automático de rechazo al quejoso -- mismo patrón de llamada simple entre microservicios que
 * ya usa admin-service para consultar catalogo-service. */
@Service
public class NotificacionQuejaService {

    private static final Logger log = LoggerFactory.getLogger(NotificacionQuejaService.class);

    @Autowired
    private RestTemplate restTemplate;

    @Value("${notificaciones.service.url}")
    private String notificacionesServiceUrl;

    public void enviarCorreoRechazo(String destinatario, String nombreQuejoso, String folio, String motivoTexto) {
        String asunto = "Defensoría de los Derechos Politécnicos — Observaciones a tu queja " + folio;
        String cuerpo = "Estimado(a) " + nombreQuejoso + ",\n\n"
                + "Tu queja con folio " + folio + " fue revisada y, por el momento, no pudo turnarse "
                + "porque se detectaron las siguientes observaciones:\n\n"
                + motivoTexto + "\n\n"
                + "Por favor ingresa a la plataforma con tu folio y correo para revisar el detalle y, "
                + "si corresponde, atender lo señalado.\n\n"
                + "Defensoría de los Derechos Politécnicos.";

        try {
            restTemplate.postForObject(
                    notificacionesServiceUrl + "/api/notificaciones/enviar",
                    Map.of("destinatario", destinatario, "asunto", asunto, "cuerpo", cuerpo),
                    String.class);
        } catch (Exception ex) {
            // El rechazo YA se guardó en la base -- si el correo falla no debe revertirse la
            // decisión del recepcionista, solo queda registrado en el log para reintentar/avisar.
            log.error("No se pudo enviar el correo de rechazo para el folio {}: {}", folio, ex.getMessage());
        }
    }
}
