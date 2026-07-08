package ipn.escom.defensoria.notificaciones_service.service;

import ipn.escom.defensoria.notificaciones_service.model.EmailRequestModel;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    public void enviarCorreoSimple(EmailRequestModel request) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom("jair100flo@gmail.com");
        message.setTo(request.getDestinatario());
        message.setSubject(request.getAsunto());
        message.setText(request.getCuerpo());
        
        mailSender.send(message);
    }
}