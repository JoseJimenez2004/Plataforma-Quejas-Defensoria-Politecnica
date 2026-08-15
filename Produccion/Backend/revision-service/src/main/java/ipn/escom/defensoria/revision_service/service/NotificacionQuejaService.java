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

    /** Deja un aviso persistido en el centro de notificaciones del quejoso (POST /registrar,
     * NO manda correo) cada vez que cambia el estatus de su queja -- lo que pidió el usuario
     * ("cambios de estatus de mi quejas"). Nunca debe tumbar la operación si falla. */
    public void registrarCambioEstatus(String correoDestino, String folio, String tituloEvento, String mensaje) {
        registrarNotificacion(correoDestino, "CAMBIO_ESTATUS", tituloEvento, mensaje, "/panel/mis-quejas/" + folio);
    }

    /** Igual que arriba, pero para cuando se emite un nuevo acuerdo de conciliación. */
    public void registrarConciliacion(String correoDestino, String folio, String asunto) {
        registrarNotificacion(correoDestino, "CONCILIACION",
                "Nuevo acuerdo de conciliación",
                "Se te propuso un acuerdo de conciliación relacionado con tu queja " + folio + ": " + asunto,
                "/panel/conciliacion");
    }

    private void registrarNotificacion(String correoDestino, String tipo, String titulo, String mensaje, String enlace) {
        try {
            restTemplate.postForObject(
                    notificacionesServiceUrl + "/api/notificaciones/registrar",
                    Map.of(
                            "correoDestino", correoDestino,
                            "tipo", tipo,
                            "titulo", titulo,
                            "mensaje", mensaje,
                            "enlace", enlace),
                    String.class);
        } catch (Exception ex) {
            log.error("No se pudo registrar la notificación ({}) para {}: {}", tipo, correoDestino, ex.getMessage());
        }
    }
}
