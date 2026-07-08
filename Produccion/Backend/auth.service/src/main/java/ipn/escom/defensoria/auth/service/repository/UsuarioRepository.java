package ipn.escom.defensoria.auth.service.repository;

import ipn.escom.defensoria.auth.service.entity.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
    
    Optional<Usuario> findByBoleta(String boleta);
    
    Optional<Usuario> findByCorreoInstitucional(String correo);
}