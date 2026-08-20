package ipn.escom.defensoria.catalogo_service.controller;

import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import ipn.escom.defensoria.catalogo_service.entity.Dependencia;
import ipn.escom.defensoria.catalogo_service.service.DependenciaService;

/**
 * Endpoints públicos de solo lectura del catálogo de dependencias del IPN. Son públicos
 * (ver WebConfig) porque el formulario de "Presentar una queja" los necesita incluso antes
 * de que el quejoso tenga una cuenta/token.
 */
@RestController
@RequestMapping("/api/catalogos/dependencias")
@Tag(name = "Dependencias", description = "Catálogo de dependencias del IPN (secretarías, direcciones, unidades académicas, etc.)")
public class DependenciaController {

    private final DependenciaService dependenciaService;

    public DependenciaController(DependenciaService dependenciaService) {
        this.dependenciaService = dependenciaService;
    }

    @GetMapping
    @Operation(summary = "Lista todas las dependencias activas, opcionalmente filtradas por tipo")
    public ResponseEntity<List<Dependencia>> listar(@RequestParam(required = false) String tipo) {
        List<Dependencia> resultado = (tipo == null || tipo.isBlank())
                ? dependenciaService.listarActivas()
                : dependenciaService.listarActivasPorTipo(tipo);
        return ResponseEntity.ok(resultado);
    }

    @GetMapping("/{clave}")
    @Operation(summary = "Obtiene una dependencia por su clave (ej. ESCOM, CECYT9, SA.1)")
    public ResponseEntity<Dependencia> obtenerPorClave(@PathVariable String clave) {
        return dependenciaService.buscarPorClave(clave)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
