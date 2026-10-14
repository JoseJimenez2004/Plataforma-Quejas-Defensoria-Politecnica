package ipn.escom.defensoria.admin_service.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import ipn.escom.defensoria.admin_service.entity.PersonalAdministrativo;
import ipn.escom.defensoria.admin_service.entity.RolStaff;

public interface PersonalAdministrativoRepository extends JpaRepository<PersonalAdministrativo, Long> {
    Optional<PersonalAdministrativo> findByCorreoInstitucional(String correoInstitucional);

    List<PersonalAdministrativo> findAllByOrderByFechaCreacionDesc();

    long countByActivoTrue();

    long countByActivoTrueAndRolIn(Collection<RolStaff> roles);
}
