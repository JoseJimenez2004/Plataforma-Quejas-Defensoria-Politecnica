package ipn.escom.defensoria.subdefensoria.repository;

import ipn.escom.defensoria.subdefensoria.entity.ExpedienteInvestigacion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ExpedienteInvestigacionRepository extends JpaRepository<ExpedienteInvestigacion, Long> {

    Optional<ExpedienteInvestigacion> findByFolio(String folio);

    Optional<ExpedienteInvestigacion> findByQuejaId(Long quejaId);

    boolean existsByQuejaId(Long quejaId);

    List<ExpedienteInvestigacion> findByEstatusOrderByFechaAdmisionAsc(String estatus);

    List<ExpedienteInvestigacion> findByEstatusInOrderByFechaAdmisionAsc(List<String> estatus);

    List<ExpedienteInvestigacion> findByUnidadAcademicaOrderByFechaAdmisionAsc(String unidadAcademica);
}
