package ipn.escom.defensoria.revision_service.model;

import java.util.List;

/**
 * Respuesta de la Bandeja de Entrada (2026-10-15): indicadores del área, UNA página de la cola
 * (10 por defecto, la más reciente arriba) y la carga de trabajo de cada recepcionista.
 * Antes regresaba todas las quejas pendientes de golpe; con cientos de quejas eso hacía lenta
 * la pantalla y el refresco automático cada 20 s.
 */
public record BandejaResumenModel(
        IndicadoresBandejaModel indicadores,
        List<QuejaResumenBandejaModel> lista,
        int pagina,
        int tamanio,
        long totalElementos,
        int totalPaginas,
        List<CargaRecepcionistaModel> equipo) {
}
