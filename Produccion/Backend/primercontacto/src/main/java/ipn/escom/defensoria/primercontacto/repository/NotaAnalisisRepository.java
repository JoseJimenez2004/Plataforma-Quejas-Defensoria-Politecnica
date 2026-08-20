package ipn.escom.defensoria.primercontacto.repository;

import ipn.escom.defensoria.primercontacto.entity.NotaAnalisis;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface NotaAnalisisRepository
        extends JpaRepository<NotaAnalisis, Long> {

    List<NotaAnalisis> findByExpedienteIdOrderByFechaCreacionDesc(
            Long expedienteId
    );

    List<NotaAnalisis> findByFolioOrderByFechaCreacionDesc(
            String folio
    );

    Optional<NotaAnalisis> findTopByExpedienteIdOrderByFechaCreacionDesc(
            Long expedienteId
    );
}