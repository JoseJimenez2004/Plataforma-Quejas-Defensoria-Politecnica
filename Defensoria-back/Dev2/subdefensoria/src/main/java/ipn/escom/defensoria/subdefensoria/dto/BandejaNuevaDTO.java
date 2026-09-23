package ipn.escom.defensoria.subdefensoria.dto;

import lombok.*;

/**
 * Fila de la pantalla P14.A "Quejas Nuevas y Asignacion de
 * Investigacion": expedientes recien turnados por Primer Contacto
 * que todavia no tienen ningun oficio de solicitud de informacion.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BandejaNuevaDTO {

    private Long expedienteId;
    private String folio;
    private String fechaAdmision;
    private String quejosoNombre;
    private String asunto;
    private String unidadAcademica;
    /** SOLICITUD_INFORMACION o GESTION_DIRECTOR: que oficio hay que redactar a continuacion. */
    private String siguienteFase;
}
