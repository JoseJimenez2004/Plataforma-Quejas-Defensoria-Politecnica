package ipn.escom.defensoria.primercontacto.repository;

import ipn.escom.defensoria.primercontacto.entity.AcuerdoConciliacion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface AcuerdoConciliacionRepository
        extends JpaRepository<AcuerdoConciliacion, Long> {

    List<AcuerdoConciliacion> findByNumeroFolioAndFechaEmisionGreaterThanEqualOrderByFechaEmisionDesc(
            String numeroFolio,
            LocalDateTime desde
    );

    List<AcuerdoConciliacion> findByNumeroFolioInAndEstado(
            List<String> numerosFolio,
            String estado
    );
}
