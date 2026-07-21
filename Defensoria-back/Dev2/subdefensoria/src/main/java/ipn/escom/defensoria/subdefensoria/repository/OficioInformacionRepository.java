package ipn.escom.defensoria.subdefensoria.repository;

import ipn.escom.defensoria.subdefensoria.entity.OficioInformacion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface OficioInformacionRepository extends JpaRepository<OficioInformacion, Long> {

    List<OficioInformacion> findByExpedienteIdOrderByFechaEnvioDesc(Long expedienteId);

    List<OficioInformacion> findByFolioOrderByFechaEnvioDesc(String folio);

    /** El oficio vigente de un expediente es el mas reciente que no ha sido respondido. */
    Optional<OficioInformacion> findFirstByExpedienteIdAndEstatusNotOrderByFechaEnvioDesc(
            Long expedienteId, String estatusExcluido);

    List<OficioInformacion> findByEstatusOrderByFechaLimiteAsc(String estatus);

    boolean existsByExpedienteIdAndEstatus(Long expedienteId, String estatus);

    long countByEstatus(String estatus);
}
