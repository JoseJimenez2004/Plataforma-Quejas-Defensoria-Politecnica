package ipn.escom.defensoria.catalogo_service.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ipn.escom.defensoria.catalogo_service.entity.InstanciaRemision;

@Repository
public interface InstanciaRemisionRepository extends JpaRepository<InstanciaRemision, String> {
    List<InstanciaRemision> findAllByOrderByOrdenAsc();
}
