package ipn.escom.defensoria.primercontacto.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EvidenciaDTO {

    private Long id;
    private String nombreArchivo;
    private String tipoArchivo;
    private String urlArchivo;
    private String fechaCarga;
}