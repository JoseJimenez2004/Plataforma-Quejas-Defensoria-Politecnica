package ipn.escom.defensoria.notificaciones_service.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import ipn.escom.defensoria.notificaciones_service.model.EnviarCorreoRequest;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.SneakyThrows;

import java.util.Base64;

/**
 * Envío de correo saliente (SMTP configurado en spring.mail de notificaciones-service.yml).
 *
 * Este endpoint existía y en algún momento se perdió: revision-service y queja-service lo
 * seguían llamando, recibían un error, lo registraban en su log y continuaban -- así que el
 * correo de rechazo al quejoso nunca salía y nadie lo notaba (hallazgo del 2026-10-07).
 *
 * Kiuwan: enviar() se partió en métodos chicos (complejidad ciclomática) sin cambiar el orden
 * de las validaciones ni los mensajes, y el asunto se limpia de saltos de línea antes de usarse
 * como cabecera (Mail Command Injection, CWE-93).
 */
@Service
public class CorreoService {

    private static final Logger log = LoggerFactory.getLogger(CorreoService.class);
    private static final String PATRON_CORREO = "^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$";
    /** CR, LF y demás caracteres de control: con ellos se podrían inyectar cabeceras SMTP. */
    private static final String CARACTERES_CONTROL = "[\\r\\n\\x00-\\x1F\\x7F]+";

    private final JavaMailSender mailSender;
    private final String remitente;

    public CorreoService(JavaMailSender mailSender, @Value("${spring.mail.username}") String remitente) {
        this.mailSender = mailSender;
        this.remitente = remitente;
    }

    public void enviar(EnviarCorreoRequest datos) {
        validarDatos(datos);
        String destinatario = validarDestinatario(datos.getDestinatario());
        // Los registros manuales sin correo usan una dirección interna de referencia; no tiene
        // caso intentar entregarla.
        if (esRegistroManual(destinatario)) {
            log.info("Correo omitido: {} es una dirección interna de registro manual", destinatario);
            return;
        }
        byte[] adjunto = decodificarAdjunto(datos);
        String asunto = limpiarCabecera(datos.getAsunto());
        armarYEnviar(datos, destinatario, asunto, adjunto);
        log.info("Correo enviado a {} ({}){}", destinatario, asunto, adjunto != null ? " con adjunto" : "");
    }

    private static void validarDatos(EnviarCorreoRequest datos) {
        if (datos == null || esVacio(datos.getDestinatario()) || esVacio(datos.getAsunto())
                || esVacio(datos.getCuerpo())) {
            throw new IllegalArgumentException("Faltan destinatario, asunto o cuerpo del correo.");
        }
    }

    private static String validarDestinatario(String valor) {
        String destinatario = valor.trim();
        if (!destinatario.matches(PATRON_CORREO)) {
            throw new IllegalArgumentException("El destinatario no es un correo válido.");
        }
        return destinatario;
    }

    private static boolean esRegistroManual(String destinatario) {
        String minusculas = destinatario.toLowerCase();
        return minusculas.endsWith("@defensoria.ipn.mx") && minusculas.startsWith("registro-manual+");
    }

    /** Devuelve null si el correo no trae adjunto. */
    private static byte[] decodificarAdjunto(EnviarCorreoRequest datos) {
        if (esVacio(datos.getAdjuntoBase64())) {
            return null;
        }
        try {
            return Base64.getDecoder().decode(datos.getAdjuntoBase64());
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("El adjunto no está en base64 válido.");
        }
    }

    /** Neutraliza saltos de línea y caracteres de control para que no se puedan inyectar cabeceras. */
    static String limpiarCabecera(String valor) {
        return valor.replaceAll(CARACTERES_CONTROL, " ").trim();
    }

    private void armarYEnviar(EnviarCorreoRequest datos, String destinatario, String asunto, byte[] adjunto) {
        boolean conAdjunto = adjunto != null;
        try {
            MimeMessage mensaje = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mensaje, conAdjunto, "UTF-8");
            helper.setFrom(remitente);
            helper.setTo(destinatario);
            helper.setSubject(asunto);
            helper.setText(datos.getCuerpo());
            if (conAdjunto) {
                agregarAdjunto(helper, datos, adjunto);
            }
            mailSender.send(mensaje);
        } catch (MessagingException ex) {
            throw new IllegalArgumentException("No se pudo armar el correo: " + ex.getMessage());
        }
    }

    @SneakyThrows
    private static void agregarAdjunto(MimeMessageHelper helper, EnviarCorreoRequest datos, byte[] adjunto) {
        String nombre = esVacio(datos.getAdjuntoNombre()) ? "adjunto.pdf" : limpiarCabecera(datos.getAdjuntoNombre());
        String tipo = esVacio(datos.getAdjuntoTipo()) ? "application/pdf" : limpiarCabecera(datos.getAdjuntoTipo());
        helper.addAttachment(nombre, new ByteArrayResource(adjunto), tipo);
    }

    private static boolean esVacio(String valor) {
        return valor == null || valor.isBlank();
    }
}
