package ipn.escom.defensoria.notificaciones_service.controller;

import ipn.escom.defensoria.notificaciones_service.model.EmailRequestModel;
import ipn.escom.defensoria.notificaciones_service.service.EmailService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/notificaciones")
public class EmailController {

    @Autowired
    private EmailService emailService;

    @PostMapping("/enviar")
    public ResponseEntity<String> enviarEmail(@RequestBody EmailRequestModel model) {
        emailService.enviarCorreoSimple(model);
        return ResponseEntity.ok("Correo enviado exitosamente.");
    }
}