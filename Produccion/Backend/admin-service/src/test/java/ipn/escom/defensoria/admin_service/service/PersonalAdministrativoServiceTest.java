package ipn.escom.defensoria.admin_service.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
import org.springframework.security.crypto.password.PasswordEncoder;

import ipn.escom.defensoria.admin_service.entity.PersonalAdministrativo;
import ipn.escom.defensoria.admin_service.entity.RolStaff;
import ipn.escom.defensoria.admin_service.model.PersonalCreadoResponseModel;
import ipn.escom.defensoria.admin_service.model.PersonalRequest;
import ipn.escom.defensoria.admin_service.repository.PersonalAdministrativoRepository;

/** Pruebas unitarias de {@link PersonalAdministrativoService}: CU-A01 a CU-A04 y RF-A09. */
@ExtendWith(MockitoExtension.class)
@DisplayName("PersonalAdministrativoService — cuentas del personal")
class PersonalAdministrativoServiceTest {

    private static final Long ID = 10L;
    private static final String CORREO = "recep@ipn.mx";
    private static final String ADMIN = "admin@ipn.mx";
    private static final String HASH = "$2a$hash";
    private static final String PATRON_TEMPORAL = "Tmp-.{8}";

    @Mock
    private PersonalAdministrativoRepository repositorio;
    @Mock
    private PasswordEncoder encoder;

    @InjectMocks
    private PersonalAdministrativoService servicio;

    private PersonalAdministrativo personal;

    @BeforeEach
    void setUp() {
        personal = new PersonalAdministrativo();
        personal.setId(ID);
        personal.setCorreoInstitucional(CORREO);
        personal.setPassword(HASH);
        personal.setRol(RolStaff.RECEPCIONISTA);
        personal.setActivo(true);
        lenient().when(repositorio.findById(ID)).thenReturn(Optional.of(personal));
        lenient().when(repositorio.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    private static PersonalRequest solicitud(RolStaff rol) {
        PersonalRequest d = new PersonalRequest();
        d.setNombreCompleto("Laura Méndez");
        d.setNumeroEmpleado("E-100");
        d.setCorreoInstitucional("laura@ipn.mx");
        d.setRol(rol);
        return d;
    }

    @Nested
    @DisplayName("Login (CU-A01)")
    class Login {

        @Test
        void validarLogin_correcto_registraUltimoAcceso() {
            when(repositorio.findByCorreoInstitucional(CORREO)).thenReturn(Optional.of(personal));
            when(encoder.matches("Clave123", HASH)).thenReturn(true);
            PersonalAdministrativo r = servicio.validarLogin(CORREO, "Clave123");
            assertNotNull(r.getUltimoLogin(), "Se registra el último acceso");
        }

        @Test
        void validarLogin_cuentaDesactivada_lanzaExcepcionSinCompararPassword() {
            personal.setActivo(false);
            when(repositorio.findByCorreoInstitucional(CORREO)).thenReturn(Optional.of(personal));
            RuntimeException ex = assertThrows(RuntimeException.class, () -> servicio.validarLogin(CORREO, "x"));
            assertTrue(ex.getMessage().contains("desactivada"), "Cuenta dada de baja");
            verify(encoder, never()).matches(anyString(), anyString());
        }

        @Test
        void validarLogin_passwordIncorrecta_mismoMensajeQueCorreoInexistente() {
            when(repositorio.findByCorreoInstitucional(CORREO)).thenReturn(Optional.of(personal));
            when(repositorio.findByCorreoInstitucional("nadie@ipn.mx")).thenReturn(Optional.empty());
            when(encoder.matches("mala", HASH)).thenReturn(false);
            String m1 = assertThrows(RuntimeException.class, () -> servicio.validarLogin(CORREO, "mala")).getMessage();
            String m2 = assertThrows(RuntimeException.class, () -> servicio.validarLogin("nadie@ipn.mx", "x")).getMessage();
            assertEquals(m1, m2, "No se revela si el correo existe");
        }
    }

    @Nested
    @DisplayName("Alta y restablecimiento (CU-A02, CU-A03)")
    class Alta {

        @Test
        void crear_sinPasswordTemporal_generaUnaConPrefijoYLaCifra() {
            when(repositorio.findByCorreoInstitucional("laura@ipn.mx")).thenReturn(Optional.empty());
            when(encoder.encode(anyString())).thenReturn(HASH);

            PersonalCreadoResponseModel r = servicio.crear(solicitud(RolStaff.SUBDEFENSOR));

            assertTrue(r.getPasswordTemporal().matches(PATRON_TEMPORAL), "Contraseña temporal Tmp-XXXXXXXX");
            ArgumentCaptor<PersonalAdministrativo> guardado = ArgumentCaptor.forClass(PersonalAdministrativo.class);
            verify(repositorio).save(guardado.capture());
            assertEquals(HASH, guardado.getValue().getPassword(), "En la BD solo queda el hash");
            assertTrue(guardado.getValue().isForzarCambioPassword(), "Debe cambiarla al entrar");
        }

        @Test
        void crear_correoDuplicado_lanzaExcepcion() {
            when(repositorio.findByCorreoInstitucional("laura@ipn.mx")).thenReturn(Optional.of(personal));
            RuntimeException ex = assertThrows(RuntimeException.class, () -> servicio.crear(solicitud(RolStaff.DEFENSOR)));
            assertEquals("Ya existe una cuenta con ese correo institucional.", ex.getMessage(), "Correo único");
        }

        @Test
        void crear_sinRol_lanzaExcepcion() {
            RuntimeException ex = assertThrows(RuntimeException.class, () -> servicio.crear(solicitud(null)));
            assertEquals("Completa nombre, número de empleado, correo y rol.", ex.getMessage(), "Rol obligatorio");
        }

        @Test
        void resetearPassword_generaDistintaCadaVez() {
            when(encoder.encode(anyString())).thenReturn(HASH);
            String primera = servicio.resetearPassword(ID);
            String segunda = servicio.resetearPassword(ID);
            assertNotEquals(primera, segunda, "SecureRandom no repite la contraseña");
            assertTrue(personal.isCuentaTemporal(), "Queda como temporal");
        }
    }

    @Nested
    @DisplayName("Baja y cambio de rol (CU-A04): nunca quedarse sin administración")
    class Baja {

        @Test
        void darDeBaja_recepcionista_laDesactiva() {
            servicio.darDeBaja(ID, ADMIN);
            assertFalse(personal.isActivo(), "Queda inactiva");
        }

        @Test
        void darDeBaja_aSiMisma_lanzaExcepcion() {
            RuntimeException ex = assertThrows(RuntimeException.class, () -> servicio.darDeBaja(ID, CORREO.toUpperCase()));
            assertTrue(ex.getMessage().startsWith("No puedes desactivar"), "No se desactiva la propia cuenta");
        }

        @Test
        void darDeBaja_ultimoAdministradorActivo_lanzaExcepcion() {
            personal.setRol(RolStaff.DEFENSOR);
            when(repositorio.countByActivoTrueAndRolIn(any())).thenReturn(1L);
            RuntimeException ex = assertThrows(RuntimeException.class, () -> servicio.darDeBaja(ID, ADMIN));
            assertTrue(ex.getMessage().startsWith("Es la última cuenta activa"), "Siempre debe quedar un administrador");
            assertTrue(personal.isActivo(), "La cuenta sigue activa");
        }

        @Test
        void darDeBaja_administradorConOtroDisponible_seDesactiva() {
            personal.setRol(RolStaff.ADMIN_SISTEMAS);
            when(repositorio.countByActivoTrueAndRolIn(any())).thenReturn(2L);
            assertDoesNotThrow(() -> servicio.darDeBaja(ID, ADMIN), "Con dos administradores sí se puede");
        }

        @Test
        void editar_quitarRolAlUltimoAdministrador_lanzaExcepcion() {
            personal.setRol(RolStaff.DEFENSOR);
            when(repositorio.countByActivoTrueAndRolIn(any())).thenReturn(1L);
            assertThrows(RuntimeException.class, () -> servicio.editar(ID, solicitud(RolStaff.RECEPCIONISTA), ADMIN),
                    "Cambiar el rol del último administrador se bloquea");
        }

        @Test
        void editar_cambiarNombreSinTocarRol_noRevisaAdministracion() {
            PersonalRequest d = new PersonalRequest();
            d.setNombreCompleto("Nuevo nombre");
            servicio.editar(ID, d, ADMIN);
            assertEquals("Nuevo nombre", personal.getNombreCompleto(), "Se actualiza el nombre");
            verify(repositorio, never()).countByActivoTrueAndRolIn(any());
        }

        @Test
        void reactivar_cuentaInexistente_lanzaExcepcion() {
            when(repositorio.findById(99L)).thenReturn(Optional.empty());
            assertThrows(RuntimeException.class, () -> servicio.reactivar(99L), "Cuenta inexistente");
        }
    }

    @Nested
    @DisplayName("Cambio de contraseña propia (RF-A09)")
    class CambioPassword {

        @BeforeEach
        void cuentaExiste() {
            when(repositorio.findByCorreoInstitucional(CORREO)).thenReturn(Optional.of(personal));
        }

        @Test
        void cambiarMiPassword_actualIncorrecta_lanzaExcepcion() {
            when(encoder.matches("mala", HASH)).thenReturn(false);
            RuntimeException ex = assertThrows(RuntimeException.class,
                    () -> servicio.cambiarMiPassword(CORREO, "mala", "NuevaClave1"));
            assertEquals("La contraseña actual no es correcta.", ex.getMessage(), "Debe confirmar la actual");
        }

        @ParameterizedTest
        @ValueSource(strings = {"Corta1", "sinmayuscula1", "SinNumeros"})
        void cambiarMiPassword_nuevaNoCumple_lanzaExcepcion(String nueva) {
            when(encoder.matches("Actual123", HASH)).thenReturn(true);
            assertThrows(RuntimeException.class, () -> servicio.cambiarMiPassword(CORREO, "Actual123", nueva),
                    "Rechaza " + nueva);
        }

        @Test
        void cambiarMiPassword_valida_quitaLaMarcaDeTemporal() {
            when(encoder.matches("Actual123", HASH)).thenReturn(true);
            when(encoder.encode("NuevaClave1")).thenReturn("nuevo-hash");
            servicio.cambiarMiPassword(CORREO, "Actual123", "NuevaClave1");
            assertEquals("nuevo-hash", personal.getPassword(), "Contraseña cambiada");
            assertFalse(personal.isForzarCambioPassword(), "Ya no se le pide cambiarla");
        }
    }
}
