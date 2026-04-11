package ipn.escom.defensoria.recepcionista.repository;

import ipn.escom.defensoria.recepcionista.entity.cat.CatEstatusQueja;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repositorio JPA para el catálogo de estatus de quejas.
 *
 * Se usa en el servicio para obtener el objeto CatEstatusQueja
 * por su nombre antes de asignarlo a una queja.
 *
 * Ejemplo de uso en QuejaServiceImpl:
 *   catEstatusQuejaRepository.findByNombre("EN_REVISION")
 *
 * Retorna Optional para que el servicio pueda lanzar una excepción
 * descriptiva si el estatus solicitado no existe en el catálogo
 * (lo que indicaría que el init.sql no tiene ese valor).
 */
@Repository
public interface CatEstatusQuejaRepository extends JpaRepository<CatEstatusQueja, Integer> {

    Optional<CatEstatusQueja> findByNombre(String nombre);
}
