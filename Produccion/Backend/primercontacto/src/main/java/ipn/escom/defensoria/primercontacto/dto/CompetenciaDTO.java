package ipn.escom.defensoria.primercontacto.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import jakarta.persistence.Column;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CompetenciaDTO {

    /*
     * Folio propio de Primer Contacto.
     * Ejemplo: PC-A1B2C3D4
     */
    @NotBlank
    private String folio;

    @NotBlank
    private String justificacion;

    @NotBlank
    private String areaTurno;

    @NotBlank
    private String responsableTurno;

<<<<<<< HEAD:Produccion/Backend/primercontacto/src/main/java/ipn/escom/defensoria/primercontacto/dto/CompetenciaDTO.java
=======
    @Column(name = "observaciones", columnDefinition = "TEXT")
>>>>>>> b41378653456fe3429bbc88f39e3f15062f0a782:Defensoria-back/Dev2/primercontacto/src/main/java/ipn/escom/defensoria/primercontacto/dto/CompetenciaDTO.java
    private String observaciones;
}