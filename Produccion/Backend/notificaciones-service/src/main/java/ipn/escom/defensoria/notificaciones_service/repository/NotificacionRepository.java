package ipn.escom.defensoria.notificaciones_service.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ipn.escom.defensoria.notificaciones_service.entity.Notificacion;

@Repository
public interface NotificacionRepository extends JpaRepository<Notificacion, Long> {
    List<Notificacion> findByCorreoDestinoOrderByFechaCreacionDesc(String correoDestino);

    long countByCorreoDestinoAndLeidaFalse(String correoDestino);
}
