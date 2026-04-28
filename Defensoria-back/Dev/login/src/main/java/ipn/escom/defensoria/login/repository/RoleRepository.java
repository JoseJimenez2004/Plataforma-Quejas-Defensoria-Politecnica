package ipn.escom.defensoria.login.repository;

import ipn.escom.defensoria.login.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface RoleRepository extends JpaRepository<Role, Integer> {
    // Para buscar el rol por su nombre
    Optional<Role> findByName(String name);
}