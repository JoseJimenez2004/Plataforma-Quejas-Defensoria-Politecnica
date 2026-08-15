package ipn.escom.defensoria.auth.service.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import ipn.escom.defensoria.auth.service.model.PerfilModel;
import ipn.escom.defensoria.auth.service.model.PerfilUpdateRequest;
import ipn.escom.defensoria.auth.service.service.UsuarioService;

/**
 * Perfil del quejoso autenticado -- "Configuración de Perfil" del panel. Requiere JWT (ver
 * WebConfig); el correo sale del token verificado, no de un parámetro.
 */
@RestController
@RequestMapping("/api/auth")
@Tag(name = "Perfil", description = "Datos de contacto y generales del quejoso autenticado")
public class PerfilController {

    @Autowired
    private UsuarioService usuarioService;

    @GetMapping("/me")
    @Operation(summary = "Obtiene el perfil completo del usuario autenticado")
    public ResponseEntity<PerfilModel> obtenerMiPerfil() {
        String correo = correoActual();
        return ResponseEntity.ok(usuarioService.obtenerPerfil(correo));
    }

    @PutMapping("/perfil")
    @Operation(summary = "Actualiza correo personal, teléfono, unidad académica y/o domicilio")
    public ResponseEntity<PerfilModel> actualizarPerfil(@RequestBody PerfilUpdateRequest datos) {
        String correo = correoActual();
        return ResponseEntity.ok(usuarioService.actualizarPerfil(correo, datos));
    }

    private String correoActual() {
        Authentication autenticacion = SecurityContextHolder.getContext().getAuthentication();
        return autenticacion.getName();
    }
}
