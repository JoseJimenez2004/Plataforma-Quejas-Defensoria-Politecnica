package ipn.escom.defensoria.quejoso.service;

import ipn.escom.defensoria.quejoso.dto.ActivacionCuentaDTO;
import ipn.escom.defensoria.quejoso.dto.LoginDTO;
import ipn.escom.defensoria.quejoso.entity.Queja;
import ipn.escom.defensoria.quejoso.entity.Usuario;
import ipn.escom.defensoria.quejoso.repository.QuejaRepository;
import ipn.escom.defensoria.quejoso.repository.UsuarioRepository;
import ipn.escom.defensoria.quejoso.dto.UsuarioPerfilDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;


@Service
public class UsuarioService {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private QuejaRepository quejaRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JavaMailSender mailSender;

    /**
     * MQ-05 y MQ-09: Activa la cuenta del usuario.
     * Si el usuario no existe, lo crea basándose en los datos de la queja.
     */
    @Transactional
    public void activarCuenta(ActivacionCuentaDTO dto) {
        // 1. Validar formato de contraseña (8 chars, 1 Mayus, 1 Núm)
        validarPassword(dto.getPassword(), dto.getConfirmarPassword());

        // 2. Buscar la queja por folio
        Queja queja = quejaRepository.findByFolio(dto.getNumeroFolio())
                .orElseThrow(() -> new RuntimeException("El folio no existe."));

        // 3. Validación de seguridad MQ-09:
        // Se usa correoQuejoso de la entidad Queja para evitar NullPointerException si no hay usuario aún.
        if (!queja.getCorreoQuejoso().equalsIgnoreCase(dto.getCorreo())) {
            throw new RuntimeException("El correo proporcionado no coincide con el registro del folio.");
        }

        Usuario usuario;

        // 4. Determinar si recuperamos o creamos al usuario
        if (queja.getQuejoso() != null) {
            usuario = queja.getQuejoso();
            if (usuario.isActivo()) {
                throw new RuntimeException("Esta cuenta ya se encuentra activa.");
            }
        } else {
            // Escenario: Queja llegó sin cuenta vinculada. Creamos el usuario "Just-in-Time".
            usuario = new Usuario();
            // Usamos la boleta/id institucional como nombre inicial
            usuario.setNombre(queja.getIdentificacionInstitucional());
            usuario.setCorreoInstitucional(queja.getCorreoQuejoso());
            usuario.setBoleta(queja.getIdentificacionInstitucional());
            usuario.setUnidadAcademica(queja.getUnidaddondeOcurrio());

            // --- Lógica Perfil MQ-20 ---
            // Sincronizamos los datos del tutor de la queja al perfil de contacto del usuario
            if (queja.getTutor() != null) {
                usuario.setNombreTutor(queja.getTutor().getNombre() + " " +
                        queja.getTutor().getPrimerApellido());
                usuario.setParentescoTutor(queja.getTutor().getParentesco());
                usuario.setTelefonoTutor(queja.getTutor().getTelefono());
            }
        }

        // 5. Cifrar contraseña y activar cuenta
        String passwordCifrada = passwordEncoder.encode(dto.getPassword());
        usuario.setPassword(passwordCifrada);
        usuario.setActivo(true);

        // 6. Persistir cambios
        Usuario usuarioGuardado = usuarioRepository.save(usuario);

        // 7. Vincular la queja al nuevo usuario si la relación era nula
        if (queja.getQuejoso() == null) {
            queja.setQuejoso(usuarioGuardado);
            quejaRepository.save(queja);
        }
    }

    /**
     * Valida que las contraseñas coincidan y cumplan con la política de seguridad.
     */
    private void validarPassword(String p1, String p2) {
        if (p1 == null || !p1.equals(p2)) {
            throw new RuntimeException("Las contraseñas no coinciden.");
        }

        // Regex: Mínimo 8 caracteres, al menos una mayúscula y un número
        String regex = "^(?=.*[A-Z])(?=.*\\d).{8,}$";
        if (!p1.matches(regex)) {
            throw new RuntimeException("La contraseña no cumple con los requisitos mínimos (8 caracteres, una mayúscula y un número).");
        }
    }

    // Añade estos métodos a UsuarioService.java

    public UsuarioPerfilDTO obtenerPerfil(Long usuarioId) {
        Usuario u = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        return UsuarioPerfilDTO.builder()
                .nombre(u.getNombre())
                .correoInstitucional(u.getCorreoInstitucional())
                .boleta(u.getBoleta())
                .unidadAcademica(u.getUnidadAcademica())
                .correoPersonal(u.getCorreoPersonal())
                .telefonoCelular(u.getTelefonoCelular())
                .nombreTutor(u.getNombreTutor())
                .parentescoTutor(u.getParentescoTutor())
                .telefonoTutor(u.getTelefonoTutor())
                .build();
    }

    @Transactional
    public void actualizarPerfil(Long usuarioId, UsuarioPerfilDTO dto) {
        Usuario u = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        u.setCorreoPersonal(dto.getCorreoPersonal());
        u.setTelefonoCelular(dto.getTelefonoCelular());
        u.setNombreTutor(dto.getNombreTutor());
        u.setParentescoTutor(dto.getParentescoTutor());
        u.setTelefonoTutor(dto.getTelefonoTutor());

        usuarioRepository.save(u);
    }

    // Añade esto a UsuarioService.java

    public UsuarioPerfilDTO login(LoginDTO dto) {
        // 1. Buscar al usuario por correo
        Usuario usuario = usuarioRepository.findByCorreoInstitucional(dto.getCorreo())
                .orElseThrow(() -> new RuntimeException("Credenciales incorrectas o cuenta no activa"));

        // 2. Verificar que la cuenta esté activa (que tenga password)
        if (usuario.getPassword() == null) {
            throw new RuntimeException("La cuenta no ha sido activada. Usa tu folio primero.");
        }

        // 3. Comparar contraseñas usando el PasswordEncoder
        if (!passwordEncoder.matches(dto.getPassword(), usuario.getPassword())) {
            throw new RuntimeException("Credenciales incorrectas");
        }

        // 4. Retornar datos básicos del perfil (En el futuro, aquí generamos el JWT)
        return obtenerPerfil(usuario.getId());
    }

    public void generarCodigoRecuperacion(String correo) {
        Usuario usuario = usuarioRepository.findByCorreoInstitucional(correo)
                .orElseThrow(() -> new RuntimeException("Correo no registrado"));

        // 1. Generar código de 6 dígitos aleatorio
        String codigo = String.format("%06d", new java.util.Random().nextInt(999999));

        // 2. Guardar el código (puedes cifrarlo con passwordEncoder si quieres máxima seguridad)
        usuario.setCodigoRecuperacion(passwordEncoder.encode(codigo));

        // 3. Definir expiración (10 minutos a partir de ahora)
        usuario.setFechaExpiracionCodigo(LocalDateTime.now().plusMinutes(10));

        usuarioRepository.save(usuario);

        // 4. SIMULACIÓN: Imprimir en consola (Aquí es donde enviarías el correo real)
        enviarCorreoCodigo(correo, codigo); // Llamamos al nuevo método

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

        // 1. Validar que no haya expirado
        if (usuario.getFechaExpiracionCodigo().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("El código ha expirado. Solicita uno nuevo.");
        }

        // 2. Validar que el código coincida
        if (!passwordEncoder.matches(codigoRecuperado, usuario.getCodigoRecuperacion())) {
            throw new RuntimeException("El código es incorrecto.");
        }

        // 3. Si todo está bien, cambiar contraseña y limpiar el código usado
        usuario.setPassword(passwordEncoder.encode(nuevaPassword));
        usuario.setCodigoRecuperacion(null);
        usuario.setFechaExpiracionCodigo(null);

        usuarioRepository.save(usuario);
    }

}