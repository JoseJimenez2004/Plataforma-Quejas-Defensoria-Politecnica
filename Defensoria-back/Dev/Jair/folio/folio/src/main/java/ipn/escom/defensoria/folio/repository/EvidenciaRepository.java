package ipn.escom.defensoria.folio.repository;

import ipn.escom.defensoria.folio.entity.EvidenciaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EvidenciaRepository extends JpaRepository<EvidenciaEntity, Long> {
}