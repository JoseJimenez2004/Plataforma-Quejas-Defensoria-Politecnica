package ipn.escom.defensoria.queja_service.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import ipn.escom.defensoria.queja_service.dto.RespuestaConciliacionRequest;
import ipn.escom.defensoria.queja_service.entity.AcuerdoConciliacion;
import ipn.escom.defensoria.queja_service.service.ConciliacionService;

/**
 * Acuerdos de conciliación del quejoso autenticado -- todo requiere JWT (igual que el resto
 * de /api/quejoso/**, ver WebConfig: cualquier ruta no listada explícitamente como pública
 * cae en anyRequest().authenticated()).
 */
@RestController
@RequestMapping("/api/quejoso/conciliaciones")
@Tag(name = "Conciliación", description = "Acuerdos de conciliación propuestos al quejoso")
public class ConciliacionController {

    private final ConciliacionService conciliacionService;

    public ConciliacionController(ConciliacionService conciliacionService) {
        this.conciliacionService = conciliacionService;
    }

    @GetMapping("/mias")
    @Operation(summary = "Lista los acuerdos de conciliación dirigidos al usuario autenticado")
    public ResponseEntity<List<AcuerdoConciliacion>> listarMisAcuerdos() {
        String correo = SecurityContextHolder.getContext().getAuthentication().getName();
        return ResponseEntity.ok(conciliacionService.listarMisAcuerdos(correo));
    }

    @PutMapping("/{id}/respuesta")
    @Operation(summary = "Acepta o rechaza un acuerdo de conciliación propio")
    public ResponseEntity<AcuerdoConciliacion> responder(
            @PathVariable Long id, @RequestBody RespuestaConciliacionRequest datos) {
        String correo = SecurityContextHolder.getContext().getAuthentication().getName();
        return ResponseEntity.ok(conciliacionService.responder(id, correo, datos));
    }
}
