package ipn.escom.defensoria.admin_service.controller;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import ipn.escom.defensoria.admin_service.entity.BitacoraAccion;
import ipn.escom.defensoria.admin_service.model.RespaldoResumenModel;
import ipn.escom.defensoria.admin_service.service.BitacoraService;
import ipn.escom.defensoria.admin_service.service.RespaldoService;
import jakarta.servlet.http.HttpServletRequest;

/** "Seguridad y Respaldos" del mockup -- exclusivo de ADMIN_SISTEMAS. La restauración es una
 * operación destructiva: se exige mandar {"confirmar": true} en el cuerpo a propósito, para
 * que no sea un solo clic accidental. */
@RestController
@RequestMapping("/api/admin/seguridad")
@PreAuthorize("hasRole('ADMIN_SISTEMAS')")
@Tag(name = "Seguridad y Respaldos", description = "Respaldos manuales/automáticos y bitácora de acciones")
public class SeguridadController {

    @Autowired
    private RespaldoService respaldoService;

    @Autowired
    private BitacoraService bitacoraService;

    @GetMapping("/respaldos")
    @Operation(summary = "Lista los respaldos disponibles, más reciente primero")
    public ResponseEntity<List<RespaldoResumenModel>> listarRespaldos() {
        return ResponseEntity.ok(respaldoService.listar());
    }

    @PostMapping("/respaldos/manual")
    @Operation(summary = "Ejecuta un respaldo manual de la base de datos ahora mismo")
    public ResponseEntity<RespaldoResumenModel> respaldoManual(HttpServletRequest request) {
        RespaldoResumenModel resultado = respaldoService.ejecutarRespaldoManual();
        bitacoraService.registrar(usuarioActual(), "Respaldo manual ejecutado: " + resultado.getNombreArchivo(), request);
        return ResponseEntity.ok(resultado);
    }

    @GetMapping("/respaldos/{nombreArchivo}/descargar")
    @Operation(summary = "Descarga un archivo de respaldo específico")
    public ResponseEntity<Resource> descargar(@PathVariable String nombreArchivo) {
        Resource recurso = respaldoService.obtenerArchivo(nombreArchivo);
        return ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=\"" + nombreArchivo + "\"")
                .body(recurso);
    }

    @PostMapping("/restaurar")
    @Operation(summary = "Restaura la base de datos desde un respaldo (requiere confirmar=true, DESTRUCTIVO)")
    public ResponseEntity<Void> restaurar(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        Object confirmar = body.get("confirmar");
        Object archivo = body.get("nombreArchivo");
        if (!Boolean.TRUE.equals(confirmar) || archivo == null) {
            throw new RuntimeException("Debes confirmar explícitamente la restauración e indicar el archivo.");
        }
        respaldoService.restaurar(archivo.toString());
        bitacoraService.registrar(usuarioActual(), "RESTAURACIÓN de base de datos desde: " + archivo, request);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/bitacora")
    @Operation(summary = "Lista las últimas 50 acciones críticas registradas")
    public ResponseEntity<List<BitacoraAccion>> bitacora() {
        return ResponseEntity.ok(bitacoraService.listarRecientes());
    }

    private String usuarioActual() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }
}
