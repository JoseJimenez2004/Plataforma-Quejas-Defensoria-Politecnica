package ipn.escom.defensoria.revision_service.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ipn.escom.defensoria.revision_service.entity.AcuerdoConciliacion;

@Repository
public interface AcuerdoConciliacionRepository extends JpaRepository<AcuerdoConciliacion, Long> {
    List<AcuerdoConciliacion> findByNumeroFolioOrderByFechaEmisionDesc(String numeroFolio);

    List<AcuerdoConciliacion> findAllByOrderByFechaEmisionDesc();
}
