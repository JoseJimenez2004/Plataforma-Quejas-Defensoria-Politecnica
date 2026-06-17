package ipn.escom.defensoria.recepcion.repository;

import ipn.escom.defensoria.recepcion.model.Queja;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface QuejaRepository extends JpaRepository<Queja, Long> {
}