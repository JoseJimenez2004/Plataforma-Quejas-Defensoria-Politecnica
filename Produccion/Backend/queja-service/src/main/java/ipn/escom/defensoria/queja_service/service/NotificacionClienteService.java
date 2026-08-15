package ipn.escom.defensoria.queja_service.service;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

/** Deja un aviso persistido en el centro de notificaciones del quejoso cuando se registra una
 * queja nueva (POST /api/notificaciones/registrar) -- mismo patrón de llamada simple entre
 * microservicios que ya usa revision-service para el correo de rechazo. Nunca debe tumbar el
 * registro de la queja si falla. */
@Service
public class NotificacionClienteService {

    private static final Logger log = LoggerFactory.getLogger(NotificacionClienteService.class);

    @Autowired
    private RestTemplate restTemplate;

    @Value("${notificaciones.service.url}")
    private String notificacionesServiceUrl;

    public void notificarQuejaCreada(String correoDestino, String folio) {
        try {
            restTemplate.postForObject(
                    notificacionesServiceUrl + "/api/notificaciones/registrar",
                    Map.of(
                            "correoDestino", correoDestino,
                            "tipo", "QUEJA_CREADA",
                            "titulo", "Queja registrada",
                            "mensaje", "Tu queja con folio " + folio + " fue registrada correctamente. "
                                    + "Te avisaremos aquí cuando cambie de estatus.",
                            "enlace", "/panel/mis-quejas/" + folio),
                    String.class);
        } catch (Exception ex) {
            log.error("No se pudo registrar la notificación de queja creada para el folio {}: {}",
                    folio, ex.getMessage());
        }
    }

    /** Correo real (no solo aviso en la app) al padre/madre/tutor cuando se presenta una queja
     * a nombre de un menor de edad -- usa POST /api/notificaciones/enviar (mismo endpoint que ya
     * usa revision-service para el correo de rechazo), no /registrar. Nunca debe tumbar el
     * registro de la queja si falla o si el tutor no dejó correo. */
    public void notificarTutorQuejaCreada(String correoTutor, String nombreTutor, String nombreMenor, String folio) {
        if (correoTutor == null || correoTutor.isBlank()) {
            return;
        }
        try {
            String asunto = "Defensoría de los Derechos Politécnicos — Queja registrada a nombre de " + nombreMenor;
            String cuerpo = "Estimado(a) " + nombreTutor + ",\n\n"
                    + "Te informamos que se registró una queja ante la Defensoría de los Derechos "
                    + "Politécnicos a nombre de " + nombreMenor + ", de quien apareces como tutor(a) "
                    + "o adulto(a) responsable.\n\n"
                    + "El folio de seguimiento es: " + folio + "\n\n"
                    + "Puedes dar seguimiento al trámite ingresando el folio y el correo del quejoso "
                    + "en la sección \"Consultar folio\" de la plataforma.\n\n"
                    + "Defensoría de los Derechos Politécnicos.";
            restTemplate.postForObject(
                    notificacionesServiceUrl + "/api/notificaciones/enviar",
                    Map.of("destinatario", correoTutor, "asunto", asunto, "cuerpo", cuerpo),
                    String.class);
        } catch (Exception ex) {
            log.error("No se pudo enviar el correo al tutor para el folio {}: {}", folio, ex.getMessage());
        }
    }
}
