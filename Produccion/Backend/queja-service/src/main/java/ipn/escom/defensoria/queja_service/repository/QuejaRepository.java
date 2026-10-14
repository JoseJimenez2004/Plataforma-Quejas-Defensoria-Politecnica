package ipn.escom.defensoria.queja_service.repository;


import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ipn.escom.defensoria.queja_service.entity.Queja;

@Repository
public interface QuejaRepository extends JpaRepository<Queja, Long> {
    Optional<Queja> findByNumeroFolioAndCorreoInstitucional(String numeroFolio, String correoInstitucional);

    // "Mis Quejas" del panel autenticado — todas las quejas asociadas al correo del usuario
    // logueado, sin importar si las registró como autenticado o (antes de tener cuenta) como
    // invitado con ese mismo correo. Orden más reciente primero.
    List<Queja> findByCorreoInstitucionalOrderByFechaCreacionDesc(String correoInstitucional);

    /** Última queja del mismo correo que ya trae los datos del quejoso (la del formulario
     *  público con la que nació su cuenta, o una posterior que los heredó). */
    Optional<Queja> findFirstByCorreoInstitucionalIgnoreCaseAndNombreQuejosoIsNotNullOrderByFechaCreacionDesc(
            String correoInstitucional);
}