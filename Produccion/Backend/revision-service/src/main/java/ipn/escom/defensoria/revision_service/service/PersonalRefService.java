package ipn.escom.defensoria.revision_service.service;

import java.util.List;

import org.springframework.stereotype.Service;

import ipn.escom.defensoria.revision_service.entity.PersonalAdministrativo;
import ipn.escom.defensoria.revision_service.entity.RolStaff;
import ipn.escom.defensoria.revision_service.model.DefensorOpcionModel;
import ipn.escom.defensoria.revision_service.repository.PersonalAdministrativoRepository;

/** Lee directo de "personal_administrativo" (misma tabla que administra admin-service) para
 * poblar el combo "Defensor / Abogado Responsable" -- se filtra por rol DEFENSOR/SUBDEFENSOR,
 * que son quienes reciben expedientes turnados. */
@Service
public class PersonalRefService {

    private final PersonalAdministrativoRepository repository;

    public PersonalRefService(PersonalAdministrativoRepository repository) {
        this.repository = repository;
    }

    public List<DefensorOpcionModel> listarDefensoresDisponibles() {
        List<PersonalAdministrativo> personal = repository.findByRolInAndActivoTrueOrderByNombreCompletoAsc(
                List.of(RolStaff.DEFENSOR, RolStaff.SUBDEFENSOR));
        return personal.stream()
                .map(p -> new DefensorOpcionModel(p.getId(), p.getNombreCompleto(), p.getRol().name()))
                .toList();
    }
}
