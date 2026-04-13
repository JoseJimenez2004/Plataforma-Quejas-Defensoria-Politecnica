package ipn.escom.defensoria.recepcionista.repository;

import ipn.escom.defensoria.recepcionista.entity.DocumentoEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repositorio JPA para la entidad DocumentoEntity.
 *
 * Se usa en la pantalla de Validación de Requisitos para obtener
 * los archivos adjuntos de una queja y mostrar su lista al recepcionista.
 */
@Repository
public interface DocumentoRepository extends JpaRepository<DocumentoEntity, Long> {

    /**
     * Obtiene todos los documentos adjuntos de una queja específica.
     * Spring genera el SQL automáticamente desde el nombre del método:
     * SELECT * FROM documentos WHERE queja_id = ?
     *
     * Parámetro: quejaId (Long) — ID de la queja.
     * Retorna: lista de documentos (vacía si la queja no tiene adjuntos).
     */
    List<DocumentoEntity> findByQuejaId(Long quejaId);
}
