package ipn.escom.defensoria.primercontacto.dto;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExpedienteAnalisisDTO {

    /*
     * ID interno de Primer Contacto.
     */
    private Long expedienteId;

    /*
     * Folio propio de Primer Contacto.
     * Ejemplo: PC-A1B2C3D4
     */
    private String folio;

    /*
     * Folio con el que llegó desde Revisión.
     * Ejemplo: FOL-12345678
     */
    private String folioOrigen;

    /*
     * Si ya pasó a Subdefensoría:
     * SD-XXXXXXXX
     */
    private String folioSubdefensoria;

    private String tema;
    private String descripcionHechos;
    private String fechaRecepcion;
    private String estatus;
    private String prioridad;

    private QuejosoDTO quejoso;

    private List<EvidenciaDTO> evidencias;

    private List<NotaAnalisisDTO> notas;
}