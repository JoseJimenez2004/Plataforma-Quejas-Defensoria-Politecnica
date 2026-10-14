package ipn.escom.defensoria.subdefensoria.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

/**
 * Manda a la unidad académica el oficio o el recordatorio (con su PDF adjunto) por correo,
 * usando POST /api/notificaciones/enviar de notificaciones-service.
 *
 * Mientras se valida el flujo, {@code correo.unidad-academica.redirigir-a} manda TODO a un
 * correo de prueba (el asunto y el cuerpo dicen a quién iba dirigido en realidad). Con
 * {@code correo.unidad-academica.habilitado=false} no se envía nada.
 *
 * Es best-effort, como las notificaciones al quejoso: si el correo falla, el oficio ya quedó
 * guardado y solo se deja en el log; el resultado indica si salió para marcar correoEnviado.
 */
@Service
public class CorreoUnidadAcademicaService {

    private static final Logger log = LoggerFactory.getLogger(CorreoUnidadAcademicaService.class);
    private static final String ENDPOINT_ENVIAR = "/api/notificaciones/enviar";
    private static final String FIRMA = "\n\nDefensoría de los Derechos Politécnicos.";

    private final RestTemplate restTemplate;
    private final String notificacionesUrl;
    private final boolean habilitado;
    private final String redirigirA;

    public CorreoUnidadAcademicaService(
            RestTemplate restTemplate,
            @Value("${notificaciones.service.url:http://localhost:8085}") String notificacionesUrl,
            @Value("${correo.unidad-academica.habilitado:true}") boolean habilitado,
            @Value("${correo.unidad-academica.redirigir-a:}") String redirigirA
    ) {
        this.restTemplate = restTemplate;
        this.notificacionesUrl = notificacionesUrl;
        this.habilitado = habilitado;
        this.redirigirA = redirigirA == null ? "" : redirigirA.trim();
    }

    /** @return true si notificaciones-service aceptó el correo. */
    public boolean enviar(String destinatario, String asunto, String cuerpo, DocumentoService.Pdf pdf) {
        if (!habilitado) {
            log.info("Correo a la unidad académica deshabilitado; no se envió: {}", asunto);
            return false;
        }
        if (destinatario == null || destinatario.isBlank()) {
            log.warn("El oficio no tiene correo de destinatario; no se envió: {}", asunto);
            return false;
        }

        String para = destinatario.trim();
        String asuntoFinal = asunto;
        String cuerpoFinal = cuerpo;
        if (!redirigirA.isEmpty()) {
            asuntoFinal = "[PRUEBA] " + asunto;
            cuerpoFinal = "(Correo de prueba: iba dirigido a " + para + ")\n\n" + cuerpo;
            para = redirigirA;
        }

        Map<String, String> body = new HashMap<>();
        body.put("destinatario", para);
        body.put("asunto", asuntoFinal);
        body.put("cuerpo", cuerpoFinal + FIRMA);
        if (pdf != null) {
            body.put("adjuntoNombre", pdf.nombreArchivo());
            body.put("adjuntoTipo", "application/pdf");
            body.put("adjuntoBase64", Base64.getEncoder().encodeToString(pdf.contenido()));
        }

        try {
            restTemplate.postForObject(notificacionesUrl + ENDPOINT_ENVIAR, body, String.class);
            log.info("Correo \"{}\" enviado a {}", asuntoFinal, para);
            return true;
        } catch (Exception ex) {
            log.error("No se pudo enviar el correo \"{}\" a {}: {}", asuntoFinal, para, ex.getMessage());
            return false;
        }
    }
}
