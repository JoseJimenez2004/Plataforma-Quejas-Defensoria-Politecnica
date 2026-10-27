package ipn.escom.defensoria.queja_service.validacion;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Pruebas unitarias de {@link ValidadorFechas}. El "hoy" se toma del propio validador para que
 * las pruebas no dependan de la fecha en que se ejecuten.
 */
@DisplayName("ValidadorFechas — fecha de nacimiento y fecha de los hechos")
class ValidadorFechasTest {

    private static final LocalDate MINIMA = ReglasQueja.FECHA_NACIMIENTO_MINIMA;
    private static final LocalDate NACIMIENTO_VALIDO = LocalDate.of(2000, 5, 10);

    @Test
    void fechaNacimientoMaxima_esTreintaYUnoDeDiciembreDelAnioPasado() {
        LocalDate maxima = ValidadorFechas.fechaNacimientoMaxima();
        assertEquals(ValidadorFechas.hoy().getYear() - 1, maxima.getYear(), "Debe ser del año anterior");
        assertEquals(12, maxima.getMonthValue(), "Debe ser diciembre");
        assertEquals(31, maxima.getDayOfMonth(), "Debe ser el día 31");
    }

    @Test
    void validarFechaNacimiento_nula_lanzaExcepcion() {
        ValidacionException ex = assertThrows(ValidacionException.class,
                () -> ValidadorFechas.validarFechaNacimiento(null));
        assertEquals("Falta la fecha de nacimiento.", ex.getMessage(), "Mensaje de fecha faltante");
    }

    @Test
    void validarFechaNacimiento_limiteInferiorExacto_esValida() {
        assertDoesNotThrow(() -> ValidadorFechas.validarFechaNacimiento(MINIMA), "1920-01-01 es válida");
    }

    @Test
    void validarFechaNacimiento_unDiaAntesDelLimiteInferior_lanzaExcepcion() {
        ValidacionException ex = assertThrows(ValidacionException.class,
                () -> ValidadorFechas.validarFechaNacimiento(MINIMA.minusDays(1)));
        assertTrue(ex.getMessage().contains("1920"), "Debe mencionar el límite de 1920");
    }

    @Test
    void validarFechaNacimiento_limiteSuperiorExacto_esValida() {
        LocalDate maxima = ValidadorFechas.fechaNacimientoMaxima();
        assertDoesNotThrow(() -> ValidadorFechas.validarFechaNacimiento(maxima), "31/dic del año pasado es válida");
    }

    @Test
    void validarFechaNacimiento_unDiaDespuesDelLimiteSuperior_lanzaExcepcion() {
        LocalDate primeroDeEnero = ValidadorFechas.fechaNacimientoMaxima().plusDays(1);
        ValidacionException ex = assertThrows(ValidacionException.class,
                () -> ValidadorFechas.validarFechaNacimiento(primeroDeEnero));
        assertTrue(ex.getMessage().startsWith("La fecha de nacimiento no puede ser posterior"),
                "Nadie puede declarar haber nacido en el año en curso");
    }

    @Test
    void validarFechaHechos_nula_lanzaExcepcion() {
        ValidacionException ex = assertThrows(ValidacionException.class,
                () -> ValidadorFechas.validarFechaHechos(null, NACIMIENTO_VALIDO));
        assertEquals("Falta la fecha de los hechos.", ex.getMessage(), "Mensaje de fecha faltante");
    }

    @Test
    void validarFechaHechos_hoy_esValida() {
        assertDoesNotThrow(() -> ValidadorFechas.validarFechaHechos(ValidadorFechas.hoy(), NACIMIENTO_VALIDO),
                "Los hechos pueden ser de hoy");
    }

    @Test
    void validarFechaHechos_manana_lanzaExcepcion() {
        LocalDate manana = ValidadorFechas.hoy().plusDays(1);
        ValidacionException ex = assertThrows(ValidacionException.class,
                () -> ValidadorFechas.validarFechaHechos(manana, NACIMIENTO_VALIDO));
        assertEquals("La fecha de los hechos no puede ser posterior a hoy.", ex.getMessage(), "Fecha futura");
    }

    @Test
    void validarFechaHechos_mismoDiaDelNacimiento_esValida() {
        assertDoesNotThrow(() -> ValidadorFechas.validarFechaHechos(NACIMIENTO_VALIDO, NACIMIENTO_VALIDO),
                "El mismo día del nacimiento es el borde válido");
    }

    @Test
    void validarFechaHechos_antesDelNacimiento_lanzaExcepcion() {
        ValidacionException ex = assertThrows(ValidacionException.class,
                () -> ValidadorFechas.validarFechaHechos(NACIMIENTO_VALIDO.minusDays(1), NACIMIENTO_VALIDO));
        assertTrue(ex.getMessage().contains("anterior a tu fecha de nacimiento"), "Hechos antes de nacer");
    }

    @Test
    void validarFechaHechos_sinFechaNacimiento_soloValidaContraHoy() {
        assertDoesNotThrow(() -> ValidadorFechas.validarFechaHechos(MINIMA, null),
                "Sin fecha de nacimiento no se compara contra ella");
    }
}
