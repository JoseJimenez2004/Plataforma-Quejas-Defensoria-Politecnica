package ipn.escom.defensoria.subdefensoria.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OficioDTO {

    private Long id;
    private Long expedienteId;
    private String folio;
    private String numeroOficio;
    private String fase;
    private String destinatarioNombre;
    private String destinatarioCorreo;
    private String unidadAcademica;
    private String contenidoRedactado;
    private String tipoPlazo;
    private String fechaEnvio;
    private String fechaLimite;
    private String estatus;
}
