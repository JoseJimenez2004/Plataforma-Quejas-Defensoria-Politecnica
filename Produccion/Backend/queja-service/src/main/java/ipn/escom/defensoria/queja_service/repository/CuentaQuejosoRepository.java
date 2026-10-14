package ipn.escom.defensoria.queja_service.repository;

import ipn.escom.defensoria.queja_service.entity.CuentaQuejoso;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CuentaQuejosoRepository extends JpaRepository<CuentaQuejoso, Long> {

    Optional<CuentaQuejoso> findFirstByCorreoInstitucionalIgnoreCase(String correoInstitucional);
}
