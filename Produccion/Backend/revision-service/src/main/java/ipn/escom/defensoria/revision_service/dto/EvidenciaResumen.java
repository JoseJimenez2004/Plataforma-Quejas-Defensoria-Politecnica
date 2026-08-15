package ipn.escom.defensoria.revision_service.dto;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EvidenciaResumen {
    private Long id;
    private String nombreArchivo;
    private String tipoMime;
    private Long tamanioBytes;
    private LocalDateTime fechaSubida;
}
