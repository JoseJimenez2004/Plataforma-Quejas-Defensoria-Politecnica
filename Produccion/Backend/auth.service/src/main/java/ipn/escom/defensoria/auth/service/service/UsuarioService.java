package ipn.escom.defensoria.auth.service.service;

import java.time.LocalDateTime;
import java.util.Random;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import ipn.escom.defensoria.auth.service.entity.Usuario;
import ipn.escom.defensoria.auth.service.repository.UsuarioRepository;
import ipn.escom.defensoria.auth.service.model.LoginModel; 
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import ipn.escom.defensoria.auth.service.client.QuejasClient;
import ipn.escom.defensoria.auth.service.model.ActivacionCuentaModel;

@Service
public class UsuarioService {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    public Usuario validarLogin(LoginModel model) {
        // 1. Buscar al usuario por correo institucional
        Usuario usuario = usuarioRepository.findByCorreoInstitucional(model.getCorreo())
                .orElseThrow(() -> new RuntimeException("Credenciales incorrectas o cuenta no activa"));

        // 2. Verificar que la cuenta esté activa (que tenga password)
        if (usuario.getPassword() == null) {
            throw new RuntimeException("La cuenta no ha sido activada. Usa tu folio primero.");
        }

        // 3. Comparar contraseñas usando el PasswordEncoder
        if (!passwordEncoder.matches(model.getPassword(), usuario.getPassword())) {
            throw new RuntimeException("Credenciales incorrectas");
        }

        // 4. Si todo es correcto, retornamos la entidad Usuario
        return usuario;
    }

    @Autowired
    private JavaMailSender mailSender;

    public void generarCodigoRecuperacion(String correo) {
        Usuario usuario = usuarioRepository.findByCorreoInstitucional(correo)
                .orElseThrow(() -> new RuntimeException("Correo no registrado"));

        // Generar código de 6 dígitos aleatorio
        String codigo = String.format("%06d", new Random().nextInt(999999));
        
        // Guardar el código cifrado
        usuario.setCodigoRecuperacion(passwordEncoder.encode(codigo));
        usuario.setFechaExpiracionCodigo(LocalDateTime.now().plusMinutes(10));

        usuarioRepository.save(usuario);
        enviarCorreoCodigo(correo, codigo);
    }

    private void enviarCorreoCodigo(String destinatario, String codigo) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom("jair100flo@gmail.com");
        message.setTo(destinatario);
        message.setSubject("Código de Recuperación - Defensoría");
        message.setText("Hola,\n\nTu código de verificación para restablecer tu contraseña es: "
                + codigo + "\n\nEste código expirará en 10 minutos.");
        
        mailSender.send(message);
    }

    public void validarCodigoYCambiarPassword(String correo, String codigoRecuperado, String nuevaPassword) {
        Usuario usuario = usuarioRepository.findByCorreoInstitucional(correo)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        if (usuario.getFechaExpiracionCodigo().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("El código ha expirado. Solicita uno nuevo.");
        }

        if (!passwordEncoder.matches(codigoRecuperado, usuario.getCodigoRecuperacion())) {
            throw new RuntimeException("El código es incorrecto.");
        }

        // Cifrar la nueva contraseña según las reglas
        validarPassword(nuevaPassword);
        usuario.setPassword(passwordEncoder.encode(nuevaPassword));
        usuario.setCodigoRecuperacion(null);
        usuario.setFechaExpiracionCodigo(null);

        usuarioRepository.save(usuario);
    }

    private void validarPassword(String p1) {
        String regex = "^(?=.*[A-Z])(?=.*\\d).{8,}$";
        if (!p1.matches(regex)) {
            throw new RuntimeException("La contraseña no cumple con los requisitos mínimos (8 caracteres, una mayúscula y un número).");
        }
    }

    @Autowired
    private QuejasClient quejasClient;

    public void activarCuenta(ActivacionCuentaModel model) {
        // 1. Validar contraseñas
        if (model.getPassword() == null || !model.getPassword().equals(model.getConfirmarPassword())) {
            throw new RuntimeException("Las contraseñas no coinciden.");
        }
        validarPassword(model.getPassword()); // El método de la regex (8 chars, 1 mayús, 1 num)

        // 1. Empaquetamos los datos en un mapa idéntico a lo que espera el controlador
        java.util.Map<String, String> datosParaValidar = java.util.Map.of(
            "folio", model.getNumeroFolio(),
            "correo", model.getCorreo()
        );

        // 2. Comunicación HTTP limpia con el quejas-service enviando el JSON en el cuerpo de la petición
        boolean esValido = quejasClient.validarFolioYCorreo(datosParaValidar);
        
        if (!esValido) {
            throw new RuntimeException("El folio no existe o el correo no coincide con el registro.");
        }

        // 3. Lógica de activación "Just-in-Time"
        Usuario usuario = usuarioRepository.findByCorreoInstitucional(model.getCorreo())
                .orElse(new Usuario());

        if (usuario.getId() != null && usuario.isActivo()) {
            throw new RuntimeException("Esta cuenta ya se encuentra activa.");
        }

        if (usuario.getId() == null) {
            usuario.setCorreoInstitucional(model.getCorreo());
            usuario.setBoleta("PENDIENTE"); // Se podría traer del quejas-service luego
            usuario.setNombre("Ciudadano Defensoría"); // Se podría traer del quejas-service luego
        }

        usuario.setPassword(passwordEncoder.encode(model.getPassword()));
        usuario.setActivo(true);

        usuarioRepository.save(usuario);
    }
}