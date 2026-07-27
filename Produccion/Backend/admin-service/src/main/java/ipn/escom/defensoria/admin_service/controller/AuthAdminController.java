package ipn.escom.defensoria.admin_service.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import ipn.escom.defensoria.admin_service.config.JwtUtil;
import ipn.escom.defensoria.admin_service.entity.PersonalAdministrativo;
import ipn.escom.defensoria.admin_service.model.AuthAdminResponseModel;
import ipn.escom.defensoria.admin_service.model.LoginAdminModel;
import ipn.escom.defensoria.admin_service.service.BitacoraService;
import ipn.escom.defensoria.admin_service.service.PersonalAdministrativoService;
import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/admin/auth")
@Tag(name = "Auth Admin", description = "Login del personal administrativo")
public class AuthAdminController {

    @Autowired
    private PersonalAdministrativoService personalService;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private BitacoraService bitacoraService;

    @PostMapping("/login")
    @Operation(summary = "Login de personal administrativo (cualquier rol)")
    public ResponseEntity<AuthAdminResponseModel> login(@RequestBody LoginAdminModel model,
            HttpServletRequest request) {
        PersonalAdministrativo personal = personalService.validarLogin(model.getCorreo(), model.getPassword());
        String token = jwtUtil.generarToken(personal.getCorreoInstitucional(), personal.getRol().name());

        bitacoraService.registrar(personal.getCorreoInstitucional(), "Inicio de sesión", request);

        return ResponseEntity.ok(new AuthAdminResponseModel(
                token, personal.getNombreCompleto(), personal.getRol().name(),
                personal.isForzarCambioPassword()));
    }
}
