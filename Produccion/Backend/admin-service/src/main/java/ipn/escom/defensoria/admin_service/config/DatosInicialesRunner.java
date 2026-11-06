package ipn.escom.defensoria.admin_service.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import ipn.escom.defensoria.admin_service.entity.PersonalAdministrativo;
import ipn.escom.defensoria.admin_service.entity.RolStaff;
import ipn.escom.defensoria.admin_service.repository.PersonalAdministrativoRepository;

/** Si la tabla de personal administrativo está completamente vacía (primer arranque del
 * servicio), crea la cuenta inicial de la Defensora (DEFENSOR) para poder entrar por primera vez al
 * panel. La contraseña temporal se imprime UNA sola vez en el log de arranque -- después de
 * activarla, cámbiala desde "Editar Perfil". */
@Component
public class DatosInicialesRunner implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DatosInicialesRunner.class);
    private static final String PASSWORD_INICIAL = "Defensoria2026!";

    private final PersonalAdministrativoRepository repository;
    private final PasswordEncoder passwordEncoder;

    public DatosInicialesRunner(PersonalAdministrativoRepository repository, PasswordEncoder passwordEncoder) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        if (repository.count() > 0) {
            return;
        }

        PersonalAdministrativo admin = new PersonalAdministrativo();
        admin.setNombreCompleto("Defensora (cuenta inicial)");
        admin.setNumeroEmpleado("DEF-0001");
        admin.setCorreoInstitucional("defensora@ipn.mx");
        admin.setRol(RolStaff.DEFENSOR);
        admin.setPassword(passwordEncoder.encode(PASSWORD_INICIAL));
        admin.setCuentaTemporal(true);
        admin.setForzarCambioPassword(true);
        admin.setActivo(true);
        repository.save(admin);

        log.warn("========================================================================");
        log.warn(" Cuenta inicial de la Defensora creada (correo: defensora@ipn.mx).");
        log.warn(" Contraseña temporal: {}", PASSWORD_INICIAL);
        log.warn(" Cámbiala en cuanto inicies sesión por primera vez.");
        log.warn("========================================================================");
    }
}
