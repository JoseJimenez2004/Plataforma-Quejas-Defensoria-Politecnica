package ipn.escom.defensoria.administracion.repository;

import ipn.escom.defensoria.administracion.entity.UsuarioAdmin; // Asegúrate de que este import coincida con tu paquete
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UsuarioAdminRepository extends JpaRepository<UsuarioAdmin, Long> {

    
    // SELECT * FROM defensoria.usuarios_admin WHERE correo_institucional = ?
    Optional<UsuarioAdmin> findByCorreoInstitucional(String correoInstitucional);
    
}