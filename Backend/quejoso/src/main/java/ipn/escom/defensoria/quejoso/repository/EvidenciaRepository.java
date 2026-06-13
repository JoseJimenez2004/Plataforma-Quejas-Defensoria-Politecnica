package ipn.escom.defensoria.quejoso.repository;

import ipn.escom.defensoria.quejoso.entity.EvidenciaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EvidenciaRepository extends JpaRepository<EvidenciaEntity, Long> {
    // Hereda los métodos deleteById y save automáticamente
}