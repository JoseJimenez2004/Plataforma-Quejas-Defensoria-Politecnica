package ipn.escom.defensoria.primercontacto.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import ipn.escom.defensoria.primercontacto.entity.PersonalAdministrativo;

@Repository
public interface PersonalAdministrativoRepository
        extends JpaRepository<PersonalAdministrativo, Long> {

    Optional<PersonalAdministrativo> findByCorreoInstitucional(
            String correoInstitucional
    );
}