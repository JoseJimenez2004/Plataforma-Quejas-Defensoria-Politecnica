package ipn.escom.defensoria.primercontacto.repository;

import ipn.escom.defensoria.primercontacto.entity.CitaPrimerContacto;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface CitaPrimerContactoRepository
        extends JpaRepository<CitaPrimerContacto, Long> {

    List<CitaPrimerContacto>
    findByExpedienteIdOrderByFechaCitaDescHoraCitaDesc(
            Long expedienteId
    );

    List<CitaPrimerContacto>
    findByFolioOrderByFechaCitaDescHoraCitaDesc(
            String folio
    );

    List<CitaPrimerContacto>
    findByFechaCitaOrderByHoraCitaAsc(
            LocalDate fechaCita
    );

    List<CitaPrimerContacto>
    findByAnalistaIdOrderByFechaCitaAscHoraCitaAsc(
            Long analistaId
    );

    List<CitaPrimerContacto>
    findByEstatusOrderByFechaCitaAscHoraCitaAsc(
            String estatus
    );

    boolean existsByFolioAndEstatusNot(
            String folio,
            String estatus
    );

    boolean existsByExpedienteIdAndEstatusNot(
            Long expedienteId,
            String estatus
    );
}