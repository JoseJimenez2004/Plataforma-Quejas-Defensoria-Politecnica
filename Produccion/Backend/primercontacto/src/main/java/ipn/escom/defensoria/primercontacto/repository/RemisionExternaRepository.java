package ipn.escom.defensoria.primercontacto.repository;

import ipn.escom.defensoria.primercontacto.entity.RemisionExterna;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RemisionExternaRepository
        extends JpaRepository<RemisionExterna, Long> {

    Optional<RemisionExterna> findByExpedienteId(
            Long expedienteId
    );

    Optional<RemisionExterna> findByFolio(
            String folio
    );

    boolean existsByExpedienteId(
            Long expedienteId
    );

    boolean existsByFolio(
            String folio
    );
}