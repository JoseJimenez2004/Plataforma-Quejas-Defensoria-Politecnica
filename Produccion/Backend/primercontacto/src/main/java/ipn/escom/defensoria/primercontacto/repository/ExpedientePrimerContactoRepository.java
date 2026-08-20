package ipn.escom.defensoria.primercontacto.repository;
import ipn.escom.defensoria.primercontacto.entity.ExpedientePrimerContacto;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface ExpedientePrimerContactoRepository
        extends JpaRepository<ExpedientePrimerContacto, Long> {

    Optional<ExpedientePrimerContacto> findByFolio(String folio);

    Optional<ExpedientePrimerContacto> findByFolioOrigen(String folioOrigen);

    boolean existsByFolio(String folio);

    boolean existsByFolioOrigen(String folioOrigen);
}