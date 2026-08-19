package ipn.escom.defensoria.revision_service.model;

import lombok.Data;

@Data
public class PrimerContactoIngresoResponse {

    /*
     * ID interno de Primer Contacto.
     * Revision no debe utilizarlo para relacionar sus datos.
     */
    private Long id;

    /*
     * Nuevo folio generado por Primer Contacto.
     */
    private String folio;

    /*
     * Folio original que salió de Revisión.
     */
    private String folioOrigen;

    private String estatus;
}