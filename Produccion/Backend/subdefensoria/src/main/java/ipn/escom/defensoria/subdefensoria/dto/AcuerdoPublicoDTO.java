package ipn.escom.defensoria.subdefensoria.dto;

import lombok.*;

/**
 * Lo que ve el quejoso al abrir el enlace de su acuerdo de conciliación (correo o
 * notificación), sin iniciar sesión. Nada de ids internos.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AcuerdoPublicoDTO {

    private String folioQueja;
    private String quejosoNombre;
    private String asunto;
    private String textoAcuerdo;
    private String fechaEnvio;

    /* ACEPTADO | RECHAZADO | null (sin responder). */
    private String respuestaQuejoso;
    private String comentarioQuejoso;
    private String fechaRespuesta;

    /* true cuando ya se emitió el acuerdo de conclusión (caso cerrado). */
    private boolean concluido;

    /* true si todavía puede aceptar o rechazar. */
    private boolean puedeResponder;
}
