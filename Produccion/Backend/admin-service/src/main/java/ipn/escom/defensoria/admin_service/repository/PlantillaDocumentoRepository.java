package ipn.escom.defensoria.admin_service.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import ipn.escom.defensoria.admin_service.entity.PlantillaDocumento;

public interface PlantillaDocumentoRepository extends JpaRepository<PlantillaDocumento, Long> {
    Optional<PlantillaDocumento> findByTipo(String tipo);

    List<PlantillaDocumento> findAllByOrderByNombreAsc();

    long countByActivaTrue();
}
