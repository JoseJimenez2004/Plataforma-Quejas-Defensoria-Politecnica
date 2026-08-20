package ipn.escom.defensoria.primercontacto.repository;

import ipn.escom.defensoria.primercontacto.entity.EvidenciaPrimerContacto;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EvidenciaPrimerContactoRepository
        extends JpaRepository<EvidenciaPrimerContacto, Long> {

    List<EvidenciaPrimerContacto> findByExpedienteId(
            Long expedienteId
    );
}