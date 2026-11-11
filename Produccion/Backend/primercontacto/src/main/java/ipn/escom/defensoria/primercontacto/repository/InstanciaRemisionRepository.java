package ipn.escom.defensoria.primercontacto.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import ipn.escom.defensoria.primercontacto.entity.InstanciaRemision;

public interface InstanciaRemisionRepository extends JpaRepository<InstanciaRemision, String> {
    List<InstanciaRemision> findAllByOrderByOrdenAsc();
}
