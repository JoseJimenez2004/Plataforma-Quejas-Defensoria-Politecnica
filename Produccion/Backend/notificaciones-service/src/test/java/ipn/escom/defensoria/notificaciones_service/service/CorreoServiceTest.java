package ipn.escom.defensoria.notificaciones_service.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Base64;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;

import ipn.escom.defensoria.notificaciones_service.model.EnviarCorreoRequest;
import jakarta.mail.Multipart;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;

/** Pruebas unitarias de {@link CorreoService}, incluida la defensa contra inyección de cabeceras (CWE-93). */
@ExtendWith(MockitoExtension.class)
@DisplayName("CorreoService — envío de correos")
class CorreoServiceTest {

    private static final String REMITENTE = "defensoria@ipn.mx";
    private static final String DESTINO = "quejoso@ipn.mx";

    @Mock
    private JavaMailSender mailSender;

    private CorreoService servicio;

    @BeforeEach
    void setUp() {
        servicio = new CorreoService(mailSender, REMITENTE);
        lenient().when(mailSender.createMimeMessage()).thenAnswer(inv -> new MimeMessage((Session) null));
    }

    private static EnviarCorreoRequest correo(String destinatario, String asunto, String cuerpo) {
        EnviarCorreoRequest d = new EnviarCorreoRequest();
        d.setDestinatario(destinatario);
        d.setAsunto(asunto);
        d.setCuerpo(cuerpo);
        return d;
    }

    private MimeMessage enviado() {
        ArgumentCaptor<MimeMessage> mensaje = ArgumentCaptor.forClass(MimeMessage.class);
        verify(mailSender).send(mensaje.capture());
        return mensaje.getValue();
    }

    @Test
    void enviar_correoValido_loEnviaConRemitenteYAsunto() throws Exception {
        servicio.enviar(correo("  " + DESTINO + " ", "Observaciones a tu queja", "Cuerpo"));

        MimeMessage m = enviado();
        assertEquals(REMITENTE, m.getFrom()[0].toString(), "Remitente configurado");
        assertEquals(DESTINO, m.getAllRecipients()[0].toString(), "Destinatario sin espacios");
        assertEquals("Observaciones a tu queja", m.getSubject(), "Asunto");
    }

    @Test
    void enviar_asuntoConSaltosDeLinea_seNeutralizan() throws Exception {
        servicio.enviar(correo(DESTINO, "Hola\r\nBcc: atacante@mal.com", "Cuerpo"));
        String asunto = enviado().getSubject();
        assertFalse(asunto.contains("\n") || asunto.contains("\r"), "No debe quedar ningún salto de línea");
        assertEquals("Hola Bcc: atacante@mal.com", asunto, "El intento de cabecera queda como texto plano");
    }

    @Test
    void limpiarCabecera_caracteresDeControl_losReemplazaPorEspacio() {
        assertEquals("a b", CorreoService.limpiarCabecera("a\u0000\u0007b "), "Caracteres de control fuera");
    }

    @ParameterizedTest
    @ValueSource(strings = {"sin-arroba", "a@b", "a b@c.com", "a@@b.com"})
    void enviar_destinatarioInvalido_lanzaExcepcion(String destinatario) {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> servicio.enviar(correo(destinatario, "Asunto", "Cuerpo")));
        assertEquals("El destinatario no es un correo válido.", ex.getMessage(), "Rechaza " + destinatario);
    }

    @Test
    void enviar_sinCuerpo_lanzaExcepcion() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> servicio.enviar(correo(DESTINO, "Asunto", " ")));
        assertEquals("Faltan destinatario, asunto o cuerpo del correo.", ex.getMessage(), "Cuerpo obligatorio");
    }

    @Test
    void enviar_nulo_lanzaExcepcion() {
        assertThrows(IllegalArgumentException.class, () -> servicio.enviar(null), "Petición nula");
    }

    @Test
    void enviar_registroManualInterno_noSeEnvia() {
        servicio.enviar(correo("registro-manual+fol-abcd1234@defensoria.ipn.mx", "Asunto", "Cuerpo"));
        verify(mailSender, never()).send(any(MimeMessage.class));
    }

    @Test
    void enviar_conAdjuntoBase64_armaMensajeMultiparte() throws Exception {
        EnviarCorreoRequest d = correo(DESTINO, "Oficio", "Se adjunta el oficio");
        d.setAdjuntoBase64(Base64.getEncoder().encodeToString("%PDF-1.7".getBytes()));

        servicio.enviar(d);

        MimeMessage m = enviado();
        m.saveChanges();
        assertTrue(m.getContent() instanceof Multipart, "Con adjunto el mensaje es multiparte");
        assertEquals("adjunto.pdf", ((Multipart) m.getContent()).getBodyPart(1).getFileName(), "Nombre por defecto");
    }

    @Test
    void enviar_adjuntoQueNoEsBase64_lanzaExcepcion() {
        EnviarCorreoRequest d = correo(DESTINO, "Oficio", "Cuerpo");
        d.setAdjuntoBase64("esto no es base64!!");
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> servicio.enviar(d));
        assertEquals("El adjunto no está en base64 válido.", ex.getMessage(), "Base64 inválido");
        verify(mailSender, never()).send(any(MimeMessage.class));
    }
}
