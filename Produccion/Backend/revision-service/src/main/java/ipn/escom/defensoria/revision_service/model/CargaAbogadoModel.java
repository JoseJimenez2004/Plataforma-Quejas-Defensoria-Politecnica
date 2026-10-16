package ipn.escom.defensoria.revision_service.model;

import java.util.List;

/**
 * Carga de trabajo de un abogado para decidir a quién turnar (2026-10-16).
 *
 * @param cargaActiva        quejas a su nombre que siguen en trámite
 * @param totalAsignadas     todas las que se le han turnado
 * @param foliosAntecedente  antecedentes EN CURSO de esta queja que ya lleva este abogado
 * @param sugerido           el sistema lo recomienda (ver RevisionQuejaService.abogados)
 * @param motivoSugerencia   por qué se le sugiere
 */
public record CargaAbogadoModel(
        Long id,
        String nombre,
        String rol,
        long cargaActiva,
        long totalAsignadas,
        List<String> foliosAntecedente,
        boolean sugerido,
        String motivoSugerencia) {
}
