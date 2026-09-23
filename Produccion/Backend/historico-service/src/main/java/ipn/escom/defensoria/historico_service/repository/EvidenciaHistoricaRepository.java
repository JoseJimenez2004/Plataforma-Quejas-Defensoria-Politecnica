package ipn.escom.defensoria.historico_service.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import ipn.escom.defensoria.historico_service.entity.EvidenciaHistorica;

@Repository
public interface EvidenciaHistoricaRepository extends JpaRepository<EvidenciaHistorica, Long> {

    List<EvidenciaHistorica> findByQuejaHistoricaId(Long quejaHistoricaId);
}
