package ipn.escom.defensoria.primercontacto.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotaAnalisisDTO {

    private Long id;
    private Long quejaId;
    private String folio;
    private Long analistaId;
    private String analistaNombre;
    private String contenido;
    private String fechaCreacion;
    private String fechaActualizacion;
}