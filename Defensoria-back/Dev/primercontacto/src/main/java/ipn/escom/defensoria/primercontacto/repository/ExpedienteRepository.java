package ipn.escom.defensoria.primercontacto.repository;

import ipn.escom.defensoria.primercontacto.entity.Expediente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.List;

@Repository
public interface ExpedienteRepository extends JpaRepository<Expediente, Long> {
    
    Optional<Expediente> findByFolio(String folio);
    
    List<Expediente> findByEstatus(String estatus);
    
    List<Expediente> findByPrioridad(String prioridad);
}