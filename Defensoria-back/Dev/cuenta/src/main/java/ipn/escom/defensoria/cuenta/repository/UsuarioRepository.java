package ipn.escom.defensoria.cuenta.repository;

import ipn.escom.defensoria.cuenta.entity.UsuarioEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UsuarioRepository extends JpaRepository<UsuarioEntity, Long> {

    /**
     * Busca un usuario por su correo electrónico.
     * Útil para el proceso de Login y para evitar correos duplicados.
     */
    Optional<UsuarioEntity> findByCorreo(String correo);

    /**
     * Busca un usuario por su boleta o número de empleado.
     * Vital para validar la "Identidad Politécnica" en el registro.
     */
    Optional<UsuarioEntity> findByBoletaEmpleado(String boletaEmpleado);

    /**
     * Verifica si ya existe un usuario con ese correo.
     */
    Boolean existsByCorreo(String correo);

    /**
     * Verifica si ya existe un usuario con esa boleta.
     */
    Boolean existsByBoletaEmpleado(String boletaEmpleado);
}