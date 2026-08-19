package ipn.escom.defensoria.primercontacto.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubdefensoriaIngresoResponse {

    /*
     * ID interno de Subdefensoría.
     * Primer Contacto NO lo utiliza como relación.
     */
    private Long id;

    /*
     * Folio nuevo generado por Subdefensoría.
     * Ejemplo: SD-A1B2C3D4
     */
    private String folio;

    /*
     * Folio que recibió de Primer Contacto.
     */
    private String folioOrigen;

    private String estatus;
}