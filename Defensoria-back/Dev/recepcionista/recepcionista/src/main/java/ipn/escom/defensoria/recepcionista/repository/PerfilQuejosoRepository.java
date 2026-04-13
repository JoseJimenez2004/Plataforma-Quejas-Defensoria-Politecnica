package ipn.escom.defensoria.recepcionista.repository;

import ipn.escom.defensoria.recepcionista.entity.PerfilQuejosoEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repositorio JPA para la entidad PerfilQuejosoEntity.
 *
 * Se usa principalmente para la pantalla de Búsqueda de Antecedentes,
 * donde la recepcionista busca si un quejoso ya tiene quejas previas
 * antes de proceder con la canalización.
 */
@Repository
public interface PerfilQuejosoRepository extends JpaRepository<PerfilQuejosoEntity, Long> {

    /**
     * Búsqueda de antecedentes por nombre o apellidos del quejoso.
     *
     * Busca coincidencias parciales (LIKE '%texto%') sin importar mayúsculas (LOWER).
     * Ejemplo: buscar "juan" encuentra "Juan Pérez", "Juana López", etc.
     *
     * La consulta busca en tres combinaciones:
     *   1. nombre + primer_apellido juntos (ej. "Juan Pérez")
     *   2. solo el nombre (ej. "Juan")
     *   3. solo el primer apellido (ej. "Pérez")
     *
     * Parámetro: nombre (String) — texto parcial a buscar.
     * Retorna: lista de perfiles que coincidan (vacía si no hay resultados).
     */
    @Query("SELECT p FROM PerfilQuejosoEntity p WHERE " +
           "LOWER(CONCAT(p.nombre, ' ', p.primerApellido)) LIKE LOWER(CONCAT('%', :nombre, '%')) OR " +
           "LOWER(p.nombre) LIKE LOWER(CONCAT('%', :nombre, '%')) OR " +
           "LOWER(p.primerApellido) LIKE LOWER(CONCAT('%', :nombre, '%'))")
    List<PerfilQuejosoEntity> buscarPorNombre(String nombre);
}
