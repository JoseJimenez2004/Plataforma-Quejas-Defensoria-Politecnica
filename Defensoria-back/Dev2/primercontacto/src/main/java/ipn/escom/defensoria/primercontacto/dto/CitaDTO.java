package ipn.escom.defensoria.primercontacto.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CitaDTO {

    private Long id;
    private Long quejaId;
    private String folio;
    private Long quejosoId;
    private String quejosoNombre;
    private Long analistaId;
    private String analistaNombre;
    private String fechaCita;
    private String horaCita;
    private String tipoCita;
    private String motivo;
    private String estatus;
    private String fechaCreacion;
}