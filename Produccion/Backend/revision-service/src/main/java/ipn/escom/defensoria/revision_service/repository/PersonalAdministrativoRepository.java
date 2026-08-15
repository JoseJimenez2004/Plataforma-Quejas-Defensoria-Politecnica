package ipn.escom.defensoria.revision_service.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import ipn.escom.defensoria.revision_service.entity.PersonalAdministrativo;
import ipn.escom.defensoria.revision_service.entity.RolStaff;

@Repository
public interface PersonalAdministrativoRepository extends JpaRepository<PersonalAdministrativo, Long> {
    List<PersonalAdministrativo> findByRolInAndActivoTrueOrderByNombreCompletoAsc(List<RolStaff> roles);
}
