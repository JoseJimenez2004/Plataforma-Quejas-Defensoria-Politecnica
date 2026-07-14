package ipn.escom.defensoria.queja_service.controller;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.format.annotation.DateTimeFormat.ISO;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import ipn.escom.defensoria.queja_service.dto.RegistroQuejaPublicaRequest;
import ipn.escom.defensoria.queja_service.entity.Queja;
import ipn.escom.defensoria.queja_service.service.QuejaService;

@RestController
@RequestMapping("/api/quejoso/quejas")
@Tag(name = "Quejas", description = "Registro y consulta de quejas")
public class QuejaController {

    @Autowired
    private QuejaService quejaService;

    @PostMapping("/validar-folio")
    @Operation(summary = "Valida que un folio + correo correspondan a una queja real (público, lo usa auth-service)")
    public boolean validarFolioYCorreo(@RequestBody Map<String, String> datos) {
        String folio = datos.get("folio");
        String correo = datos.get("correo");
        return quejaService.validarFolioYCorreo(folio, correo);
    }

    // Endpoint protegido para que un usuario autenticado registre una queja nueva.
    // Acepta 0 o más archivos de evidencia — se guardan completos como BYTEA en la tabla
    // queja_evidencias, ver QuejaEvidencia. Los datos de "unidad académica"/"fecha de
    // hechos"/"denunciado" ahora son columnas propias, ya no se concatenan en la descripción.
    @PostMapping("/registrar")
    @Operation(summary = "Registra una queja nueva con 0 o más archivos de evidencia (requiere JWT)")
    public ResponseEntity<Queja> registrarQueja(
            @RequestParam String motivo,
            @RequestParam String descripcion,
            @RequestParam(required = false) String unidadAcademicaClave,
            @RequestParam(required = false) @DateTimeFormat(iso = ISO.DATE) LocalDate fechaHechos,
            @RequestParam(required = false) String nombreDenunciado,
            @RequestParam(required = false) String apellidoDenunciado,
            @RequestParam(required = false) List<MultipartFile> archivos) {

        // Obtenemos el correo del usuario logueado directamente desde el JWT verificado
        String correoUsuario = SecurityContextHolder.getContext().getAuthentication().getName();

        Queja nuevaQueja = quejaService.registrarQueja(
                motivo, descripcion, correoUsuario,
                unidadAcademicaClave, fechaHechos, nombreDenunciado, apellidoDenunciado,
                archivos);
        return ResponseEntity.ok(nuevaQueja);
    }

    // Endpoint protegido: lista todas las quejas del usuario logueado (panel "Mis Quejas" /
    // "Resumen"). El correo sale del JWT verificado, igual que en /registrar.
    @GetMapping("/mias")
    @Operation(summary = "Lista las quejas del usuario autenticado (requiere JWT)")
    public ResponseEntity<List<Queja>> listarMisQuejas() {
        String correoUsuario = SecurityContextHolder.getContext().getAuthentication().getName();
        return ResponseEntity.ok(quejaService.listarMisQuejas(correoUsuario));
    }

    // Endpoint protegido: detalle de una queja propia (panel autenticado, vista "Ver/Editar").
    @GetMapping("/mias/{folio}")
    @Operation(summary = "Obtiene el detalle de una queja propia del usuario autenticado (requiere JWT)")
    public ResponseEntity<Queja> obtenerMiQueja(@PathVariable String folio) {
        String correoUsuario = SecurityContextHolder.getContext().getAuthentication().getName();
        return ResponseEntity.ok(quejaService.obtenerMiQueja(folio, correoUsuario));
    }

    // Endpoint protegido: evidencias (solo metadatos, sin el contenido) de una queja propia.
    @GetMapping("/mias/{folio}/evidencias")
    @Operation(summary = "Lista las evidencias (sin contenido) de una queja propia (requiere JWT)")
    public ResponseEntity<List<ipn.escom.defensoria.queja_service.dto.EvidenciaResumen>> listarEvidencias(
            @PathVariable String folio) {
        String correoUsuario = SecurityContextHolder.getContext().getAuthentication().getName();
        return ResponseEntity.ok(quejaService.listarEvidencias(folio, correoUsuario));
    }

    // Endpoint público: detalle de una queja por folio + correo (misma llave que validar-folio,
    // pero regresando los datos en vez de solo true/false). Lo usa "Consultar queja" en el
    // frontend y auth-service (vía Feign) para poblar nombre/boleta reales al activar cuenta.
    @GetMapping("/folio/{folio}")
    @Operation(summary = "Obtiene el detalle de una queja por folio + correo (público, ambos datos son la llave de acceso)")
    public ResponseEntity<Queja> obtenerPorFolio(
            @PathVariable String folio,
            @RequestParam String correo) {
        Queja queja = quejaService.obtenerPorFolioYCorreo(folio, correo);
        return ResponseEntity.ok(queja);
    }

    // Endpoint público: permite registrar una queja sin sesión iniciada (gente sin cuenta
    // institucional). Guarda la identidad completa del quejoso, ya que aquí no hay JWT del
    // que derivar el correo. Ver WebConfig para el permitAll de esta ruta.
    @PostMapping("/registro-publico")
    @Operation(summary = "Registra una queja de forma pública, sin necesidad de sesión iniciada")
    public ResponseEntity<Queja> registrarQuejaPublica(@ModelAttribute RegistroQuejaPublicaRequest datos) {
        Queja nuevaQueja = quejaService.registrarQuejaPublica(datos);
        return ResponseEntity.ok(nuevaQueja);
    }
}