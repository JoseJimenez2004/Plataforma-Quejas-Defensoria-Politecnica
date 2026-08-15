package ipn.escom.defensoria.subdefensoria.repository;

import ipn.escom.defensoria.subdefensoria.entity.AcuerdoConclusion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AcuerdoConclusionRepository extends JpaRepository<AcuerdoConclusion, Long> {
    Optional<AcuerdoConclusion> findByExpedienteId(Long expedienteId);
}
