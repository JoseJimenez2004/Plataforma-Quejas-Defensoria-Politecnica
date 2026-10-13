package ipn.escom.defensoria.admin_service.controller;

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

    private final PersonalAdministrativoService personalService;
    private final JwtUtil jwtUtil;
    private final BitacoraService bitacoraService;

    public AuthAdminController(PersonalAdministrativoService personalService, JwtUtil jwtUtil, BitacoraService bitacoraService) {
        this.personalService = personalService;
        this.jwtUtil = jwtUtil;
        this.bitacoraService = bitacoraService;
    }

    @PostMapping("/login")
    @Operation(summary = "Login de personal administrativo (cualquier rol)")
    public ResponseEntity<AuthAdminResponseModel> login(@RequestBody LoginAdminModel model,
            HttpServletRequest request) {
        PersonalAdministrativo personal;
        try {
            personal = personalService.validarLogin(model.getCorreo(), model.getPassword());
        } catch (RuntimeException ex) {
            // CU-ADM-09: los intentos fallidos también se auditan (fuerza bruta, cuentas desactivadas).
            String correo = model.getCorreo() == null ? "(sin correo)" : model.getCorreo().trim();
            if (correo.length() > 120) {
                correo = correo.substring(0, 120);
            }
            bitacoraService.registrar(correo, "Intento de inicio de sesión fallido: " + ex.getMessage(), request);
            throw ex;
        }
        String token = jwtUtil.generarToken(personal.getCorreoInstitucional(), personal.getRol().name());

        bitacoraService.registrar(personal.getCorreoInstitucional(), "Inicio de sesión", request);

        return ResponseEntity.ok(new AuthAdminResponseModel(
                token, personal.getNombreCompleto(), personal.getRol().name(),
                personal.isForzarCambioPassword()));
    }
}
