package ipn.escom.defensoria.quejoso.repository;

import ipn.escom.defensoria.quejoso.entity.NotificacionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface NotificacionRepository extends JpaRepository<NotificacionEntity, Long> {

    // Spring Data JPA genera la consulta automáticamente basándose en el nombre
    List<NotificacionEntity> findByUsuarioIdAndFechaExpiracionAfterOrderByFechaCreacionDesc(
            Long usuarioId,
            LocalDateTime ahora
    );
}