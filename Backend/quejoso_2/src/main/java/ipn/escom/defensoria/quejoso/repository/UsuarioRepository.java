package ipn.escom.defensoria.quejoso.repository;

import ipn.escom.defensoria.quejoso.entity.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
    // Para vincular automáticamente la queja si el usuario ya existe
    Optional<Usuario> findByBoleta(String boleta);
    Optional<Usuario> findByCorreoInstitucional(String correo);
}