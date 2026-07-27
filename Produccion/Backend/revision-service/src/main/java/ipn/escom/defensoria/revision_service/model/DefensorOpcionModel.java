package ipn.escom.defensoria.revision_service.model;

import lombok.AllArgsConstructor;
import lombok.Data;

/** Opción del combo "Defensor / Abogado Responsable", leída directo de
 * personal_administrativo (rol SUBDEFENSOR o DEFENSOR). */
@Data
@AllArgsConstructor
public class DefensorOpcionModel {
    private Long id;
    private String nombreCompleto;
    private String rol;
}
