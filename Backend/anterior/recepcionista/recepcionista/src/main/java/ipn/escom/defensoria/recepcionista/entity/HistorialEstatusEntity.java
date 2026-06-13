package ipn.escom.defensoria.recepcionista.entity;

import ipn.escom.defensoria.recepcionista.entity.cat.CatEstatusQueja;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Entidad JPA que mapea la tabla "historial_estatus_queja" de PostgreSQL.
 *
 * Registra cada cambio de estatus que ocurre en una queja, funcionando
 * como una bitácora de auditoría. Cada vez que la recepcionista acepta,
 * rechaza o canaliza una queja, se inserta un nuevo registro aquí.
 *
 * Esto permite saber:
 * - Qué estatus ha tenido la queja a lo largo del tiempo
 * - Quién hizo el cambio (usuarioModificoId)
 * - Cuándo ocurrió (fechaCambio)
 * - Por qué (comentarios — incluye el motivo de rechazo o notas de canalización)
 *
 * Esta entidad solo se crea (INSERT), nunca se modifica ni elimina.
 */
@Entity
@Table(name = "historial_estatus_queja")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HistorialEstatusEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ID de la queja a la que pertenece este cambio de estatus
    @Column(name = "queja_id", nullable = false)
    private Long quejaId;

    // El nuevo estatus al que cambió la queja
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "estatus_id", nullable = false)
    private CatEstatusQueja estatus;

    // ID del usuario que realizó el cambio (hardcoded=1 hasta que exista auth)
    @Column(name = "usuario_modifico_id", nullable = false)
    private Integer usuarioModificoId;

    // Descripción del cambio: motivo de rechazo, notas de canalización, etc.
    @Column(columnDefinition = "TEXT")
    private String comentarios;

    // Timestamp exacto del momento en que se realizó el cambio
    @Column(name = "fecha_cambio")
    private LocalDateTime fechaCambio;
}
