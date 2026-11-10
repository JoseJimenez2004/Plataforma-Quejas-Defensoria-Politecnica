package ipn.escom.defensoria.primercontacto.repository;
import ipn.escom.defensoria.primercontacto.entity.ExpedientePrimerContacto;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ExpedientePrimerContactoRepository
        extends JpaRepository<ExpedientePrimerContacto, Long> {

    Optional<ExpedientePrimerContacto> findByFolio(String folio);

    Optional<ExpedientePrimerContacto> findByFolioOrigen(String folioOrigen);

    boolean existsByFolio(String folio);

    boolean existsByFolioOrigen(String folioOrigen);

    List<ExpedientePrimerContacto> findByEstatusIn(Collection<String> estatus);

    List<ExpedientePrimerContacto> findByQuejosoCorreoIgnoreCase(String quejosoCorreo);

    /** 2026-11: expedientes que Recepción turnó a este analista. */
    List<ExpedientePrimerContacto> findByAnalistaAsignadoId(Long analistaAsignadoId);
}