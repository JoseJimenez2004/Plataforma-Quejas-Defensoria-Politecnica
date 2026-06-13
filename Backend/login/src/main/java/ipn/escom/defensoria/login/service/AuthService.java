package ipn.escom.defensoria.login.service;

import ipn.escom.defensoria.login.entity.User;
import ipn.escom.defensoria.login.entity.Role;
import ipn.escom.defensoria.login.dto.RegistroRequest;
import ipn.escom.defensoria.login.dto.LoginRequest;  
import ipn.escom.defensoria.login.dto.AuthResponse; 
import ipn.escom.defensoria.login.repository.UserRepository;
import ipn.escom.defensoria.login.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    // --- MÉTODO DE REGISTRO ---
    public String registrarUsuario(RegistroRequest request) {
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new RuntimeException("Error: El correo ya está registrado.");
        }

        Role rol = roleRepository.findByName(request.getNombreRol())
                .orElseThrow(() -> new RuntimeException("Error: Rol no encontrado."));

        User nuevoUsuario = new User();
        nuevoUsuario.setNombre(request.getNombre());
        nuevoUsuario.setApellidoPaterno(request.getApellidoPaterno());
        nuevoUsuario.setApellidoMaterno(request.getApellidoMaterno()); // Agregado
        nuevoUsuario.setEmail(request.getEmail());
        nuevoUsuario.setRol(rol);
        nuevoUsuario.setPassword(passwordEncoder.encode(request.getPassword()));

        userRepository.save(nuevoUsuario);
        return "Usuario registrado exitosamente con rol: " + request.getNombreRol();
    }

    // --- MÉTODO DE LOGIN (NUEVO) ---
    public AuthResponse login(LoginRequest request) {
        // 1. Buscar al usuario por correo
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("Error: Usuario no encontrado."));

        // 2. Comparar contraseña plana con el Hash de la BD
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new RuntimeException("Error: Contraseña incorrecta.");
        }

        // 3. Generar respuesta (De momento con un token estático hasta que pongas JWT)
        return AuthResponse.builder()
                .token("TOKEN_PROVISIONAL_PENDIENTE_JWT")
                .email(user.getEmail())
                .nombre(user.getNombre())
                .rol(user.getRol().getName())
                .build();
    }
}