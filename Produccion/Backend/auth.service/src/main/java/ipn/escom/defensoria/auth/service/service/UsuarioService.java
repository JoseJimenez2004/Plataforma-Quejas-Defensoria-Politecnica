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
import ipn.escom.defensoria.auth.service.model.PerfilModel;
import ipn.escom.defensoria.auth.service.model.PerfilUpdateRequest;
import ipn.escom.defensoria.auth.service.model.QuejaResumenModel;

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

        // Confirmación de que la contraseña SÍ cambió -- antes solo se mandaba el código, no
        // había ningún aviso una vez que el cambio se completaba. Sirve también como alerta de
        // seguridad si alguien más restableció la contraseña sin que el dueño lo pidiera.
        try {
            enviarCorreoConfirmacionCambio(correo);
        } catch (Exception ex) {
            // La contraseña ya quedó cambiada -- el correo es informativo, no crítico.
        }
    }

    private void enviarCorreoConfirmacionCambio(String destinatario) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom("jair100flo@gmail.com");
        message.setTo(destinatario);
        message.setSubject("Tu contraseña fue restablecida - Defensoría");
        message.setText("Hola,\n\n"
                + "Tu contraseña en la Plataforma de Seguimiento de la Defensoría de los Derechos "
                + "Politécnicos fue restablecida correctamente el "
                + LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")) + ".\n\n"
                + "Si tú no realizaste este cambio, contacta de inmediato a la Defensoría de los "
                + "Derechos Politécnicos para proteger tu cuenta.\n\n"
                + "Defensoría de los Derechos Politécnicos.");
        mailSender.send(message);
    }

    // Requisitos: mínimo 8 caracteres, al menos una mayúscula y al menos un número. El resto
    // de los caracteres ("." al final del lookahead) puede ser cualquier cosa: minúsculas,
    // acentos y símbolos/caracteres especiales (!@#$%^&*, etc.) están permitidos explícitamente,
    // no hay una lista blanca que los excluya.
    private void validarPassword(String p1) {
        if (p1 == null || p1.isBlank()) {
            throw new RuntimeException("La contraseña no puede estar vacía.");
        }
        String regex = "^(?=.*[A-Z])(?=.*\\d).{8,}$";
        if (!p1.matches(regex)) {
            throw new RuntimeException(
                    "La contraseña no cumple con los requisitos mínimos: 8 caracteres, una mayúscula y un número. Puedes usar también símbolos y caracteres especiales.");
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
            usuario.setBoleta("PENDIENTE");
            usuario.setNombre("Ciudadano Defensoría");

            // Traemos el nombre y número de identificación reales de la queja que ya validamos
            // arriba, en vez de dejar los placeholders. Si por algo falla esta llamada (la
            // queja es de un flujo autenticado viejo sin esos campos, error de red, etc.) no
            // tumbamos la activación de la cuenta — nos quedamos con el placeholder.
            try {
                QuejaResumenModel resumen = quejasClient.obtenerPorFolio(model.getNumeroFolio(), model.getCorreo());
                String nombreCompleto = construirNombre(resumen);
                if (nombreCompleto != null) {
                    usuario.setNombre(nombreCompleto);
                }
                if (resumen.getNumeroIdentificacionQuejoso() != null && !resumen.getNumeroIdentificacionQuejoso().isBlank()) {
                    usuario.setBoleta(resumen.getNumeroIdentificacionQuejoso());
                }
            } catch (RuntimeException ex) {
                // No es crítico: la cuenta se activa igual, solo con datos genéricos.
            }
        }

        usuario.setPassword(passwordEncoder.encode(model.getPassword()));
        usuario.setActivo(true);

        usuarioRepository.save(usuario);

        // Correo de bienvenida -- se manda AQUÍ (cuando se crea/activa la cuenta), no cuando
        // se presenta una queja. Nunca debe tumbar la activación si el correo falla.
        try {
            enviarCorreoBienvenida(usuario.getCorreoInstitucional(), usuario.getNombre());
        } catch (Exception ex) {
            // La cuenta ya quedó activa -- el correo es informativo, no crítico.
        }
    }

    private void enviarCorreoBienvenida(String destinatario, String nombre) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom("jair100flo@gmail.com");
        message.setTo(destinatario);
        message.setSubject("Bienvenido a la Defensoría de los Derechos Politécnicos");
        message.setText("Hola " + nombre + ",\n\n"
                + "Tu cuenta en la Plataforma de Seguimiento de la Defensoría de los Derechos "
                + "Politécnicos ha sido creada y activada correctamente.\n\n"
                + "Ya puedes iniciar sesión con tu correo institucional y la contraseña que "
                + "acabas de definir para dar seguimiento a tus quejas, recibir notificaciones "
                + "y actualizar tus datos de contacto.\n\n"
                + "Si tú no solicitaste esta cuenta, ignora este mensaje.\n\n"
                + "Defensoría de los Derechos Politécnicos.");
        mailSender.send(message);
    }

    /** Perfil completo del usuario autenticado (GET /api/auth/me) -- antes el frontend solo
     * tenía nombre+correo (lo que regresaba /login), sin boleta/unidad académica/domicilio. */
    public PerfilModel obtenerPerfil(String correo) {
        Usuario usuario = usuarioRepository.findByCorreoInstitucional(correo)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado."));
        return new PerfilModel(
                usuario.getNombre(),
                usuario.getCorreoInstitucional(),
                usuario.getBoleta(),
                usuario.getUnidadAcademica(),
                usuario.getCorreoPersonal(),
                usuario.getTelefonoCelular(),
                usuario.getDomicilio());
    }

    /** Actualiza los campos editables del perfil (correo personal, teléfono, unidad
     * académica, domicilio) -- nombre/correo institucional/boleta son de solo lectura. */
    public PerfilModel actualizarPerfil(String correo, PerfilUpdateRequest datos) {
        Usuario usuario = usuarioRepository.findByCorreoInstitucional(correo)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado."));

        if (datos.getCorreoPersonal() != null) {
            usuario.setCorreoPersonal(datos.getCorreoPersonal().isBlank() ? null : datos.getCorreoPersonal());
        }
        if (datos.getTelefonoCelular() != null) {
            if (!datos.getTelefonoCelular().isBlank() && !datos.getTelefonoCelular().matches("\\d{10}")) {
                throw new RuntimeException("El teléfono celular debe tener exactamente 10 dígitos.");
            }
            usuario.setTelefonoCelular(datos.getTelefonoCelular().isBlank() ? null : datos.getTelefonoCelular());
        }
        if (datos.getUnidadAcademica() != null) {
            usuario.setUnidadAcademica(datos.getUnidadAcademica().isBlank() ? null : datos.getUnidadAcademica());
        }
        if (datos.getDomicilio() != null) {
            usuario.setDomicilio(datos.getDomicilio().isBlank() ? null : datos.getDomicilio());
        }

        usuarioRepository.save(usuario);
        return obtenerPerfil(correo);
    }

    private String construirNombre(QuejaResumenModel resumen) {
        if (resumen.getNombreQuejoso() == null || resumen.getNombreQuejoso().isBlank()) {
            return null;
        }
        StringBuilder nombre = new StringBuilder(resumen.getNombreQuejoso());
        if (resumen.getApellidoPaternoQuejoso() != null && !resumen.getApellidoPaternoQuejoso().isBlank()) {
            nombre.append(' ').append(resumen.getApellidoPaternoQuejoso());
        }
        if (resumen.getApellidoMaternoQuejoso() != null && !resumen.getApellidoMaternoQuejoso().isBlank()) {
            nombre.append(' ').append(resumen.getApellidoMaternoQuejoso());
        }
        return nombre.toString();
    }
}