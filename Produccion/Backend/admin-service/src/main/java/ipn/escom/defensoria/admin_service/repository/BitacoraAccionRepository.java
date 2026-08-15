package ipn.escom.defensoria.admin_service.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import ipn.escom.defensoria.admin_service.entity.BitacoraAccion;

public interface BitacoraAccionRepository extends JpaRepository<BitacoraAccion, Long> {
    List<BitacoraAccion> findTop50ByOrderByFechaDesc();
}
