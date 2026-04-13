package ipn.escom.defensoria.usuarios.repository;

import ipn.escom.defensoria.usuarios.entity.NotificacionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface NotificacionRepository extends JpaRepository<NotificacionEntity, Long> {
}