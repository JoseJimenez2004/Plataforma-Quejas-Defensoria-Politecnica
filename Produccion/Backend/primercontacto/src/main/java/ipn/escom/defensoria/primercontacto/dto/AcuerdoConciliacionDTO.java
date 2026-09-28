package ipn.escom.defensoria.primercontacto.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AcuerdoConciliacionDTO {

    private Long id;

    /* Folio de la queja (FOL-...), el que ve el quejoso. */
    private String numeroFolio;

    private String asunto;
    private String terminos;

    /* PENDIENTE | ACEPTADO | RECHAZADO */
    private String estado;

    private String fechaEmision;
    private String fechaRespuesta;
    private String comentarioQuejoso;

    private String creadoPor;
    private String creadoPorNombre;
}
