package ipn.escom.defensoria.revision_service.model;

import lombok.Data;

/** Body de POST /api/revision/conciliaciones -- el personal solo da el folio, el correo del
 * quejoso se resuelve internamente a partir de la queja. */
@Data
public class CrearConciliacionRequest {
    private String numeroFolio;
    private String asunto;
    private String terminos;
}
