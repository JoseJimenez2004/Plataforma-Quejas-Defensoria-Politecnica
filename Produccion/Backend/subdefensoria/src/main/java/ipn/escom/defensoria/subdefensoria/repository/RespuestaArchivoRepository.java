package ipn.escom.defensoria.subdefensoria.repository;

import ipn.escom.defensoria.subdefensoria.entity.RespuestaArchivo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface RespuestaArchivoRepository extends JpaRepository<RespuestaArchivo, Long> {

    Optional<RespuestaArchivo> findByRespuestaId(Long respuestaId);

    boolean existsByRespuestaId(Long respuestaId);

    /** Nombre del archivo de cada respuesta, sin cargar el contenido: [respuestaId, nombre]. */
    @Query("select a.respuestaId, a.nombreArchivo from RespuestaArchivo a where a.respuestaId in :respuestas")
    List<Object[]> nombresPorRespuesta(List<Long> respuestas);
}
