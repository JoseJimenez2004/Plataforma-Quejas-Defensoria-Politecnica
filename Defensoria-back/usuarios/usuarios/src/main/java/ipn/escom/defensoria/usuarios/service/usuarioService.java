package ipn.escom.defensoria.usuarios.service;

import ipn.escom.defensoria.usuarios.dto.usuarioDTO;
import ipn.escom.defensoria.usuarios.entity.usuarioEntity;
import ipn.escom.defensoria.usuarios.repository.usuarioRepository;
import java.util.UUID;
import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.mail.javamail.JavaMailSender;

@Service
public class usuarioService {

    @Autowired
    private usuarioRepository repository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    public String registrarUsuario(usuarioDTO dto) {
        // 1. Verificamos si el correo ya existe
        if(repository.findByCorreo(dto.getCorreo()).isPresent()) 
            return "Error: El correo ya está registrado";
        
        if(dto.getBoleta() != null && repository.existsByBoleta(dto.getBoleta()))
            return "Error: El número de Boleta o Empleado ya tiene una cuenta activa.";

        // 2. Creamos la entidad y mapeamos los datos
        usuarioEntity usuario = new usuarioEntity();
        usuario.setCorreo(dto.getCorreo());
        usuario.setNombre(dto.getNombre());
        usuario.setPrimerApellido(dto.getPrimerApellido());
        usuario.setSegundoApellido(dto.getSegundoApellido());
        usuario.setBoleta(dto.getBoleta());
        usuario.setFolioVinculado(dto.getFolioVinculado());

        // 3. ENCRIPTAMOS LA CONTRASEÑA antes de guardar
        String passwordHashed = passwordEncoder.encode(dto.getPassword());
        usuario.setPassword(passwordHashed);

        // 4. Guardamos en la BD
        repository.save(usuario);

        return "Cuenta registrado exitosamente";
    }
    @Autowired
    private JavaMailSender mailSender;
    
    public String solicitarRecuperacion(String correo) {
        Optional<usuarioEntity> usuarioOpt = repository.findByCorreo(correo);
        if (usuarioOpt.isEmpty()) return "Correo no registrado.";

        usuarioEntity usuario = usuarioOpt.get();
        // Generamos un ID único aleatorio
        String token = UUID.randomUUID().toString();
        usuario.setResetToken(token);
        usuario.setTokenExpiration(LocalDateTime.now().plusMinutes(15));

        repository.save(usuario);

        try {
            SimpleMailMessage mensaje = new SimpleMailMessage();
            mensaje.setTo(correo);
            mensaje.setSubject("Recuperación de Contraseña - Defensoría Politécnica");
            mensaje.setText("Hola " + usuario.getNombre() + ",\n\n" +
                           "Has solicitado restablecer tu contraseña. Utiliza el siguiente token:\n" +
                           token + "\n\nEste token expira en 15 minutos.");

            mailSender.send(mensaje);
            return "Se ha enviado un correo con las instrucciones.";
        } catch (Exception e) {
            return "Error al enviar el correo: " + e.getMessage();
        }
    }
    
    public String cambiarPasswordConToken(String token, String nuevoPassword) {
        Optional<usuarioEntity> usuarioOpt = repository.findByResetToken(token);

        if (usuarioOpt.isEmpty() || usuarioOpt.get().getTokenExpiration().isBefore(LocalDateTime.now())) {
            return "Token inválido o expirado.";
        }

        usuarioEntity usuario = usuarioOpt.get();
        usuario.setPassword(passwordEncoder.encode(nuevoPassword));
        usuario.setResetToken(null); // Limpiamos el token para que no se use de nuevo
        usuario.setTokenExpiration(null);

        repository.save(usuario);
        return "Contraseña actualizada correctamente.";
    }
}