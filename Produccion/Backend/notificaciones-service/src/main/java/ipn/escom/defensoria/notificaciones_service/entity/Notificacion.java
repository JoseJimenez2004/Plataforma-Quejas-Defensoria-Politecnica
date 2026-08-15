package ipn.escom.defensoria.notificaciones_service.entity;

import java.time.LocalDateTime;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

/**
 * Centro de notificaciones por usuario -- antes notificaciones-service solo mandaba correo
 * "a ciegas" (POST /enviar, sin guardar nada). Esta entidad persiste un aviso por evento
 * (inicio de sesión, queja creada, cambio de estatus, conciliación) para que el quejoso los
 * vea dentro del panel, no solo por correo.
 */
@Data
@Entity
@Table(name = "notificaciones")
public class Notificacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "correo_destino", nullable = false)
    private String correoDestino;

    /** LOGIN | QUEJA_CREADA | CAMBIO_ESTATUS | CONCILIACION | GENERAL */
    @Column(nullable = false)
    private String tipo;

    @Column(nullable = false)
    private String titulo;

    @Column(columnDefinition = "TEXT")
    private String mensaje;

    @Column(nullable = false)
    private boolean leida = false;

    @Column(name = "fecha_creacion")
    private LocalDateTime fechaCreacion = LocalDateTime.now();

    /** Ruta relativa opcional dentro del panel (ej. "/panel/mis-quejas/FOL-XXXX") para que
     * el frontend pueda llevar al usuario directo al detalle relacionado. */
    private String enlace;
}
