package ipn.escom.defensoria.recepcionista.controller;

import ipn.escom.defensoria.recepcionista.dto.*;
import ipn.escom.defensoria.recepcionista.service.IQuejaService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controlador REST principal del módulo Recepcionista.
 *
 * Expone los endpoints para gestionar el ciclo de vida de las quejas
 * desde la perspectiva de la recepcionista de la DDP.
 *
 * Ruta base: /api/recepcionista/quejas
 *
 * @CrossOrigin("*") permite que el frontend (Angular en localhost:4200)
 * pueda consumir estos endpoints sin bloqueo por CORS.
 * Cuando el proyecto suba a producción, cambiar "*" por la URL real del frontend.
 *
 * Cada método delega la lógica al servicio (IQuejaService) y solo se encarga
 * de recibir la petición HTTP y devolver la respuesta con el código correcto.
 */
@RestController
@RequestMapping("/api/recepcionista/quejas")
@CrossOrigin(origins = "*")
public class QuejaController {

    // Spring inyecta automáticamente la implementación (QuejaServiceImpl)
    @Autowired
    private IQuejaService quejaService;

    /**
     * Dashboard de Recepción.
     * Devuelve la lista de quejas con estatus "RECIBIDA" que aún no han sido
     * revisadas por la recepcionista. Alimenta la tabla principal del panel.
     *
     * GET /api/recepcionista/quejas
     * Respuesta: 200 OK + List<QuejaResumenDTO>
     */
    @GetMapping
    public ResponseEntity<List<QuejaResumenDTO>> listarPendientes() {
        return ResponseEntity.ok(quejaService.listarQuejasPendientes());
    }

    /**
     * Pantalla de Validación de Requisitos.
     * Devuelve el detalle completo de una queja específica: datos del quejoso,
     * descripción de los hechos, tema, tipo de documento, archivos adjuntos
     * y si la documentación está completa o incompleta.
     *
     * GET /api/recepcionista/quejas/{id}
     * Respuesta: 200 OK + QuejaDetalleDTO
     *            400 BAD REQUEST si el id no existe
     */
    @GetMapping("/{id}")
    public ResponseEntity<QuejaDetalleDTO> obtenerDetalle(@PathVariable Long id) {
        return ResponseEntity.ok(quejaService.obtenerDetalle(id));
    }

    /**
     * Acción "Aceptar Queja" en la pantalla de Validación.
     * Cambia el estatus de RECIBIDA → EN_REVISION y registra el evento
     * en la tabla historial_estatus_queja automáticamente.
     * La queja deja de aparecer en el dashboard.
     *
     * PUT /api/recepcionista/quejas/{id}/aceptar
     * Respuesta: 200 OK + QuejaResumenDTO con estatus actualizado
     */
    @PutMapping("/{id}/aceptar")
    public ResponseEntity<QuejaResumenDTO> aceptar(@PathVariable Long id) {
        return ResponseEntity.ok(quejaService.aceptarQueja(id));
    }

    /**
     * Acción "Rechazar Queja" en la pantalla de Validación.
     * Cambia el estatus a ATENDIDA y guarda el motivo de rechazo en el historial.
     * El body debe incluir el motivo — @Valid valida que no venga vacío.
     *
     * PUT /api/recepcionista/quejas/{id}/rechazar
     * Body: { "motivoRechazo": "texto obligatorio" }
     * Respuesta: 200 OK + QuejaResumenDTO con estatus actualizado
     *            400 BAD REQUEST si motivoRechazo está vacío
     */
    @PutMapping("/{id}/rechazar")
    public ResponseEntity<QuejaResumenDTO> rechazar(
            @PathVariable Long id,
            @Valid @RequestBody RechazarQuejaRequest request) {
        return ResponseEntity.ok(quejaService.rechazarQueja(id, request));
    }

    /**
     * Pantalla de Notificación de Rechazo.
     * Registra en el historial que la recepcionista notificó al quejoso
     * con el motivo de rechazo. En el futuro, este endpoint disparará
     * el envío de un correo electrónico al quejoso.
     *
     * POST /api/recepcionista/quejas/{id}/notificar-rechazo
     * Body: { "motivoRechazo": "texto obligatorio" }
     * Respuesta: 200 OK + mensaje de confirmación (String)
     */
    @PostMapping("/{id}/notificar-rechazo")
    public ResponseEntity<String> notificarRechazo(
            @PathVariable Long id,
            @Valid @RequestBody RechazarQuejaRequest request) {
        return ResponseEntity.ok(quejaService.notificarRechazo(id, request));
    }

    /**
     * Pantalla de Búsqueda de Antecedentes — acción "Confirmar Canalización".
     * Registra la opción de canalización elegida (ej. "Turno a abogado")
     * con notas adicionales en el historial y mueve la queja a EN_REVISION.
     *
     * POST /api/recepcionista/quejas/{id}/canalizar
     * Body: { "opcionCanalizacion": "texto obligatorio", "notasAdicionales": "opcional" }
     * Respuesta: 200 OK + QuejaResumenDTO con estatus actualizado
     */
    @PostMapping("/{id}/canalizar")
    public ResponseEntity<QuejaResumenDTO> canalizar(
            @PathVariable Long id,
            @Valid @RequestBody CanarizarRequest request) {
        return ResponseEntity.ok(quejaService.canalizar(id, request));
    }
}
