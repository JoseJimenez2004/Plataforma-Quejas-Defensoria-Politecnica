package ipn.escom.defensoria.revision_service.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import ipn.escom.defensoria.revision_service.entity.QuejaEvidencia;

@Repository
public interface QuejaEvidenciaRepository extends JpaRepository<QuejaEvidencia, Long> {
    List<QuejaEvidencia> findByQuejaId(Long quejaId);
}
