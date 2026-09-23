package ipn.escom.defensoria.subdefensoria.repository;

import ipn.escom.defensoria.subdefensoria.entity.RespuestaExterna;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RespuestaExternaRepository extends JpaRepository<RespuestaExterna, Long> {

    List<RespuestaExterna> findByExpedienteIdOrderByFechaRecepcionDesc(Long expedienteId);

    List<RespuestaExterna> findByOficioIdOrderByFechaRecepcionDesc(Long oficioId);
}
