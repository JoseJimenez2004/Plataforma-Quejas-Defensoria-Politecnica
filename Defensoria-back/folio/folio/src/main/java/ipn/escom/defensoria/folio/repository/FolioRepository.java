package ipn.escom.defensoria.folio.repository;

import ipn.escom.defensoria.folio.entity.FolioEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository

public interface FolioRepository extends JpaRepository<FolioEntity, Long> {
    long countByCodigoFolioStartingWith(String prefijo);
}
