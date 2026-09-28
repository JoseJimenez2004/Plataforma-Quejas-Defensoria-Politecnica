package ipn.escom.defensoria.primercontacto.dto;

import lombok.*;
<<<<<<< HEAD:Produccion/Backend/primercontacto/src/main/java/ipn/escom/defensoria/primercontacto/dto/ExpedienteEntranteRequest.java

=======
>>>>>>> b41378653456fe3429bbc88f39e3f15062f0a782:Defensoria-back/Dev2/primercontacto/src/main/java/ipn/escom/defensoria/primercontacto/dto/ExpedienteEntranteRequest.java
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExpedienteEntranteRequest {
<<<<<<< HEAD:Produccion/Backend/primercontacto/src/main/java/ipn/escom/defensoria/primercontacto/dto/ExpedienteEntranteRequest.java

    /*
     * Folio propio de Primer Contacto.
     * Este será el folioOrigen en Subdefensoría.
     */
    private String folioOrigen;

    private String asunto;

    private String descripcionHechos;

    private LocalDate fechaAdmision;

    private Long abogadoAsesorId;

    private String abogadoAsesorNombre;

    private QuejosoResumenRequest quejoso;

=======
    private Long quejaId;
    private String folio;
    private String asunto;
    private String descripcionHechos;
    private LocalDate fechaAdmision;
    private Long abogadoAsesorId;
    private String abogadoAsesorNombre;
    private QuejosoResumenRequest quejoso;
>>>>>>> b41378653456fe3429bbc88f39e3f15062f0a782:Defensoria-back/Dev2/primercontacto/src/main/java/ipn/escom/defensoria/primercontacto/dto/ExpedienteEntranteRequest.java
    private String observacionesAnalista;
}