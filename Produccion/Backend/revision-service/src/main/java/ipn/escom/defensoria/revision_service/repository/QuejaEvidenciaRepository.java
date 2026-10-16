package ipn.escom.defensoria.revision_service.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import ipn.escom.defensoria.revision_service.entity.QuejaEvidencia;

@Repository
public interface QuejaEvidenciaRepository extends JpaRepository<QuejaEvidencia, Long> {
    List<QuejaEvidencia> findByQuejaId(Long quejaId);

    /** Para el ✓/✗ de documentación en la bandeja: cuenta sin traer el contenido (BYTEA) de
     * los archivos. Antes se usaba findByQuejaId y cada refresco de la bandeja bajaba de la BD
     * todos los archivos de todas las quejas pendientes. */
    long countByQuejaId(Long quejaId);
}
