package ipn.escom.defensoria.subdefensoria.repository;

import ipn.escom.defensoria.subdefensoria.entity.RecordatorioUrgencia;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RecordatorioUrgenciaRepository extends JpaRepository<RecordatorioUrgencia, Long> {

    List<RecordatorioUrgencia> findByOficioIdOrderByFechaEnvioDesc(Long oficioId);
}
