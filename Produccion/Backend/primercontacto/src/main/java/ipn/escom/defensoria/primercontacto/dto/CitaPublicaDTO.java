package ipn.escom.defensoria.primercontacto.dto;

import lombok.*;

/**
 * Lo que ve el quejoso al abrir el enlace de su cita (correo o notificación), sin iniciar
 * sesión. Solo datos de la cita: nada de ids internos ni del analista más allá del nombre.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CitaPublicaDTO {

    private String folioQueja;
    private String quejosoNombre;
    private String fechaCita;
    private String horaCita;
    private String tipoCita;
    private String motivo;
    private String estatus;
    private String fechaLimiteRespuesta;
    private String motivoCancelacionQuejoso;
    private String analistaNombre;

    /* true si todavía puede confirmar o cancelar (PROGRAMADA y dentro del plazo). */
    private boolean puedeResponder;
}
