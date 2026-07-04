package ipn.escom.defensoria.quejoso.entity;

import jakarta.persistence.Entity;

import jakarta.persistence.Table;
import jakarta.persistence.Id;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.FetchType;
import jakarta.persistence.PrePersist;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "notificaciones")
@Data
public class NotificacionEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String titulo;
    private String mensaje;
    private LocalDateTime fechaCreacion = LocalDateTime.now();
    private LocalDateTime fechaExpiracion; // NOW + 30 días
    private boolean leida = false;

    private String tipo; // Estatus, Requerimiento, Conciliación
    private String folioQueja;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id")
    private Usuario usuario;

    @PrePersist
    protected void antesDeGuardar() {
        this.fechaCreacion = LocalDateTime.now();
        this.fechaExpiracion = this.fechaCreacion.plusDays(30);
    }
}