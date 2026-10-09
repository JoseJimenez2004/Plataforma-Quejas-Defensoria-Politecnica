package ipn.escom.defensoria.admin_service.model;

import java.util.List;

/**
 * Indicadores de quejas para el dashboard de la Defensora (CU-ADM-05). Se calculan directo
 * sobre defensoria_db (tablas quejas, dependencias y respuestas_denunciado).
 */
public record IndicadoresQuejasModel(
        long total,
        long recibidas,
        long enValidacion,
        long corregidas,
        long turnadas,
        long rechazadas,
        long canceladas,
        long ultimos30Dias,
        long turnadasEsteMes,
        long respuestasDenunciado,
        List<ConteoModel> unidadesConMasQuejas,
        List<ConteoModel> porMes) {

    /** Una etiqueta (unidad académica o mes "2026-10") y su total. */
    public record ConteoModel(String clave, String etiqueta, long total) {
    }
}
