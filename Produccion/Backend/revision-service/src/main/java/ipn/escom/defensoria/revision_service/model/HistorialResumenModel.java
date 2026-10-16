package ipn.escom.defensoria.revision_service.model;

/** Totales del Historial con los filtros aplicados (periodo, "solo mías", búsqueda). */
public record HistorialResumenModel(
        long total,
        long turnadas,
        long rechazadas,
        long atendidasPorMi,
        Double horasPromedioAtencion) {
}
