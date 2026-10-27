package ipn.escom.defensoria.queja_service.validacion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Pruebas unitarias de {@link ValidadorCorreo} (CU-Q01).
 * Particiones: vacío, formato general inválido, puntos mal colocados, dominios no Google
 * (reglas laxas) y dominios Google (parte local de 6 a 30 caracteres, solo letras/dígitos/punto).
 */
@DisplayName("ValidadorCorreo — validación en dos niveles")
class ValidadorCorreoTest {

    private static final int LONGITUD_MAXIMA = 254;
    private static final int GMAIL_MINIMO = 6;
    private static final int GMAIL_MAXIMO = 30;

    @Test
    void validarYNormalizar_correoConMayusculasYEspacios_regresaNormalizado() {
        String resultado = ValidadorCorreo.validarYNormalizar("  Juan.Perez@IPN.MX  ");
        assertEquals("juan.perez@ipn.mx", resultado, "Debe quitar espacios y pasar a minúsculas");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    void validarYNormalizar_vacio_lanzaFaltaCorreo(String correo) {
        ValidacionException ex = assertThrows(ValidacionException.class,
                () -> ValidadorCorreo.validarYNormalizar(correo));
        assertEquals("Falta el correo electrónico.", ex.getMessage(), "Mensaje para correo vacío");
    }

    @ParameterizedTest
    @ValueSource(strings = {"sinarroba.com", "a@b", "a@@b.com", "nombre@dominio.c", "nombre @ipn.mx", "@ipn.mx"})
    void validarYNormalizar_formatoInvalido_lanzaExcepcion(String correo) {
        ValidacionException ex = assertThrows(ValidacionException.class,
                () -> ValidadorCorreo.validarYNormalizar(correo));
        assertTrue(ex.getMessage().startsWith("El correo electrónico no tiene un formato válido"),
                "Debe rechazar por formato: " + correo);
    }

    @ParameterizedTest
    @ValueSource(strings = {".juan@ipn.mx", "juan.@ipn.mx"})
    void validarYNormalizar_puntoAlInicioOFinal_lanzaExcepcion(String correo) {
        ValidacionException ex = assertThrows(ValidacionException.class,
                () -> ValidadorCorreo.validarYNormalizar(correo));
        assertEquals("El correo no puede empezar ni terminar con un punto antes de la arroba.",
                ex.getMessage(), "Mensaje de punto en el borde");
    }

    @Test
    void validarYNormalizar_dosPuntosSeguidos_lanzaExcepcion() {
        ValidacionException ex = assertThrows(ValidacionException.class,
                () -> ValidadorCorreo.validarYNormalizar("juan..perez@ipn.mx"));
        assertEquals("El correo no puede tener dos puntos seguidos.", ex.getMessage(),
                "Mensaje de puntos dobles");
    }

    @ParameterizedTest
    @ValueSource(strings = {"juan-perez@outlook.com", "maria_lopez@yahoo.com.mx", "ab@ipn.mx", "x+y@alumno.ipn.mx"})
    void validarYNormalizar_dominioNoGoogleConSimbolos_esAceptado(String correo) {
        assertEquals(correo, ValidadorCorreo.validarYNormalizar(correo),
                "Las reglas de Gmail no deben aplicarse a otros dominios");
    }

    @Test
    void validarYNormalizar_gmailEnLimiteInferior_esAceptado() {
        String correo = "a".repeat(GMAIL_MINIMO) + "@gmail.com";
        assertEquals(correo, ValidadorCorreo.validarYNormalizar(correo), "6 caracteres es válido en Gmail");
    }

    @Test
    void validarYNormalizar_gmailDebajoDelLimite_lanzaExcepcion() {
        String correo = "a".repeat(GMAIL_MINIMO - 1) + "@gmail.com";
        ValidacionException ex = assertThrows(ValidacionException.class,
                () -> ValidadorCorreo.validarYNormalizar(correo));
        assertTrue(ex.getMessage().startsWith("Una dirección de Gmail"), "5 caracteres no es válido en Gmail");
    }

    @Test
    void validarYNormalizar_gmailEnLimiteSuperior_esAceptado() {
        String correo = "a".repeat(GMAIL_MAXIMO) + "@gmail.com";
        assertEquals(correo, ValidadorCorreo.validarYNormalizar(correo), "30 caracteres es válido en Gmail");
    }

    @Test
    void validarYNormalizar_gmailArribaDelLimite_lanzaExcepcion() {
        String correo = "a".repeat(GMAIL_MAXIMO + 1) + "@googlemail.com";
        assertThrows(ValidacionException.class, () -> ValidadorCorreo.validarYNormalizar(correo),
                "31 caracteres no es válido en Google");
    }

    @Test
    void validarYNormalizar_gmailConGuion_lanzaExcepcion() {
        assertThrows(ValidacionException.class,
                () -> ValidadorCorreo.validarYNormalizar("juan-perez@gmail.com"),
                "Gmail no admite guion en la parte local");
    }

    @Test
    void validarYNormalizar_excedeLongitudMaxima_lanzaExcepcion() {
        String dominio = "@ipn.mx";
        String correo = "a".repeat(LONGITUD_MAXIMA - dominio.length() + 1) + dominio;
        ValidacionException ex = assertThrows(ValidacionException.class,
                () -> ValidadorCorreo.validarYNormalizar(correo));
        assertEquals("El correo electrónico es demasiado largo.", ex.getMessage(), "255 caracteres excede el máximo");
    }
}
