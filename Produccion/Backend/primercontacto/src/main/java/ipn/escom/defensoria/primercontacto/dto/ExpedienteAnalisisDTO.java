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
<<<<<<< HEAD:Produccion/Backend/primercontacto/src/main/java/ipn/escom/defensoria/primercontacto/dto/ExpedienteAnalisisDTO.java

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

=======
>>>>>>> b41378653456fe3429bbc88f39e3f15062f0a782:Defensoria-back/Dev2/primercontacto/src/main/java/ipn/escom/defensoria/primercontacto/dto/ExpedienteAnalisisDTO.java
    private String tema;
    private String descripcionHechos;
    private String fechaRecepcion;
    private String estatus;
    private String prioridad;

    private QuejosoDTO quejoso;

    private List<EvidenciaDTO> evidencias;

    private List<NotaAnalisisDTO> notas;
}