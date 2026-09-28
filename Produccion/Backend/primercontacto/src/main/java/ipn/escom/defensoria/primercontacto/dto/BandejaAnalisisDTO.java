package ipn.escom.defensoria.primercontacto.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BandejaAnalisisDTO {

    private Long expedienteId;

    /*
     * Folio propio de Primer Contacto.
     * Ejemplo: PC-A1B2C3D4
     */
    private String folio;

    /*
     * Folio proveniente de Revisión.
     * Ejemplo: FOL-12345678
     */
    private String folioOrigen;

    private String nombreQuejoso;
    private String unidadAcademica;
    private String tema;
    private String prioridad;
    private String estatus;
    private String fechaRecepcion;

    /*
     * Indicador aparte, NO un estado: el expediente tiene una cita
     * programada o confirmada. Sustituye al pseudo-estado CON_CITA,
     * que tapaba el estatus real y el dashboard calculaba distinto.
     */
    private boolean tieneCitaActiva;
}