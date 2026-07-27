package ipn.escom.defensoria.admin_service.service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import ipn.escom.defensoria.admin_service.entity.PersonalAdministrativo;
import ipn.escom.defensoria.admin_service.model.PersonalCreadoResponseModel;
import ipn.escom.defensoria.admin_service.model.PersonalRequest;
import ipn.escom.defensoria.admin_service.model.PersonalResumenModel;
import ipn.escom.defensoria.admin_service.repository.PersonalAdministrativoRepository;

@Service
public class PersonalAdministrativoService {

    private static final String CARACTERES_PASSWORD =
            "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnpqrstuvwxyz23456789!@#$%";
    private final SecureRandom random = new SecureRandom();

    @Autowired
    private PersonalAdministrativoRepository repository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    public PersonalAdministrativo validarLogin(String correo, String password) {
        PersonalAdministrativo personal = repository.findByCorreoInstitucional(correo)
                .orElseThrow(() -> new RuntimeException("Credenciales incorrectas."));

        if (!personal.isActivo()) {
            throw new RuntimeException("Esta cuenta está desactivada. Contacta a un administrador.");
        }
        if (!passwordEncoder.matches(password, personal.getPassword())) {
            throw new RuntimeException("Credenciales incorrectas.");
        }

        personal.setUltimoLogin(LocalDateTime.now());
        repository.save(personal);
        return personal;
    }

    public List<PersonalResumenModel> listar() {
        return repository.findAllByOrderByFechaCreacionDesc().stream()
                .map(this::aResumen)
                .toList();
    }

    public PersonalCreadoResponseModel crear(PersonalRequest datos) {
        if (esVacio(datos.getNombreCompleto()) || esVacio(datos.getNumeroEmpleado())
                || esVacio(datos.getCorreoInstitucional()) || datos.getRol() == null) {
            throw new RuntimeException("Completa nombre, número de empleado, correo y rol.");
        }
        if (repository.findByCorreoInstitucional(datos.getCorreoInstitucional()).isPresent()) {
            throw new RuntimeException("Ya existe una cuenta con ese correo institucional.");
        }

        String passwordTemporal = esVacio(datos.getPasswordTemporal())
                ? generarPasswordTemporal()
                : datos.getPasswordTemporal();

        PersonalAdministrativo personal = new PersonalAdministrativo();
        personal.setNombreCompleto(datos.getNombreCompleto());
        personal.setNumeroEmpleado(datos.getNumeroEmpleado());
        personal.setCorreoInstitucional(datos.getCorreoInstitucional());
        personal.setRol(datos.getRol());
        personal.setPassword(passwordEncoder.encode(passwordTemporal));
        personal.setCuentaTemporal(true);
        personal.setForzarCambioPassword(true);
        personal.setActivo(true);

        PersonalAdministrativo guardado = repository.save(personal);
        // La contraseña en claro solo viaja en ESTA respuesta -- nunca se vuelve a poder
        // consultar después (en la BD solo queda el hash).
        return new PersonalCreadoResponseModel(
                guardado.getId(), guardado.getNombreCompleto(), guardado.getCorreoInstitucional(),
                passwordTemporal);
    }

    public PersonalAdministrativo editar(Long id, PersonalRequest datos) {
        PersonalAdministrativo personal = obtener(id);

        if (!esVacio(datos.getNombreCompleto())) {
            personal.setNombreCompleto(datos.getNombreCompleto());
        }
        if (!esVacio(datos.getCorreoInstitucional())) {
            personal.setCorreoInstitucional(datos.getCorreoInstitucional());
        }
        if (datos.getRol() != null) {
            personal.setRol(datos.getRol());
        }
        if (datos.isRestablecerPassword()) {
            personal.setForzarCambioPassword(true);
        }
        if (datos.isDesactivarTemporalmente()) {
            personal.setActivo(false);
        }

        return repository.save(personal);
    }

    public String resetearPassword(Long id) {
        PersonalAdministrativo personal = obtener(id);
        String nueva = generarPasswordTemporal();
        personal.setPassword(passwordEncoder.encode(nueva));
        personal.setCuentaTemporal(true);
        personal.setForzarCambioPassword(true);
        repository.save(personal);
        return nueva;
    }

    public void darDeBaja(Long id) {
        PersonalAdministrativo personal = obtener(id);
        personal.setActivo(false);
        repository.save(personal);
    }

    public void reactivar(Long id) {
        PersonalAdministrativo personal = obtener(id);
        personal.setActivo(true);
        repository.save(personal);
    }

    public long contarActivos() {
        return repository.countByActivoTrue();
    }

    /** Autoservicio: cualquier cuenta (sin importar el rol) puede cambiar SU PROPIA
     * contraseña siempre que confirme la actual. A diferencia de resetearPassword() (que
     * genera una temporal y exige cambiarla de nuevo), aquí la contraseña queda definitiva. */
    public void cambiarMiPassword(String correo, String passwordActual, String passwordNueva) {
        PersonalAdministrativo personal = repository.findByCorreoInstitucional(correo)
                .orElseThrow(() -> new RuntimeException("No se encontró la cuenta."));

        if (esVacio(passwordActual) || !passwordEncoder.matches(passwordActual, personal.getPassword())) {
            throw new RuntimeException("La contraseña actual no es correcta.");
        }
        validarPassword(passwordNueva);

        personal.setPassword(passwordEncoder.encode(passwordNueva));
        personal.setCuentaTemporal(false);
        personal.setForzarCambioPassword(false);
        repository.save(personal);
    }

    private void validarPassword(String password) {
        if (esVacio(password)) {
            throw new RuntimeException("La nueva contraseña no puede estar vacía.");
        }
        if (!password.matches("^(?=.*[A-Z])(?=.*\\d).{8,}$")) {
            throw new RuntimeException(
                    "La nueva contraseña debe tener al menos 8 caracteres, una mayúscula y un número. "
                            + "Puedes usar también símbolos y caracteres especiales.");
        }
    }

    private PersonalAdministrativo obtener(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new RuntimeException("No se encontró ese personal administrativo."));
    }

    private PersonalResumenModel aResumen(PersonalAdministrativo p) {
        return new PersonalResumenModel(
                p.getId(), p.getNombreCompleto(), p.getNumeroEmpleado(),
                p.getCorreoInstitucional(), p.getRol(), p.isActivo(), p.isCuentaTemporal());
    }

    private String generarPasswordTemporal() {
        StringBuilder sb = new StringBuilder("Tmp-");
        for (int i = 0; i < 8; i++) {
            sb.append(CARACTERES_PASSWORD.charAt(random.nextInt(CARACTERES_PASSWORD.length())));
        }
        return sb.toString();
    }

    private boolean esVacio(String valor) {
        return valor == null || valor.isBlank();
    }
}
