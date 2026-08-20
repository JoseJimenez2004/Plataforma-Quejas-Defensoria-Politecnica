package ipn.escom.defensoria.primercontacto.repository;

import ipn.escom.defensoria.primercontacto.entity.DictamenPrimerContacto;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DictamenPrimerContactoRepository
        extends JpaRepository<DictamenPrimerContacto, Long> {

    Optional<DictamenPrimerContacto> findByExpedienteId(
            Long expedienteId
    );

    Optional<DictamenPrimerContacto> findByFolio(
            String folio
    );

    boolean existsByExpedienteId(
            Long expedienteId
    );

    boolean existsByFolio(
            String folio
    );
}