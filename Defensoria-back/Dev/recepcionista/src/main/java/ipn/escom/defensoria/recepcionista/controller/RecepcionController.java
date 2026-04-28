package ipn.escom.defensoria.recepcionista.controller;

import ipn.escom.defensoria.recepcionista.entity.Queja;
import ipn.escom.defensoria.recepcionista.service.GestionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

@RestController
@RequestMapping("/api/recepcion")
@RequiredArgsConstructor
public class RecepcionController {

    private final GestionService gestionService;

    // CU25 y CU28
    @GetMapping("/panel")
    @PreAuthorize("hasRole('RECEPCIONISTA')")
    public ResponseEntity<List<Queja>> verPanel() {
        return ResponseEntity.ok(gestionService.obtenerPendientes());
    }

    @GetMapping("/historial")
    @PreAuthorize("hasRole('RECEPCIONISTA')")
    public ResponseEntity<List<Queja>> verHistorial() {
        return ResponseEntity.ok(gestionService.obtenerTodas());
    }

    // CU27
    @GetMapping("/queja/{id}")
    @PreAuthorize("hasRole('RECEPCIONISTA')")
    public ResponseEntity<Queja> abrirQueja(@PathVariable Long id) {
        return ResponseEntity.ok(gestionService.obtenerPorId(id));
    }

    // CU33: Buscar antecedencia
    @GetMapping("/buscar-antecedencia")
    @PreAuthorize("hasRole('RECEPCIONISTA')")
    public ResponseEntity<List<Queja>> buscarAntecedencia(
            @RequestParam(required = false) String correo,
            @RequestParam(required = false) String boleta) {
        return ResponseEntity.ok(gestionService.buscarAntecedentes(correo, boleta));
    }

    // CU30 y CU31
    @PatchMapping("/validar/{id}")
    @PreAuthorize("hasRole('RECEPCIONISTA')")
    public ResponseEntity<Queja> validar(@PathVariable Long id) {
        return ResponseEntity.ok(gestionService.validarQueja(id));
    }

    @PatchMapping("/rechazar/{id}")
    @PreAuthorize("hasRole('RECEPCIONISTA')")
    public ResponseEntity<Queja> rechazar(@PathVariable Long id, @RequestBody String motivo) {
        return ResponseEntity.ok(gestionService.rechazarQueja(id, motivo));
    }

    // CU34 y CU35
    @PatchMapping("/turnar/{id}")
    @PreAuthorize("hasRole('RECEPCIONISTA')")
    public ResponseEntity<Queja> turnar(@PathVariable Long id, @RequestParam String abogado) {
        return ResponseEntity.ok(gestionService.turnarAAbogado(id, abogado));
    }
}