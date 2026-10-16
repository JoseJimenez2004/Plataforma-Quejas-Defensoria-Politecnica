package ipn.escom.defensoria.subdefensoria.repository;

import ipn.escom.defensoria.subdefensoria.entity.QuejaReferencia;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface QuejaReferenciaRepository extends JpaRepository<QuejaReferencia, Long> {

    Optional<QuejaReferencia> findByNumeroFolio(String numeroFolio);
}
