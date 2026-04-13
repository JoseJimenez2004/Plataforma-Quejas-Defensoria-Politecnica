package ipn.escom.defensoria.usuarios.service;

import ipn.escom.defensoria.usuarios.dto.LoginDTO;
import ipn.escom.defensoria.usuarios.dto.usuarioDTO;
import ipn.escom.defensoria.usuarios.entity.usuarioEntity;
import ipn.escom.defensoria.usuarios.repository.usuarioRepository;
import ipn.escom.defensoria.usuarios.repository.NotificacionRepository;
import ipn.escom.defensoria.usuarios.entity.NotificacionEntity;
import java.util.UUID;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
    
    @Autowired
    private NotificacionRepository notificacionRepository; 
    
    @Autowired
    private JavaMailSender mailSender;

    public String registrarUsuario(usuarioDTO dto) {
        // Validamos correo y boleta existentes 
        if(repository.findByCorreo(dto.getCorreo()).isPresent()) 
            return "Error: El correo ya está registrado";
        if(dto.getBoleta() != null && repository.existsByBoleta(dto.getBoleta()))
            return "Error: El número de Boleta o Empleado ya tiene una cuenta activa.";
        usuarioEntity usuario = new usuarioEntity();
        usuario.setCorreo(dto.getCorreo());
        usuario.setNombre(dto.getNombre());
        usuario.setPrimerApellido(dto.getPrimerApellido());
        usuario.setSegundoApellido(dto.getSegundoApellido());
        usuario.setBoleta(dto.getBoleta());
        // Si el DTO trae un folio individual (del proceso de queja), lo agregamos a la lista
        if (dto.getFolioVinculado() != null && !dto.getFolioVinculado().isEmpty()) {
            usuario.getFoliosVinculados().put(dto.getFolioVinculado(), usuario.getBoleta());
        }
        // Encriptación
        usuario.setPassword(passwordEncoder.encode(dto.getPassword()));
        repository.save(usuario);
        return "Cuenta registrada exitosamente";
    }
    
    public void agregarNuevoFolio(String correo, String nuevoFolio) {
        usuarioEntity usuario = repository.findByCorreo(correo)
            .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        usuario.getFoliosVinculados().put(nuevoFolio, usuario.getBoleta());
        repository.save(usuario);
    }
    
    public void crearNotificacion(String correo, String titulo, String mensaje) {
        usuarioEntity usuario = repository.findByCorreo(correo)
            .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        NotificacionEntity nota = new NotificacionEntity();
        nota.setTitulo(titulo);
        nota.setMensaje(mensaje);
        nota.setBoleta(usuario.getBoleta()); 
        nota.setFecha(LocalDateTime.now());
        nota.setUsuario(usuario);
        usuario.getNotificaciones().add(nota);
        repository.save(usuario);
    }
        
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

    public Map<String, Object> login(LoginDTO loginDTO) {
        usuarioEntity usuario = repository.findByCorreo(loginDTO.getCorreo()).orElse(null);
        Map<String, Object> respuesta = new HashMap<>();
        // Verificamos existencia y validación de contraseña encriptada
        if (usuario != null && passwordEncoder.matches(loginDTO.getPassword(), usuario.getPassword())) {
            respuesta.put("status", "success");
            respuesta.put("nombre", usuario.getNombre());
            respuesta.put("apellidos", usuario.getPrimerApellido() + " " + usuario.getSegundoApellido());
            respuesta.put("correo", usuario.getCorreo());

            respuesta.put("folios", usuario.getFoliosVinculados().keySet());

            long pendientes = usuario.getNotificaciones().stream()
                                     .filter(n -> !n.isLeida()).count();
            respuesta.put("notificacionesPendientes", pendientes);

            return respuesta;
        }
        respuesta.put("status", "error");
        respuesta.put("mensaje", "El correo o la contraseña son incorrectos.");
        return respuesta;
    }

    public List<NotificacionEntity> obtenerNotificacionesUsuario(String correo) {
        usuarioEntity usuario = repository.findByCorreo(correo)
            .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
        return usuario.getNotificaciones();
    }

    public void marcarNotificacionComoLeida(Long id) {
        notificacionRepository.findById(id).ifPresent(n -> {
            n.setLeida(true);
            notificacionRepository.save(n);
        });
    }

    public Map<String, String> obtenerPerfilParaPrecarga(String correo) {
        // Buscamos al usuario en la base de datos
        usuarioEntity usuario = repository.findByCorreo(correo)
            .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
        // Preparamos el mapa con los datos para el formulario de Folio
        Map<String, String> datos = new HashMap<>();
        datos.put("nombre", usuario.getNombre());
        datos.put("primerApellido", usuario.getPrimerApellido());
        datos.put("segundoApellido", usuario.getSegundoApellido());
        datos.put("correo", usuario.getCorreo());
        datos.put("boleta", usuario.getBoleta());
        return datos;
    }
}