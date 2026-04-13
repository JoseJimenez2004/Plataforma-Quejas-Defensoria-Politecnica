package ipn.escom.defensoria.recepcionista.repository;

import ipn.escom.defensoria.recepcionista.entity.HistorialEstatusEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repositorio JPA para la entidad HistorialEstatusEntity.
 *
 * Solo necesita el método save() heredado de JpaRepository para insertar
 * nuevos registros en historial_estatus_queja cada vez que cambia el estatus
 * de una queja (aceptar, rechazar, canalizar).
 *
 * No se consulta ni elimina desde este microservicio — el historial
 * es de solo escritura aquí. Futuras versiones podrían agregar
 * consultas para mostrar el timeline de una queja.
 */
@Repository
public interface HistorialEstatusRepository extends JpaRepository<HistorialEstatusEntity, Long> {
}
