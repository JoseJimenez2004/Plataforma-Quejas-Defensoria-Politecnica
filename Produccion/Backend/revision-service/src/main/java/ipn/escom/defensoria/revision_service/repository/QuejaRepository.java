package ipn.escom.defensoria.revision_service.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import ipn.escom.defensoria.revision_service.entity.Queja;

@Repository
public interface QuejaRepository extends JpaRepository<Queja, Long> {

    Optional<Queja> findByNumeroFolio(String numeroFolio);

    List<Queja> findByEstatusOrderByFechaCreacionAsc(String estatus);

    List<Queja> findByEstatusInOrderByFechaCreacionAsc(List<String> estatus);

    long countByEstatus(String estatus);

    long countByEstatusAndFechaTurnadoBetween(String estatus, LocalDateTime desde, LocalDateTime hasta);

    /** Antecedentes: otras quejas de la misma persona, excluyendo la queja actual. Se busca
     * por número de identificación (boleta/empleado) cuando existe; si no (ej. queja pública
     * sin ese dato), se cae a buscar por correo institucional -- ver RevisionQuejaService. */
    List<Queja> findByNumeroIdentificacionQuejosoAndIdNotOrderByFechaCreacionDesc(
            String numeroIdentificacionQuejoso, Long idExcluido);

    List<Queja> findByCorreoInstitucionalAndIdNotOrderByFechaCreacionDesc(String correoInstitucional, Long idExcluido);

    /** Historial: quejas ya procesadas (rechazadas o turnadas), más recientes primero. */
    List<Queja> findByEstatusInOrderByFechaCreacionDesc(List<String> estatus);
}
