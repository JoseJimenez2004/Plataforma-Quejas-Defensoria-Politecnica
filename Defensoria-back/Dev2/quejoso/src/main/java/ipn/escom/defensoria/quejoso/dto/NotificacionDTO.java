package ipn.escom.defensoria.quejoso.dto;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class NotificacionDTO {
    private Long id;
    private String titulo;
    private String mensaje;
    private String fecha; // Formato amigable: "Hace 20 min" o "Ayer, 10:45 AM"
    private String tipo;   // REQUERIMIENTO, ESTATUS, CONCILIACION
    private boolean leida;
    private String folioRelacionado;
}