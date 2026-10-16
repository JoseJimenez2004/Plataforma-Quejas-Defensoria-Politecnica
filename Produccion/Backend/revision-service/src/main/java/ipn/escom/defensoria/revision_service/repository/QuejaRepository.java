package ipn.escom.defensoria.revision_service.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import ipn.escom.defensoria.revision_service.entity.Queja;

@Repository
public interface QuejaRepository extends JpaRepository<Queja, Long> {

    Optional<Queja> findByNumeroFolio(String numeroFolio);

    List<Queja> findByEstatusOrderByFechaCreacionAsc(String estatus);

    List<Queja> findByEstatusInOrderByFechaCreacionAsc(List<String> estatus);

    long countByEstatus(String estatus);

    long countByEstatusAndFechaTurnadoBetween(String estatus, LocalDateTime desde, LocalDateTime hasta);

    long countByFechaTurnadoBetween(LocalDateTime desde, LocalDateTime hasta);

    /** Antecedentes: otras quejas de la misma persona, excluyendo la queja actual. Se busca
     * por número de identificación (boleta/empleado) cuando existe; si no (ej. queja pública
     * sin ese dato), se cae a buscar por correo institucional -- ver RevisionQuejaService. */
    List<Queja> findByNumeroIdentificacionQuejosoAndIdNotOrderByFechaCreacionDesc(
            String numeroIdentificacionQuejoso, Long idExcluido);

    List<Queja> findByCorreoInstitucionalAndIdNotOrderByFechaCreacionDesc(String correoInstitucional, Long idExcluido);

    /** Historial: quejas ya procesadas (rechazadas o turnadas), más recientes primero. */
    List<Queja> findByEstatusInOrderByFechaCreacionDesc(List<String> estatus);

    /** Quejas que llevan EN_VALIDACION más tiempo del permitido sin que nadie las cierre
     * (rechazo/turnado) -- se liberan solas de vuelta a la bandeja. Ver
     * RevisionQuejaService.liberarRevisionesVencidas(). */
    List<Queja> findByEstatusAndFechaInicioRevisionBefore(String estatus, LocalDateTime limite);

    /**
     * Toma una queja para revisarla SOLO si sigue libre (RECIBIDA o CORREGIDA). Es una sola
     * sentencia UPDATE, así que si dos recepcionistas la abren en el mismo instante la base de
     * datos deja pasar a una sola: la otra recibe 0 filas y se le avisa quién la tomó.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Transactional
    @Query("""
            UPDATE Queja q
               SET q.estatusPrevioRevision = q.estatus,
                   q.estatus = 'EN_VALIDACION',
                   q.revisandoPor = :correo,
                   q.revisandoPorNombre = :nombre,
                   q.fechaInicioRevision = :ahora
             WHERE q.numeroFolio = :folio
               AND q.estatus IN ('RECIBIDA', 'CORREGIDA')
            """)
    int tomarParaRevision(@Param("folio") String folio, @Param("correo") String correo,
                          @Param("nombre") String nombre, @Param("ahora") LocalDateTime ahora);

    /** Reinicia el contador de inactividad mientras la recepcionista sigue trabajando la queja. */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Transactional
    @Query("""
            UPDATE Queja q SET q.fechaInicioRevision = :ahora
             WHERE q.numeroFolio = :folio AND q.estatus = 'EN_VALIDACION'
               AND lower(q.revisandoPor) = lower(:correo)
            """)
    int renovarRevision(@Param("folio") String folio, @Param("correo") String correo,
                        @Param("ahora") LocalDateTime ahora);

    /**
     * Una página de la cola de la Bandeja. El orden (más reciente primero) llega en el Pageable.
     * texto vacío = sin búsqueda; patron es el mismo texto en minúsculas entre '%'.
     */
    @Query("""
            SELECT q FROM Queja q
             WHERE q.estatus IN :estatus
               AND (:soloMias = false OR lower(q.revisandoPor) = lower(:correo))
               AND (:texto = ''
                    OR lower(q.numeroFolio) LIKE :patron
                    OR lower(concat(coalesce(q.nombreQuejoso, ''), ' ', coalesce(q.apellido1Quejoso, ''), ' ',
                                    coalesce(q.apellido2Quejoso, ''))) LIKE :patron
                    OR lower(coalesce(q.unidadAcademicaClave, '')) LIKE :patron)
            """)
    Page<Queja> buscarBandeja(@Param("estatus") List<String> estatus,
                              @Param("soloMias") boolean soloMias,
                              @Param("correo") String correo,
                              @Param("texto") String texto,
                              @Param("patron") String patron,
                              Pageable pageable);

    /** Filtro común del Historial: resultado, "solo las que atendí", rango de recepción y texto. */
    String FILTRO_HISTORIAL = """
             WHERE q.estatus IN :estatus
               AND (:soloMias = false OR lower(q.validadoPor) = lower(:correo))
               AND q.fechaCreacion >= :desde AND q.fechaCreacion < :hasta
               AND (:texto = ''
                    OR lower(q.numeroFolio) LIKE :patron
                    OR lower(concat(coalesce(q.nombreQuejoso, ''), ' ', coalesce(q.apellido1Quejoso, ''), ' ',
                                    coalesce(q.apellido2Quejoso, ''))) LIKE :patron
                    OR lower(coalesce(q.unidadAcademicaClave, '')) LIKE :patron)
            """;

    /** Una página del Historial: la atendida más recientemente primero. */
    @Query(value = "SELECT q FROM Queja q " + FILTRO_HISTORIAL
                   + " ORDER BY coalesce(q.fechaValidacion, q.fechaCreacion) DESC, q.id DESC",
           countQuery = "SELECT count(q) FROM Queja q " + FILTRO_HISTORIAL)
    Page<Queja> buscarHistorial(@Param("estatus") List<String> estatus,
                                @Param("soloMias") boolean soloMias,
                                @Param("correo") String correo,
                                @Param("desde") LocalDateTime desde,
                                @Param("hasta") LocalDateTime hasta,
                                @Param("texto") String texto,
                                @Param("patron") String patron,
                                Pageable pageable);

    /** Datos mínimos (sin descripción ni archivos) de TODO el resultado filtrado, para los totales. */
    @Query("SELECT q.estatus, q.validadoPor, q.fechaCreacion, q.fechaValidacion FROM Queja q " + FILTRO_HISTORIAL)
    List<Object[]> resumenHistorial(@Param("estatus") List<String> estatus,
                                    @Param("soloMias") boolean soloMias,
                                    @Param("correo") String correo,
                                    @Param("desde") LocalDateTime desde,
                                    @Param("hasta") LocalDateTime hasta,
                                    @Param("texto") String texto,
                                    @Param("patron") String patron);

    /** Antecedentes por persona señalada: otras quejas contra el mismo denunciado. */
    List<Queja> findByNombreDenunciadoIgnoreCaseAndApellido1DenunciadoIgnoreCaseAndIdNotOrderByFechaCreacionDesc(
            String nombreDenunciado, String apellido1Denunciado, Long idExcluido);
}
