package ipn.escom.defensoria.primercontacto.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PrevisualizacionNotificacionDTO {

    private String folio;
    private String nombreQuejoso;
    private String correoQuejoso;
    private String asunto;
    private String cuerpoMensaje;
    private String tipoNotificacion;
}