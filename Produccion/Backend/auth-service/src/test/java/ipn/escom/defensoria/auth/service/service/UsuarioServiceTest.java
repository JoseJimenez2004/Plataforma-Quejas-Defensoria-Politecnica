package ipn.escom.defensoria.auth.service.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import ipn.escom.defensoria.auth.service.client.QuejasClient;
import ipn.escom.defensoria.auth.service.entity.Usuario;
import ipn.escom.defensoria.auth.service.model.ActivacionCuentaModel;
import ipn.escom.defensoria.auth.service.model.LoginModel;
import ipn.escom.defensoria.auth.service.model.PerfilUpdateRequest;
import ipn.escom.defensoria.auth.service.model.QuejaResumenModel;
import ipn.escom.defensoria.auth.service.repository.UsuarioRepository;

/** Pruebas unitarias de {@link UsuarioService}: CU-Q03, CU-Q04, CU-Q05 y CU-Q10. */
@ExtendWith(MockitoExtension.class)
@DisplayName("UsuarioService — cuentas del quejoso")
class UsuarioServiceTest {

    private static final String CORREO = "quejoso@ipn.mx";
    private static final String HASH = "$2a$hash";
    private static final String PASSWORD_VALIDA = "Segura123";
    private static final String CODIGO = "123456";
    private static final int MAX_INTENTOS = 5;
    private static final String FOLIO = "FOL-ABCD1234";

    @Mock
    private UsuarioRepository repositorio;
    @Mock
    private PasswordEncoder encoder;
    @Mock
    private JavaMailSender correo;
    @Mock
    private QuejasClient quejasClient;

    @InjectMocks
    private UsuarioService servicio;

    private Usuario usuario;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(servicio, "remitenteCorreo", "defensoria@ipn.mx");
        usuario = new Usuario();
        usuario.setId(1L);
        usuario.setCorreoInstitucional(CORREO);
        usuario.setPassword(HASH);
        usuario.setActivo(true);
        lenient().when(repositorio.findByCorreoInstitucional(CORREO)).thenReturn(Optional.of(usuario));
    }

    @Nested
    @DisplayName("Inicio de sesión (CU-Q04)")
    class Login {

        private LoginModel login(String password) {
            LoginModel m = new LoginModel();
            m.setCorreo(CORREO);
            m.setPassword(password);
            return m;
        }

        @Test
        void validarLogin_credencialesCorrectas_regresaUsuario() {
            when(encoder.matches(PASSWORD_VALIDA, HASH)).thenReturn(true);
            assertSame(usuario, servicio.validarLogin(login(PASSWORD_VALIDA)), "Regresa al usuario autenticado");
        }

        @Test
        void validarLogin_passwordIncorrecta_lanzaExcepcion() {
            when(encoder.matches("Otra1234", HASH)).thenReturn(false);
            RuntimeException ex = assertThrows(RuntimeException.class, () -> servicio.validarLogin(login("Otra1234")));
            assertEquals("Credenciales incorrectas", ex.getMessage(), "Contraseña incorrecta");
        }

        @Test
        void validarLogin_cuentaSinActivar_lanzaExcepcion() {
            usuario.setPassword(null);
            RuntimeException ex = assertThrows(RuntimeException.class, () -> servicio.validarLogin(login(PASSWORD_VALIDA)));
            assertTrue(ex.getMessage().contains("no ha sido activada"), "Cuenta sin contraseña");
        }

        @Test
        void validarLogin_correoInexistente_lanzaMensajeGenerico() {
            when(repositorio.findByCorreoInstitucional("nadie@ipn.mx")).thenReturn(Optional.empty());
            LoginModel m = login(PASSWORD_VALIDA);
            m.setCorreo("nadie@ipn.mx");
            RuntimeException ex = assertThrows(RuntimeException.class, () -> servicio.validarLogin(m));
            assertEquals("Credenciales incorrectas o cuenta no activa", ex.getMessage(), "No revela si el correo existe");
        }
    }

    @Nested
    @DisplayName("Recuperación de contraseña (CU-Q05)")
    class Recuperacion {

        @Test
        void generarCodigo_correoSinCuenta_noRevelaNiEnviaNada() {
            when(repositorio.findByCorreoInstitucional("nadie@ipn.mx")).thenReturn(Optional.empty());
            assertDoesNotThrow(() -> servicio.generarCodigoRecuperacion("nadie@ipn.mx"), "Responde igual (OWASP A07)");
            verify(correo, never()).send(any(SimpleMailMessage.class));
        }

        @Test
        void generarCodigo_guardaCodigoCifradoDeSeisDigitosYExpiracion() {
            when(encoder.encode(anyString())).thenReturn("codigo-cifrado");

            servicio.generarCodigoRecuperacion(CORREO);

            ArgumentCaptor<String> codigo = ArgumentCaptor.forClass(String.class);
            verify(encoder).encode(codigo.capture());
            assertTrue(codigo.getValue().matches("\\d{6}"), "Código de 6 dígitos");
            assertEquals("codigo-cifrado", usuario.getCodigoRecuperacion(), "Se guarda cifrado, no en claro");
            assertTrue(usuario.getFechaExpiracionCodigo().isAfter(LocalDateTime.now().plusMinutes(9)), "Vence en 10 minutos");
            assertEquals(0, usuario.getIntentosCodigo(), "Se reinician los intentos");
        }

        @Test
        void generarCodigo_correoFalla_noPropagaError() {
            when(encoder.encode(anyString())).thenReturn("x");
            doThrow(new MailSendException("SMTP caído")).when(correo).send(any(SimpleMailMessage.class));
            assertDoesNotThrow(() -> servicio.generarCodigoRecuperacion(CORREO), "El fallo de correo solo se registra");
        }

        private void conCodigoVigente(int intentos) {
            usuario.setCodigoRecuperacion("codigo-cifrado");
            usuario.setFechaExpiracionCodigo(LocalDateTime.now().plusMinutes(5));
            usuario.setIntentosCodigo(intentos);
        }

        @Test
        void cambiarPassword_codigoCorrecto_cambiaYLimpiaCodigo() {
            conCodigoVigente(0);
            when(encoder.matches(CODIGO, "codigo-cifrado")).thenReturn(true);
            when(encoder.encode(PASSWORD_VALIDA)).thenReturn("nuevo-hash");

            servicio.validarCodigoYCambiarPassword(CORREO, CODIGO, PASSWORD_VALIDA);

            assertEquals("nuevo-hash", usuario.getPassword(), "Contraseña nueva cifrada");
            assertNull(usuario.getCodigoRecuperacion(), "El código no se puede reutilizar");
            verify(correo).send(any(SimpleMailMessage.class));
        }

        @Test
        void cambiarPassword_sinSolicitudActiva_lanzaExcepcion() {
            RuntimeException ex = assertThrows(RuntimeException.class,
                    () -> servicio.validarCodigoYCambiarPassword(CORREO, CODIGO, PASSWORD_VALIDA));
            assertTrue(ex.getMessage().startsWith("No hay una solicitud de recuperación activa"), "Sin código previo");
        }

        @Test
        void cambiarPassword_codigoExpirado_loInvalida() {
            conCodigoVigente(0);
            usuario.setFechaExpiracionCodigo(LocalDateTime.now().minusSeconds(1));
            RuntimeException ex = assertThrows(RuntimeException.class,
                    () -> servicio.validarCodigoYCambiarPassword(CORREO, CODIGO, PASSWORD_VALIDA));
            assertEquals("El código ha expirado. Solicita uno nuevo.", ex.getMessage(), "Código vencido");
            assertNull(usuario.getCodigoRecuperacion(), "Se borra el código vencido");
        }

        @Test
        void cambiarPassword_codigoIncorrecto_cuentaIntentoRestante() {
            conCodigoVigente(0);
            when(encoder.matches("000000", "codigo-cifrado")).thenReturn(false);
            RuntimeException ex = assertThrows(RuntimeException.class,
                    () -> servicio.validarCodigoYCambiarPassword(CORREO, "000000", PASSWORD_VALIDA));
            assertEquals("El código es incorrecto. Te quedan 4 intentos.", ex.getMessage(), "Quedan 4 de 5");
            assertEquals(1, usuario.getIntentosCodigo(), "Se suma un intento");
        }

        @Test
        void cambiarPassword_cuartoIntentoFallido_quedaUnIntento() {
            conCodigoVigente(MAX_INTENTOS - 1);
            when(encoder.matches("000000", "codigo-cifrado")).thenReturn(false);
            RuntimeException ex = assertThrows(RuntimeException.class,
                    () -> servicio.validarCodigoYCambiarPassword(CORREO, "000000", PASSWORD_VALIDA));
            assertTrue(ex.getMessage().contains("Te quedan 0 intentos"), "Último intento consumido");
        }

        @Test
        void cambiarPassword_intentosAgotados_invalidaElCodigo() {
            conCodigoVigente(MAX_INTENTOS);
            RuntimeException ex = assertThrows(RuntimeException.class,
                    () -> servicio.validarCodigoYCambiarPassword(CORREO, CODIGO, PASSWORD_VALIDA));
            assertTrue(ex.getMessage().startsWith("Demasiados intentos fallidos"), "Fuerza bruta bloqueada");
            assertNull(usuario.getCodigoRecuperacion(), "Código invalidado");
            verify(encoder, never()).matches(anyString(), anyString());
        }
    }

    @Nested
    @DisplayName("Activación de cuenta (CU-Q03) y reglas de contraseña")
    class Activacion {

        private ActivacionCuentaModel activacion(String password, String confirmar) {
            ActivacionCuentaModel m = new ActivacionCuentaModel();
            m.setNumeroFolio(FOLIO);
            m.setCorreo("nuevo@ipn.mx");
            m.setPassword(password);
            m.setConfirmarPassword(confirmar);
            return m;
        }

        @ParameterizedTest
        @ValueSource(strings = {"Corta1A", "sinmayuscula1", "SinNumeroAqui", "        "})
        void activar_passwordQueNoCumple_lanzaExcepcion(String password) {
            assertThrows(RuntimeException.class, () -> servicio.activarCuenta(activacion(password, password)),
                    "Rechaza: '" + password + "'");
            verify(quejasClient, never()).validarFolioYCorreo(any());
        }

        @Test
        void activar_passwordDeOchoConSimbolos_esValida() {
            when(quejasClient.validarFolioYCorreo(Map.of("folio", FOLIO, "correo", "nuevo@ipn.mx"))).thenReturn(true);
            when(repositorio.findByCorreoInstitucional("nuevo@ipn.mx")).thenReturn(Optional.empty());
            when(quejasClient.obtenerPorFolio(FOLIO, "nuevo@ipn.mx")).thenThrow(new RuntimeException("timeout"));
            assertDoesNotThrow(() -> servicio.activarCuenta(activacion("Abcdef1!", "Abcdef1!")),
                    "8 caracteres con mayúscula, número y símbolo es válida");
        }

        @Test
        void activar_passwordsDistintas_lanzaExcepcion() {
            RuntimeException ex = assertThrows(RuntimeException.class,
                    () -> servicio.activarCuenta(activacion(PASSWORD_VALIDA, "Segura124")));
            assertEquals("Las contraseñas no coinciden.", ex.getMessage(), "Confirmación distinta");
        }

        @Test
        void activar_folioYCorreoNoCoinciden_lanzaExcepcion() {
            when(quejasClient.validarFolioYCorreo(any())).thenReturn(false);
            RuntimeException ex = assertThrows(RuntimeException.class,
                    () -> servicio.activarCuenta(activacion(PASSWORD_VALIDA, PASSWORD_VALIDA)));
            assertTrue(ex.getMessage().startsWith("El folio no existe"), "Folio y correo deben coincidir");
            verify(repositorio, never()).save(any());
        }

        @Test
        void activar_cuentaNueva_tomaNombreYBoletaDeLaQueja() {
            when(quejasClient.validarFolioYCorreo(any())).thenReturn(true);
            when(repositorio.findByCorreoInstitucional("nuevo@ipn.mx")).thenReturn(Optional.empty());
            QuejaResumenModel resumen = new QuejaResumenModel();
            resumen.setNombreQuejoso("Ana");
            resumen.setApellido1Quejoso("López");
            resumen.setNumeroIdentificacionQuejoso("2021630001");
            when(quejasClient.obtenerPorFolio(FOLIO, "nuevo@ipn.mx")).thenReturn(resumen);
            when(encoder.encode(PASSWORD_VALIDA)).thenReturn(HASH);

            servicio.activarCuenta(activacion(PASSWORD_VALIDA, PASSWORD_VALIDA));

            ArgumentCaptor<Usuario> guardado = ArgumentCaptor.forClass(Usuario.class);
            verify(repositorio).save(guardado.capture());
            assertEquals("Ana López", guardado.getValue().getNombre(), "Nombre real, no placeholder");
            assertEquals("2021630001", guardado.getValue().getBoleta(), "Boleta real");
            assertTrue(guardado.getValue().isActivo(), "Cuenta activa");
        }

        @Test
        void activar_cuentaYaActiva_lanzaExcepcion() {
            when(quejasClient.validarFolioYCorreo(any())).thenReturn(true);
            when(repositorio.findByCorreoInstitucional("nuevo@ipn.mx")).thenReturn(Optional.of(usuario));
            RuntimeException ex = assertThrows(RuntimeException.class,
                    () -> servicio.activarCuenta(activacion(PASSWORD_VALIDA, PASSWORD_VALIDA)));
            assertEquals("Esta cuenta ya se encuentra activa.", ex.getMessage(), "No se reactiva");
        }
    }

    @Nested
    @DisplayName("Perfil (CU-Q10)")
    class Perfil {

        @Test
        void actualizarPerfil_telefonoDeDiezDigitos_seGuarda() {
            PerfilUpdateRequest datos = new PerfilUpdateRequest();
            datos.setTelefonoCelular("5512345678");
            datos.setDomicilio(" ");
            servicio.actualizarPerfil(CORREO, datos);
            assertEquals("5512345678", usuario.getTelefonoCelular(), "Teléfono guardado");
            assertNull(usuario.getDomicilio(), "Un campo en blanco se borra");
        }

        @ParameterizedTest
        @ValueSource(strings = {"551234567", "55123456789", "55-1234-567"})
        void actualizarPerfil_telefonoInvalido_lanzaExcepcion(String telefono) {
            PerfilUpdateRequest datos = new PerfilUpdateRequest();
            datos.setTelefonoCelular(telefono);
            RuntimeException ex = assertThrows(RuntimeException.class, () -> servicio.actualizarPerfil(CORREO, datos));
            assertEquals("El teléfono celular debe tener exactamente 10 dígitos.", ex.getMessage(), "Rechaza " + telefono);
        }

        @Test
        void obtenerPerfil_usuarioInexistente_lanzaExcepcion() {
            when(repositorio.findByCorreoInstitucional("x@ipn.mx")).thenReturn(Optional.empty());
            assertThrows(RuntimeException.class, () -> servicio.obtenerPerfil("x@ipn.mx"), "Usuario inexistente");
        }

        @Test
        void obtenerPerfil_regresaDatosDeLaCuenta() {
            usuario.setBoleta("2021630001");
            assertNotNull(servicio.obtenerPerfil(CORREO), "Perfil encontrado");
        }
    }

    @Nested
    @DisplayName("existeCuenta")
    class ExisteCuenta {

        @Test
        void existeCuenta_correoVacio_false() {
            assertFalse(servicio.existeCuenta("  "), "Correo vacío");
        }

        @Test
        void existeCuenta_correoEnMayusculas_buscaTambienEnMinusculas() {
            when(repositorio.findByCorreoInstitucional("Quejoso@IPN.mx")).thenReturn(Optional.empty());
            assertTrue(servicio.existeCuenta(" Quejoso@IPN.mx "), "Encuentra la cuenta en minúsculas");
        }
    }
}
