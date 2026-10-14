package ipn.escom.defensoria.notificaciones_service.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import ipn.escom.defensoria.notificaciones_service.model.EnviarCorreoRequest;

/**
 * Envío de correo saliente (SMTP configurado en spring.mail de notificaciones-service.yml).
 *
 * Este endpoint existía y en algún momento se perdió: revision-service y queja-service lo
 * seguían llamando, recibían un error, lo registraban en su log y continuaban -- así que el
 * correo de rechazo al quejoso nunca salía y nadie lo notaba (hallazgo del 2026-10-07).
 */
@Service
public class CorreoService {

    private static final Logger log = LoggerFactory.getLogger(CorreoService.class);
    private static final String PATRON_CORREO = "^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$";

    private final JavaMailSender mailSender;
    private final String remitente;

    public CorreoService(JavaMailSender mailSender, @Value("${spring.mail.username}") String remitente) {
        this.mailSender = mailSender;
        this.remitente = remitente;
    }

    public void enviar(EnviarCorreoRequest datos) {
        if (datos == null || esVacio(datos.getDestinatario()) || esVacio(datos.getAsunto())
                || esVacio(datos.getCuerpo())) {
            throw new IllegalArgumentException("Faltan destinatario, asunto o cuerpo del correo.");
        }
        String destinatario = datos.getDestinatario().trim();
        if (!destinatario.matches(PATRON_CORREO)) {
            throw new IllegalArgumentException("El destinatario no es un correo válido.");
        }
        // Los registros manuales sin correo usan una dirección interna de referencia; no tiene
        // caso intentar entregarla.
        if (destinatario.toLowerCase().endsWith("@defensoria.ipn.mx")
                && destinatario.toLowerCase().startsWith("registro-manual+")) {
            log.info("Correo omitido: {} es una dirección interna de registro manual", destinatario);
            return;
        }

        SimpleMailMessage mensaje = new SimpleMailMessage();
        mensaje.setFrom(remitente);
        mensaje.setTo(destinatario);
        mensaje.setSubject(datos.getAsunto());
        mensaje.setText(datos.getCuerpo());
        mailSender.send(mensaje);
        log.info("Correo enviado a {} ({})", destinatario, datos.getAsunto());
    }

    private static boolean esVacio(String valor) {
        return valor == null || valor.isBlank();
    }
}
