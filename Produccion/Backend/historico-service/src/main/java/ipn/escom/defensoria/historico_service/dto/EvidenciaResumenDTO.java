package ipn.escom.defensoria.historico_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EvidenciaResumenDTO {
    private Long id;
    private String nombreArchivo;
    private String tipoMime;
    private String tipo;
    private Long tamanioBytes;
    private String fechaSubida;
}
