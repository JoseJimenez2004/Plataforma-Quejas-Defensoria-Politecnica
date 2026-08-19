package ipn.escom.defensoria.subdefensoria.repository;

import ipn.escom.defensoria.subdefensoria.entity.ExpedienteInvestigacion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ExpedienteInvestigacionRepository
        extends JpaRepository<ExpedienteInvestigacion, Long> {

    /*
     * Folio propio de Subdefensoría.
     */
    Optional<ExpedienteInvestigacion> findByFolio(
            String folio
    );

    /*
     * Folio recibido desde Primer Contacto.
     */
    Optional<ExpedienteInvestigacion> findByFolioOrigen(
            String folioOrigen
    );

    boolean existsByFolioOrigen(
            String folioOrigen
    );

    List<ExpedienteInvestigacion>
    findByEstatusOrderByFechaAdmisionAsc(
            String estatus
    );

    List<ExpedienteInvestigacion>
    findByEstatusInOrderByFechaAdmisionAsc(
            List<String> estatus
    );

    List<ExpedienteInvestigacion>
    findByUnidadAcademicaOrderByFechaAdmisionAsc(
            String unidadAcademica
    );
}