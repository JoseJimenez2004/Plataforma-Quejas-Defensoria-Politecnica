package ipn.escom.defensoria.catalogo_service.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ipn.escom.defensoria.catalogo_service.entity.Dependencia;

@Repository
public interface DependenciaRepository extends JpaRepository<Dependencia, Long> {
    Optional<Dependencia> findByClave(String clave);

    List<Dependencia> findByActivoTrueOrderByNombreAsc();

    List<Dependencia> findByActivoTrueAndTipoOrderByNombreAsc(String tipo);
}
