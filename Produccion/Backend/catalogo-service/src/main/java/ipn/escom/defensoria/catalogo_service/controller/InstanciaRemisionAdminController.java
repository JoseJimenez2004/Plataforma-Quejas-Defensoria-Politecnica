package ipn.escom.defensoria.catalogo_service.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import ipn.escom.defensoria.catalogo_service.entity.InstanciaRemision;
import ipn.escom.defensoria.catalogo_service.service.InstanciaRemisionService;

/**
 * Panel de la Defensora: correos de las instancias a las que Primer Contacto remite
 * (OAG, OIC, COSECOVI / Género). Solo DEFENSOR; no se crean ni se borran instancias.
 */
@RestController
@RequestMapping("/api/catalogos/instancias-remision/admin")
@PreAuthorize("hasRole('DEFENSOR')")
public class InstanciaRemisionAdminController {

    private final InstanciaRemisionService service;

    public InstanciaRemisionAdminController(InstanciaRemisionService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<List<InstanciaRemision>> listar() {
        return ResponseEntity.ok(service.listar());
    }

    /** PUT /api/catalogos/instancias-remision/admin/OAG  { "correo": "..." } */
    @PutMapping("/{clave}")
    public ResponseEntity<InstanciaRemision> cambiarCorreo(
            @PathVariable String clave,
            @RequestBody Map<String, String> cuerpo,
            Authentication authentication
    ) {
        String quien = authentication != null ? authentication.getName() : null;
        return ResponseEntity.ok(service.cambiarCorreo(clave, cuerpo == null ? null : cuerpo.get("correo"), quien));
    }
}
