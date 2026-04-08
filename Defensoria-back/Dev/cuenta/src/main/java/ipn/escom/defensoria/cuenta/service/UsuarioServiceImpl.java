package ipn.escom.defensoria.cuenta.service;

import ipn.escom.defensoria.cuenta.dto.LoginRequest;
import ipn.escom.defensoria.cuenta.dto.RegistroRequest;
import ipn.escom.defensoria.cuenta.dto.UsuarioResponse;
import ipn.escom.defensoria.cuenta.entity.UsuarioEntity;
import ipn.escom.defensoria.cuenta.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class UsuarioServiceImpl implements IUsuarioService {

    @Autowired
    private UsuarioRepository repository;

    @Override
    public UsuarioResponse registrar(RegistroRequest registroDTO) {
        // 1. Validar si el correo ya existe
        if (repository.existsByCorreo(registroDTO.getCorreo())) {
            throw new RuntimeException("El correo ya está registrado en el sistema.");
        }

        // 2. Validar si la boleta/empleado ya existe
        if (repository.existsByBoletaEmpleado(registroDTO.getBoletaEmpleado())) {
            throw new RuntimeException("La boleta o número de empleado ya existe.");
        }

        // 3. Validar que las contraseñas coincidan
        if (!registroDTO.getPassword().equals(registroDTO.getConfirmarPassword())) {
            throw new RuntimeException("Las contraseñas no coinciden.");
        }

        // 4. Mapear DTO a Entity
        UsuarioEntity nuevoUsuario = UsuarioEntity.builder()
                .nombre(registroDTO.getNombre())
                .apellidoPaterno(registroDTO.getApellidoPaterno())
                .apellidoMaterno(registroDTO.getApellidoMaterno())
                .edad(registroDTO.getEdad())
                .boletaEmpleado(registroDTO.getBoletaEmpleado())
                .unidadAcademica(registroDTO.getUnidadAcademica())
                .correo(registroDTO.getCorreo())
                .password(registroDTO.getPassword()) // Nota: En producción usa BCrypt
                .rol("ROLE_USER")
                .build();

        // 5. Guardar en H2
        UsuarioEntity guardado = repository.save(nuevoUsuario);

        // 6. Retornar Response seguro (sin password)
        return mapearARespuesta(guardado, "Registro exitoso. ¡Bienvenido a la Defensoría!");
    }

    @Override
    public UsuarioResponse login(LoginRequest loginDTO) {
        UsuarioEntity usuario = repository.findByCorreo(loginDTO.getCorreo())
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado."));

        if (!usuario.getPassword().equals(loginDTO.getPassword())) {
            throw new RuntimeException("Contraseña incorrecta.");
        }

        return mapearARespuesta(usuario, "Inicio de sesión correcto.");
    }

    @Override
    public void recuperarPassword(String correo) {
        if (!repository.existsByCorreo(correo)) {
            throw new RuntimeException("No existe una cuenta asociada a este correo.");
        }
        // Aquí iría la lógica para generar un token y enviar el correo real
        System.out.println("LOG: Enviando enlace de recuperación a " + correo);
    }

    // Método privado para convertir la Entity en el DTO de respuesta
    private UsuarioResponse mapearARespuesta(UsuarioEntity entidad, String mensaje) {
        return UsuarioResponse.builder()
                .id(entidad.getId())
                .nombre(entidad.getNombre())
                .apellidoPaterno(entidad.getApellidoPaterno())
                .boletaEmpleado(entidad.getBoletaEmpleado())
                .unidadAcademica(entidad.getUnidadAcademica())
                .correo(entidad.getCorreo())
                .mensaje(mensaje)
                .build();
    }
}