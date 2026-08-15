package ipn.escom.defensoria.queja_service.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ipn.escom.defensoria.queja_service.entity.AcuerdoConciliacion;

@Repository
public interface AcuerdoConciliacionRepository extends JpaRepository<AcuerdoConciliacion, Long> {
    List<AcuerdoConciliacion> findByCorreoInstitucionalOrderByFechaEmisionDesc(String correoInstitucional);
}
