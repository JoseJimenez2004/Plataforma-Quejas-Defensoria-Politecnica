package ipn.escom.defensoria.primercontacto.repository;

import ipn.escom.defensoria.primercontacto.entity.ResumenQueja;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ResumenQuejaRepository extends JpaRepository<ResumenQueja, Long> {

    Optional<ResumenQueja> findByFolio(String folio);
}
