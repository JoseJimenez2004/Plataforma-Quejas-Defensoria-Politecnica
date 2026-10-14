package ipn.escom.defensoria.primercontacto.repository;

import ipn.escom.defensoria.primercontacto.entity.SolicitudDenunciado;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SolicitudDenunciadoRepository extends JpaRepository<SolicitudDenunciado, Long> {

    List<SolicitudDenunciado> findByExpedienteIdOrderByFechaEnvioDesc(Long expedienteId);
}
