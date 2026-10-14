package ipn.escom.defensoria.primercontacto.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SolicitudDenunciadoDTO {

    private Long id;
    private String folio;
    private String folioQueja;
    private String correoDenunciado;
    /* false si notificaciones-service no aceptó el correo: el analista puede reenviarlo. */
    private boolean correoEnviado;
    private String enviadoPorNombre;
    private String fechaEnvio;
}
