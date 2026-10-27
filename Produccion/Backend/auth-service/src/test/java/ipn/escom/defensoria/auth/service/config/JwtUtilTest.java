package ipn.escom.defensoria.auth.service.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.jsonwebtoken.JwtException;

/** Pruebas unitarias de {@link JwtUtil}: emisión y verificación del token del quejoso. */
@DisplayName("JwtUtil — tokens del quejoso")
class JwtUtilTest {

    /** Secretos de prueba (≥ 256 bits); no son los de producción. */
    private static final String SECRETO = "secreto-de-pruebas-unitarias-con-mas-de-32-bytes";
    private static final String OTRO_SECRETO = "otro-secreto-distinto-de-pruebas-con-32-bytes!!";
    private static final String CORREO = "quejoso@ipn.mx";

    private final JwtUtil jwt = new JwtUtil(SECRETO);

    @Test
    void generarToken_extraerCorreo_regresaElSujeto() {
        String token = jwt.generarToken(CORREO);
        assertEquals(CORREO, jwt.extraerCorreo(token), "El sujeto del token es el correo");
    }

    @Test
    void validarToken_mismoCorreo_esValido() {
        assertTrue(jwt.validarToken(jwt.generarToken(CORREO), CORREO), "Token recién emitido es válido");
    }

    @Test
    void validarToken_otroCorreo_noEsValido() {
        assertFalse(jwt.validarToken(jwt.generarToken(CORREO), "otro@ipn.mx"), "No sirve para otra cuenta");
    }

    @Test
    void extraerCorreo_tokenFirmadoConOtroSecreto_lanzaExcepcion() {
        String ajeno = new JwtUtil(OTRO_SECRETO).generarToken(CORREO);
        assertThrows(JwtException.class, () -> jwt.extraerCorreo(ajeno), "La firma debe verificarse");
    }

    @Test
    void extraerCorreo_tokenAlterado_lanzaExcepcion() {
        String token = jwt.generarToken(CORREO);
        String alterado = token.substring(0, token.length() - 2) + (token.endsWith("A") ? "BB" : "AA");
        assertThrows(JwtException.class, () -> jwt.extraerCorreo(alterado), "Un token modificado se rechaza");
    }

    @Test
    void constructor_secretoCorto_lanzaExcepcion() {
        assertThrows(RuntimeException.class, () -> new JwtUtil("corto"), "HS256 exige al menos 256 bits");
    }
}
