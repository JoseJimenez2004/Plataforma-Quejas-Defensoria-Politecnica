package ipn.escom.defensoria.queja_service.dto;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Versión "ligera" de QuejaEvidencia para listarla en el detalle de una queja — sin el
 * campo `contenido` (los bytes del archivo), que solo se sirve completo cuando alguien
 * pide descargar esa evidencia en específico. */
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
