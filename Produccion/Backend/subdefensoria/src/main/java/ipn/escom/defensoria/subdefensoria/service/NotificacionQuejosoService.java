package ipn.escom.defensoria.subdefensoria.service;

import ipn.escom.defensoria.subdefensoria.entity.ExpedienteInvestigacion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

/**
 * Avisa al quejoso de lo que pasa con su queja en Subdefensoría, con el mismo patrón que
 * NotificacionQuejosoService de Primer Contacto:
 *
 *   POST /api/notificaciones/registrar -> aviso en el centro de notificaciones de su panel;
 *   POST /api/notificaciones/enviar    -> correo electrónico.
 *
 * Ambas llamadas son best-effort: si notificaciones-service no responde, la operación del
 * subdefensor YA quedó guardada y no se revierte; solo se deja en el log.
 */
@Service
public class NotificacionQuejosoService {

    private static final Logger log = LoggerFactory.getLogger(NotificacionQuejosoService.class);

    private static final String ENDPOINT_REGISTRAR = "/api/notificaciones/registrar";
    private static final String ENDPOINT_ENVIAR = "/api/notificaciones/enviar";

    public static final String TIPO_CAMBIO_ESTATUS = "CAMBIO_ESTATUS";
    public static final String TIPO_CONCILIACION = "CONCILIACION";

    private static final String ENLACE_MIS_QUEJAS = "/panel/mis-quejas/";
    private static final String FIRMA = "\n\nDefensoría de los Derechos Politécnicos.";

    private final RestTemplate restTemplate;
    private final String notificacionesUrl;

    public NotificacionQuejosoService(
            RestTemplate restTemplate,
            @Value("${notificaciones.service.url:http://localhost:8085}") String notificacionesUrl
    ) {
        this.restTemplate = restTemplate;
        this.notificacionesUrl = notificacionesUrl;
    }

    /**
     * Deja el aviso en el panel del quejoso y le manda el mismo texto por correo. El aviso
     * abre {@code enlace} (ruta del portal) o, si es null, su queja.
     */
    public void notificar(
            ExpedienteInvestigacion expediente,
            String tipo,
            String titulo,
            String mensaje,
            String enlace
    ) {
        String correo = expediente.getQuejosoCorreo();
        if (correo == null || correo.isBlank()) {
            log.warn("El expediente {} no tiene correo del quejoso; no se notificó: {}",
                    expediente.getFolio(), titulo);
            return;
        }

        String folioQueja = expediente.getFolioQueja() != null ? expediente.getFolioQueja() : expediente.getFolio();

        registrar(correo, tipo, titulo, mensaje, enlace != null ? enlace : ENLACE_MIS_QUEJAS + folioQueja);
        enviarCorreo(
                correo,
                "Defensoría de los Derechos Politécnicos — " + titulo + " (" + folioQueja + ")",
                saludo(expediente) + mensaje + FIRMA
        );
    }

    private void registrar(String correo, String tipo, String titulo, String mensaje, String enlace) {
        try {
            restTemplate.postForObject(
                    notificacionesUrl + ENDPOINT_REGISTRAR,
                    Map.of(
                            "correoDestino", correo,
                            "tipo", tipo,
                            "titulo", titulo,
                            "mensaje", mensaje,
                            "enlace", enlace
                    ),
                    String.class
            );
        } catch (Exception ex) {
            log.error("No se pudo registrar la notificación ({}) para {}: {}", tipo, correo, ex.getMessage());
        }
    }

    private void enviarCorreo(String destinatario, String asunto, String cuerpo) {
        try {
            restTemplate.postForObject(
                    notificacionesUrl + ENDPOINT_ENVIAR,
                    Map.of(
                            "destinatario", destinatario,
                            "asunto", asunto,
                            "cuerpo", cuerpo
                    ),
                    String.class
            );
        } catch (Exception ex) {
            log.error("No se pudo enviar el correo \"{}\" a {}: {}", asunto, destinatario, ex.getMessage());
        }
    }

    private String saludo(ExpedienteInvestigacion expediente) {
        String nombre = expediente.getQuejosoNombre();
        return "Estimado(a) " + (nombre == null || nombre.isBlank() ? "quejoso(a)" : nombre) + ",\n\n";
    }
}
