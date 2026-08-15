package ipn.escom.defensoria.primercontacto.repository;

import ipn.escom.defensoria.primercontacto.entity.DictamenPrimerContacto;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DictamenPrimerContactoRepository extends JpaRepository<DictamenPrimerContacto, Long> {

    Optional<DictamenPrimerContacto> findByQuejaId(Long quejaId);

    Optional<DictamenPrimerContacto> findByFolio(String folio);

    boolean existsByQuejaId(Long quejaId);

    boolean existsByFolio(String folio);
}