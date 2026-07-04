package ipn.escom.defensoria.quejoso.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ConciliacionDTO {
    private String folioQueja;
    private String tituloAcuerdo; // "ACUERDO DE CONCILIACIÓN"
    private String contenidoHtml; // El texto con los puntos detallados
    private String fechaPropuesta;
    private String urlPdf; // URL del PDF enviado por el defensor
}