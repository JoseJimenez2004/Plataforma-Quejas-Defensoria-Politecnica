package ipn.escom.defensoria.revision_service.model;

import java.time.LocalDateTime;

/**
 * Una fila del Historial paginado (2026-10-16).
 *
 * @param resultado      TURNADO o RECHAZADO (lo que decidió Recepción)
 * @param estatusActual  dónde va la queja hoy (p. ej. "En análisis · Primer Contacto")
 * @param horasAtencion  horas entre que llegó y Recepción la atendió; null si no hay fecha
 */
public record HistorialFilaModel(
        String numeroFolio,
        LocalDateTime fechaCreacion,
        LocalDateTime fechaAtencion,
        Double horasAtencion,
        String nombreQuejoso,
        String unidadAcademicaClave,
        String tipoIngreso,
        String resultado,
        String estatusActual,
        String atendioNombre,
        boolean esMia,
        String folioPrimerContacto,
        String defensorAsignado) {
}
