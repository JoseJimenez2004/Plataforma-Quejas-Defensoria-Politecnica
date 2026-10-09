package ipn.escom.defensoria.denunciado_service.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import ipn.escom.defensoria.denunciado_service.entity.RespuestaDenunciado;

public interface RespuestaDenunciadoRepository extends JpaRepository<RespuestaDenunciado, Long> {

    List<RespuestaDenunciado> findByFolioQuejaOrderByFechaRegistroDesc(String folioQueja);

    boolean existsByFolioRespuesta(String folioRespuesta);
}
