package ipn.escom.defensoria.recepcionista.repository;

import ipn.escom.defensoria.recepcionista.entity.QuejaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repositorio JPA para la entidad QuejaEntity.
 *
 * Al extender JpaRepository, Spring genera automáticamente los métodos
 * básicos: findById, save, findAll, delete, etc.
 *
 * Los métodos adicionales usan dos mecanismos:
 *
 * 1. Métodos derivados (findByEstatus_Nombre):
 *    Spring lee el nombre del método y genera el SQL automáticamente.
 *    "Estatus_Nombre" significa: navega a la relación "estatus" y filtra por su campo "nombre".
 *    Genera: SELECT * FROM quejas q JOIN cat_estatus_queja e ON q.estatus_id = e.id WHERE e.nombre = ?
 *
 * 2. @Query con JPQL (findByQuejosoId):
 *    Consulta escrita manualmente en lenguaje JPQL (Java Persistence Query Language).
 *    Similar a SQL pero usa nombres de clases y atributos Java, no nombres de tablas/columnas.
 */
@Repository
public interface QuejaRepository extends JpaRepository<QuejaEntity, Long> {

    /**
     * Dashboard: obtiene todas las quejas con un estatus específico.
     * Se usa con "RECIBIDA" para mostrar la bandeja de quejas pendientes.
     * Parámetro: nombre del estatus (String exacto del catálogo).
     */
    List<QuejaEntity> findByEstatus_Nombre(String nombreEstatus);

    /**
     * Búsqueda de antecedentes: obtiene todas las quejas de un quejoso específico,
     * ordenadas de más reciente a más antigua.
     * Parámetro: id del perfil del quejoso (Long).
     */
    @Query("SELECT q FROM QuejaEntity q WHERE q.quejoso.id = :quejosoId ORDER BY q.fechaRecepcion DESC")
    List<QuejaEntity> findByQuejosoId(Long quejosoId);
}
