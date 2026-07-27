package ipn.escom.defensoria.admin_service.controller;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import ipn.escom.defensoria.admin_service.model.CambiarPasswordRequest;
import ipn.escom.defensoria.admin_service.service.BitacoraService;
import ipn.escom.defensoria.admin_service.service.PersonalAdministrativoService;
import jakarta.servlet.http.HttpServletRequest;

/**
 * A diferencia de PersonalController (exclusivo ADMIN_SISTEMAS, opera sobre OTRAS cuentas),
 * este endpoint NO lleva @PreAuthorize de rol -- cualquier cuenta autenticada (recepcionista,
 * analista, subdefensor, defensor o admin de sistemas) puede usarlo para cambiar SU PROPIA
 * contraseña. WebConfig ya exige un JWT válido para todo lo que no sea /api/admin/auth/**.
 */
@RestController
@RequestMapping("/api/admin/perfil")
@Tag(name = "Perfil", description = "Autogestión de la propia cuenta de personal administrativo")
public class PerfilController {

    @Autowired
    private PersonalAdministrativoService personalService;

    @Autowired
    private BitacoraService bitacoraService;

    @PutMapping("/password")
    @Operation(summary = "Cambia la contraseña de la cuenta actualmente autenticada")
    public ResponseEntity<Map<String, String>> cambiarPassword(
            @RequestBody CambiarPasswordRequest datos,
            Authentication authentication,
            HttpServletRequest request) {
        String correo = authentication.getName();
        personalService.cambiarMiPassword(correo, datos.getPasswordActual(), datos.getPasswordNueva());
        bitacoraService.registrar(correo, "Cambio de contraseña propia", request);
        return ResponseEntity.ok(Map.of("mensaje", "Contraseña actualizada correctamente."));
    }
}
