package ipn.escom.defensoria.historico_service.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import ipn.escom.defensoria.historico_service.entity.QuejaHistorica;

@Repository
public interface QuejaHistoricaRepository extends JpaRepository<QuejaHistorica, Long> {

    Optional<QuejaHistorica> findByFolio(String folio);

    boolean existsByFolio(String folio);

    /**
     * La consulta que justifica que esta base exista: antecedentes de una persona por su
     * identificación, tanto si fue quien se quejó como si fue el denunciado.
     */
    @Query("""
            SELECT q FROM QuejaHistorica q
            WHERE (q.quejosoNumeroIdentificacion = :numero
                   AND (:tipo = '' OR q.quejosoTipoIdentificacion = :tipo))
               OR (q.denunciadoNumeroIdentificacion = :numero
                   AND (:tipo = '' OR q.denunciadoTipoIdentificacion = :tipo))
            ORDER BY q.fechaPresentacionOriginal DESC NULLS LAST
            """)
    List<QuejaHistorica> buscarAntecedentesPorIdentificacion(
            @Param("tipo") String tipoIdentificacion,
            @Param("numero") String numeroIdentificacion);

    /**
     * Respaldo para registros viejos que no traían boleta ni número de empleado: se busca
     * por nombre, comparando en minúsculas. Los acentos SÍ importan en la comparación --
     * los nombres se guardan tal como se capturaron, sin normalizar.
     */
    @Query("""
            SELECT q FROM QuejaHistorica q
            WHERE lower(q.quejosoApellido1) = lower(:apellido1)
              AND lower(q.quejosoNombre)    = lower(:nombre)
            ORDER BY q.fechaPresentacionOriginal DESC NULLS LAST
            """)
    List<QuejaHistorica> buscarAntecedentesPorNombre(
            @Param("nombre") String nombre,
            @Param("apellido1") String apellido1);

    /** Listado paginado para el panel de Recepción. Los filtros vacíos se mandan como
     *  cadena vacía, nunca como null: comparar un parámetro contra NULL en JPQL hace que
     *  Postgres no pueda inferir su tipo. */
    @Query("""
            SELECT q FROM QuejaHistorica q
            WHERE (:texto = '' OR lower(q.folio) LIKE lower(concat('%', :texto, '%'))
                   OR lower(q.quejosoApellido1) LIKE lower(concat('%', :texto, '%'))
                   OR lower(q.quejosoNombre) LIKE lower(concat('%', :texto, '%')))
              AND (:unidad = '' OR q.unidadAcademicaClave = :unidad)
            ORDER BY q.fechaPresentacionOriginal DESC NULLS LAST
            """)
    Page<QuejaHistorica> buscar(@Param("texto") String texto,
                                @Param("unidad") String unidadAcademicaClave,
                                Pageable pageable);

    /** Para generar el consecutivo de los folios SF- (expedientes que no traían folio). */
    long countByFolioGeneradoTrue();
}
