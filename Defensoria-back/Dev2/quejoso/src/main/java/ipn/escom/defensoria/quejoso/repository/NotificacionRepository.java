package ipn.escom.defensoria.quejoso.repository;

import ipn.escom.defensoria.quejoso.entity.NotificacionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface NotificacionRepository extends JpaRepository<NotificacionEntity, Long> {

    /**
     * Busca las notificaciones de un usuario que aún no han expirado.
     * Implementa tu regla de los 30 días de visibilidad.
     */
    List<NotificacionEntity> findByUsuarioIdAndFechaExpiracionAfterOrderByFechaCreacionDesc(
            Long usuarioId,
            LocalDateTime ahora
    );

    // Para mostrar el puntito rojo de "pendientes" en el mockup MQ-11/MQ-18
    long countByUsuarioIdAndLeidaFalse(Long usuarioId);
}