package ipn.escom.defensoria.primercontacto.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotaAnalisisDTO {

    private Long id;
    private Long expedienteId;
    private String folio;
    private Long analistaId;
    private String analistaNombre;
    /** DDP/PC/OF/año/id, calculado (no se guarda). */
    private String numeroOficio;
    private String asunto;
    private String contenido;
    private String fechaCreacion;
    private String fechaActualizacion;
}