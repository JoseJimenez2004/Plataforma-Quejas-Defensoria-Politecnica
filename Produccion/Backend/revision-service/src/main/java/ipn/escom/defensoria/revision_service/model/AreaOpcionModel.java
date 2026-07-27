package ipn.escom.defensoria.revision_service.model;

import lombok.AllArgsConstructor;
import lombok.Data;

/** Opción del combo "Área a la que se turna", leída de catalogo-service. */
@Data
@AllArgsConstructor
public class AreaOpcionModel {
    private String clave;
    private String nombre;
}
