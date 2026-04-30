package ipn.escom.defensoria.login.repository;

import ipn.escom.defensoria.login.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Integer> {
    // Clave para buscar por correo
    Optional<User> findByEmail(String email);
}