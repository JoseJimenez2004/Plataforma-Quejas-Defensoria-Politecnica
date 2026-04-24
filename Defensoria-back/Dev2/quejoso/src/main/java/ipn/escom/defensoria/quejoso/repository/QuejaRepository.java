package ipn.escom.defensoria.quejoso.repository;

import ipn.escom.defensoria.quejoso.entity.Queja;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Pageable; // Asegúrate de importar esto
import java.util.Optional;
import java.util.List;

public interface QuejaRepository extends JpaRepository<Queja, Long> {
    // Busca el último folio del año actual para incrementar el consecutivo
    @Query("SELECT q.folio FROM Queja q WHERE q.folio LIKE :prefijo ORDER BY q.folio DESC LIMIT 1")
    String findLastFolioByYear(@Param("prefijo") String prefijo);

    // Para el seguimiento por folio (MQ-06)
    Optional<Queja> findByFolio(String folio);

    // Dentro de QuejaRepository.java
    long countByQuejosoId(Long usuarioId);

    @Query("SELECT COUNT(q) FROM Queja q WHERE q.quejoso.id = :usuarioId AND q.estatus NOT IN ('FINALIZADA', 'REMISION')")
    long countEnProcesoByUsuarioId(@Param("usuarioId") Long usuarioId);

    @Query("SELECT COUNT(q) FROM Queja q WHERE q.quejoso.id = :usuarioId AND q.estatus IN ('FINALIZADA', 'REMISION')")
    long countFinalizadasByUsuarioId(@Param("usuarioId") Long usuarioId);

    @Query("SELECT q.folio FROM Queja q WHERE q.folio LIKE :prefijo ORDER BY q.folio DESC")
    List<String> findLastFolioByYear(@Param("prefijo") String prefijo, Pageable pageable);

    // Para MQ-15: Historial con filtros
    List<Queja> findByQuejosoIdOrderByFechaRegistroDesc(Long usuarioId);
}