package ipn.escom.defensoria.queja_service.controller;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import ipn.escom.defensoria.queja_service.entity.Queja;
import ipn.escom.defensoria.queja_service.service.QuejaService;

@RestController
@RequestMapping("/api/quejoso/quejas")
public class QuejaController {

    @Autowired
    private QuejaService quejaService;

    @PostMapping("/validar-folio")
    public boolean validarFolioYCorreo(@RequestBody Map<String, String> datos) {
        String folio = datos.get("folio");
        String correo = datos.get("correo");
        return quejaService.validarFolioYCorreo(folio, correo);
    }

    // Endpoint protegido para que un usuario autenticado registre una queja nueva
    @PostMapping("/registrar")
    public ResponseEntity<Queja> registrarQueja(
            @RequestParam String motivo,
            @RequestParam String descripcion,
            @RequestParam(required = false) MultipartFile archivo) {
        
        // Obtenemos el correo del usuario logueado directamente desde el JWT verificado
        String correoUsuario = SecurityContextHolder.getContext().getAuthentication().getName();
        
        Queja nuevaQueja = quejaService.registrarQueja(motivo, descripcion, correoUsuario, archivo);
        return ResponseEntity.ok(nuevaQueja);
    }
}