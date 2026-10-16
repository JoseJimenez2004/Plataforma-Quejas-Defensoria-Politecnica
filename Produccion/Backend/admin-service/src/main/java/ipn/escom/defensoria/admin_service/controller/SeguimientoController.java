package ipn.escom.defensoria.admin_service.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import ipn.escom.defensoria.admin_service.model.SeguimientoModels.DetalleQueja;
import ipn.escom.defensoria.admin_service.model.SeguimientoModels.Filtros;
import ipn.escom.defensoria.admin_service.model.SeguimientoModels.PaginaQuejas;
import ipn.escom.defensoria.admin_service.model.SeguimientoModels.Tablero;
import ipn.escom.defensoria.admin_service.model.SeguimientoModels.UnidadConteo;
import ipn.escom.defensoria.admin_service.service.SeguimientoQuejasService;

/** Seguimiento de quejas para la Defensora: tablero, explorador, unidades y detalle (solo lectura). */
@RestController
@RequestMapping("/api/admin/seguimiento")
@PreAuthorize("hasAnyRole('ADMIN_SISTEMAS','DEFENSOR')")
@Tag(name = "Seguimiento de quejas", description = "Indicadores y consulta de quejas para la Defensora")
public class SeguimientoController {

    private final SeguimientoQuejasService servicio;

    public SeguimientoController(SeguimientoQuejasService servicio) {
        this.servicio = servicio;
    }

    @GetMapping("/tablero")
    @Operation(summary = "Indicadores, etapas, meses, unidades, Recepción, abogados y motivos de rechazo")
    public ResponseEntity<Tablero> tablero() {
        return ResponseEntity.ok(servicio.tablero());
    }

    @GetMapping("/filtros")
    @Operation(summary = "Opciones para los filtros del explorador (recepcionistas, abogados, etapas)")
    public ResponseEntity<Filtros> filtros() {
        return ResponseEntity.ok(servicio.filtros());
    }

    @GetMapping("/quejas")
    @Operation(summary = "Quejas con filtros por etapa, estatus, unidad, abogado, recepcionista, mes y texto")
    public ResponseEntity<PaginaQuejas> quejas(
            @RequestParam(required = false) String etapa,
            @RequestParam(required = false) String estatus,
            @RequestParam(required = false) String unidad,
            @RequestParam(required = false) String abogado,
            @RequestParam(required = false) String recepcionista,
            @RequestParam(required = false) String mes,
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "15") int tamanio) {
        return ResponseEntity.ok(servicio.quejas(etapa, estatus, unidad, abogado, recepcionista, mes, q, pagina, tamanio));
    }

    @GetMapping("/quejas/{folio}")
    @Operation(summary = "Detalle de solo lectura de una queja")
    public ResponseEntity<DetalleQueja> detalle(@PathVariable String folio) {
        return ResponseEntity.ok(servicio.detalle(folio));
    }

    @GetMapping("/quejas/{folio}/evidencias/{id}")
    @Operation(summary = "Abre un archivo de la queja (credencial o evidencia)")
    public ResponseEntity<byte[]> evidencia(@PathVariable String folio, @PathVariable Long id) {
        Map<String, Object> archivo = servicio.evidencia(folio, id);
        String tipo = archivo.get("tipo_mime") == null ? "application/octet-stream" : archivo.get("tipo_mime").toString();
        String nombre = String.valueOf(archivo.get("nombre_archivo")).replace("\"", "");
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(tipo))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + nombre + "\"")
                .body((byte[]) archivo.get("contenido"));
    }

    @GetMapping("/unidades")
    @Operation(summary = "Dependencias y unidades académicas con quejas, de la que más tiene a la que menos")
    public ResponseEntity<List<UnidadConteo>> unidades(@RequestParam(required = false) String q) {
        return ResponseEntity.ok(servicio.unidades(q, 0));
    }
}
