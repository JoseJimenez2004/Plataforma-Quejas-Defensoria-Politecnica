package ipn.escom.defensoria.quejoso.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class EvidenciaDTO {
    private String nombreArchivo;
    private String urlDescarga;
}