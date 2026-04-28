package ipn.escom.defensoria.usuarios.repository;

import ipn.escom.defensoria.usuarios.entity.usuarioEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface usuarioRepository extends JpaRepository<usuarioEntity, Long>{
    Optional<usuarioEntity> findByCorreo(String correo);
    boolean existsByCorreo(String correo);
    boolean existsByBoleta(String boleta);
    
    Optional<usuarioEntity> findByResetToken(String token);
}
