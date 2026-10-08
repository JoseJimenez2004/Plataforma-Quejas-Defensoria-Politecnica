package ipn.escom.defensoria.primercontacto.repository;

import ipn.escom.defensoria.primercontacto.entity.AntecedenteExpediente;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AntecedenteExpedienteRepository
        extends JpaRepository<AntecedenteExpediente, Long> {

    List<AntecedenteExpediente> findByExpedienteIdOrderByFechaRegistroDesc(Long expedienteId);

    boolean existsByExpedienteIdAndOrigenAndFolioAntecedente(
            Long expedienteId,
            String origen,
            String folioAntecedente
    );
}
