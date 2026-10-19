package ipn.escom.defensoria.admin_service.controller;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import ipn.escom.defensoria.admin_service.model.PanelModels.Carga;
import ipn.escom.defensoria.admin_service.model.PanelModels.ContactoRequest;
import ipn.escom.defensoria.admin_service.model.PanelModels.DetallePersonal;
import ipn.escom.defensoria.admin_service.model.PanelModels.MiembroPersonal;
import ipn.escom.defensoria.admin_service.model.PanelModels.MotivoRechazo;
import ipn.escom.defensoria.admin_service.model.PanelModels.PaginaCasos;
import ipn.escom.defensoria.admin_service.model.PanelModels.Panel;
import ipn.escom.defensoria.admin_service.model.PanelModels.TablaUnidades;
import ipn.escom.defensoria.admin_service.model.SeguimientoModels.DetalleQueja;
import ipn.escom.defensoria.admin_service.model.SeguimientoModels.Filtros;
import ipn.escom.defensoria.admin_service.model.SeguimientoModels.PaginaQuejas;
import ipn.escom.defensoria.admin_service.model.SeguimientoModels.UnidadConteo;
import ipn.escom.defensoria.admin_service.service.BitacoraService;
import ipn.escom.defensoria.admin_service.service.PanelDefensoraService;
import ipn.escom.defensoria.admin_service.service.SeguimientoQuejasService;
import jakarta.servlet.http.HttpServletRequest;

/**
 * Panel de la Defensora: tablero, casos que requieren atención, explorador de quejas, quejas
 * por unidad, carga de trabajo, motivos de rechazo y personal. Todo de solo lectura salvo el
 * área y teléfono del personal.
 */
@RestController
@RequestMapping("/api/admin/seguimiento")
@PreAuthorize("hasAnyRole('ADMIN_SISTEMAS','DEFENSOR')")
@Tag(name = "Seguimiento de quejas", description = "Indicadores y consulta de quejas para la Defensora")
public class SeguimientoController {

    private final SeguimientoQuejasService servicio;
    private final PanelDefensoraService panel;
    private final BitacoraService bitacora;

    public SeguimientoController(SeguimientoQuejasService servicio, PanelDefensoraService panel, BitacoraService bitacora) {
        this.servicio = servicio;
        this.panel = panel;
        this.bitacora = bitacora;
    }

    @GetMapping("/tablero")
    @Operation(summary = "Panel general: indicadores del periodo, etapas, evolución, unidades, casos, carga y motivos")
    public ResponseEntity<Panel> tablero(@RequestParam(defaultValue = "30") int dias,
                                         @RequestParam(defaultValue = "12") int meses) {
        return ResponseEntity.ok(panel.panel(dias, meses));
    }

    @GetMapping("/atencion")
    @Operation(summary = "Casos que requieren atención (retrasos, sin asignar, observaciones pendientes)")
    public ResponseEntity<PaginaCasos> atencion(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String motivo,
            @RequestParam(required = false) String etapa,
            @RequestParam(required = false) String unidad,
            @RequestParam(required = false) String responsable,
            @RequestParam(required = false) Integer diasMin,
            @RequestParam(required = false) Integer diasMax,
            @RequestParam(required = false) String severidad,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "10") int tamanio) {
        return ResponseEntity.ok(panel.casos(q, motivo, etapa, unidad, responsable, diasMin, diasMax, severidad, pagina, tamanio));
    }

    @GetMapping("/filtros")
    @Operation(summary = "Opciones para los filtros del explorador (recepcionistas, abogados, etapas, estados)")
    public ResponseEntity<Filtros> filtros() {
        return ResponseEntity.ok(servicio.filtros());
    }

    @GetMapping("/quejas")
    @Operation(summary = "Quejas con filtros por etapa, estado, estatus, unidad, abogado, recepcionista, meses, prioridad, motivo y texto")
    public ResponseEntity<PaginaQuejas> quejas(
            @RequestParam(required = false) String etapa,
            @RequestParam(required = false) String estatus,
            @RequestParam(required = false) String estado,
            @RequestParam(required = false) String unidad,
            @RequestParam(required = false) String abogado,
            @RequestParam(required = false) String recepcionista,
            @RequestParam(required = false) String mes,
            @RequestParam(required = false) String desde,
            @RequestParam(required = false) String hasta,
            @RequestParam(required = false) String prioridad,
            @RequestParam(required = false) String motivo,
            @RequestParam(required = false) String orden,
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "10") int tamanio) {
        Map<String, String> f = new LinkedHashMap<>();
        f.put("etapa", etapa);
        f.put("estatus", estatus);
        f.put("estado", estado);
        f.put("unidad", unidad);
        f.put("abogado", abogado);
        f.put("recepcionista", recepcionista);
        f.put("mes", mes);
        f.put("desde", desde);
        f.put("hasta", hasta);
        f.put("prioridad", prioridad);
        f.put("motivo", motivo);
        f.put("orden", orden == null ? "reciente" : orden);
        f.put("q", q);
        return ResponseEntity.ok(servicio.quejas(f, pagina, tamanio));
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
    @Operation(summary = "Dependencias y unidades con quejas (lista simple para los selectores)")
    public ResponseEntity<List<UnidadConteo>> unidades(@RequestParam(required = false) String q) {
        return ResponseEntity.ok(servicio.unidades(q, 0));
    }

    @GetMapping("/unidades/tabla")
    @Operation(summary = "Quejas por unidad: resumen, top 10, distribución por tipo y tabla paginada con filtros")
    public ResponseEntity<TablaUnidades> tablaUnidades(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String tipo,
            @RequestParam(required = false) String estatus,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(defaultValue = "365") int dias,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "10") int tamanio) {
        return ResponseEntity.ok(panel.unidades(q, tipo, estatus, desde, hasta, dias, pagina, tamanio));
    }

    @GetMapping("/carga")
    @Operation(summary = "Carga de trabajo de recepcionistas y abogados en el periodo")
    public ResponseEntity<Carga> carga(@RequestParam(defaultValue = "30") int dias) {
        return ResponseEntity.ok(panel.carga(dias));
    }

    @GetMapping("/motivos-rechazo")
    @Operation(summary = "Motivos por los que Recepción regresa las quejas, con su categoría")
    public ResponseEntity<List<MotivoRechazo>> motivos(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String categoria,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(defaultValue = "0") int dias) {
        return ResponseEntity.ok(panel.motivos(q, categoria, desde, hasta, dias));
    }

    @GetMapping("/personal")
    @Operation(summary = "Personal de la Defensoría con área, teléfono, último acceso y estadísticas de atención")
    public ResponseEntity<List<MiembroPersonal>> personal() {
        return ResponseEntity.ok(panel.personal());
    }

    @GetMapping("/personal/{id}")
    @Operation(summary = "Detalle de una persona: actividad reciente y quejas que tiene o tuvo a su cargo")
    public ResponseEntity<DetallePersonal> detallePersonal(@PathVariable Long id) {
        return ResponseEntity.ok(panel.detallePersonal(id));
    }

    @PutMapping("/personal/{id}/contacto")
    @Operation(summary = "Guarda el área de adscripción (clave del catálogo) y el teléfono de una persona")
    public ResponseEntity<MiembroPersonal> guardarContacto(@PathVariable Long id, @RequestBody ContactoRequest datos,
                                                           HttpServletRequest request) {
        MiembroPersonal m = panel.guardarContacto(id, datos);
        bitacora.registrar(SecurityContextHolder.getContext().getAuthentication().getName(),
                "Actualización de área/teléfono de " + m.correo(), request);
        return ResponseEntity.ok(m);
    }
}
