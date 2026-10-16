package ipn.escom.defensoria.revision_service.model;

import java.util.List;

/** Respuesta del Historial paginado: totales + una página de filas (10 por defecto). */
public record HistorialPaginaModel(
        HistorialResumenModel resumen,
        List<HistorialFilaModel> filas,
        int pagina,
        int tamanio,
        long totalElementos,
        int totalPaginas) {
}
