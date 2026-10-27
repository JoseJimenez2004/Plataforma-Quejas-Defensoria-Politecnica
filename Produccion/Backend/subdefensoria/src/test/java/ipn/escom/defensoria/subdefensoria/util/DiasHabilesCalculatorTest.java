package ipn.escom.defensoria.subdefensoria.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.LocalDate;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Pruebas unitarias de {@link DiasHabilesCalculator}: plazos del Manual DDP-PO-02
 * (10 días hábiles la primera solicitud, 5 los recordatorios).
 */
@DisplayName("DiasHabilesCalculator — plazos en días hábiles")
class DiasHabilesCalculatorTest {

    private static final int PRIMERA_SOLICITUD = 10;
    private static final int RECORDATORIO = 5;
    /** Lunes 2 de noviembre de 2026. */
    private static final LocalDate LUNES = LocalDate.of(2026, 11, 2);
    private static final LocalDate VIERNES = LocalDate.of(2026, 11, 6);
    private static final LocalDate SABADO = LocalDate.of(2026, 11, 7);

    @Test
    void sumarDiasHabiles_diezDesdeLunes_terminaElLunesDosSemanasDespues() {
        assertEquals(LocalDate.of(2026, 11, 16), DiasHabilesCalculator.sumarDiasHabiles(LUNES, PRIMERA_SOLICITUD),
                "10 hábiles saltan dos fines de semana");
    }

    @Test
    void sumarDiasHabiles_cincoDesdeLunes_terminaElLunesSiguiente() {
        assertEquals(LocalDate.of(2026, 11, 9), DiasHabilesCalculator.sumarDiasHabiles(LUNES, RECORDATORIO),
                "5 hábiles = una semana");
    }

    @ParameterizedTest(name = "desde {0} + 1 hábil = {1}")
    @CsvSource({"2026-11-06, 2026-11-09", "2026-11-07, 2026-11-09", "2026-11-08, 2026-11-09", "2026-11-02, 2026-11-03"})
    void sumarDiasHabiles_unDia_saltaFinDeSemana(LocalDate desde, LocalDate esperado) {
        assertEquals(esperado, DiasHabilesCalculator.sumarDiasHabiles(desde, 1), "El siguiente día hábil");
    }

    @Test
    void sumarDiasHabiles_cero_regresaLaMismaFecha() {
        assertEquals(SABADO, DiasHabilesCalculator.sumarDiasHabiles(SABADO, 0), "Sumar 0 no mueve la fecha");
    }

    @Test
    void diasHabilesTranscurridos_deLunesAViernes_sonCuatro() {
        assertEquals(4, DiasHabilesCalculator.diasHabilesTranscurridos(LUNES, VIERNES), "No cuenta el día de inicio");
    }

    @Test
    void diasHabilesTranscurridos_deViernesALunes_esUno() {
        assertEquals(1, DiasHabilesCalculator.diasHabilesTranscurridos(VIERNES, LocalDate.of(2026, 11, 9)),
                "El fin de semana no cuenta");
    }

    @Test
    void diasHabilesTranscurridos_mismaFecha_cero() {
        assertEquals(0, DiasHabilesCalculator.diasHabilesTranscurridos(LUNES, LUNES), "Mismo día");
    }

    @Test
    void diasHabilesTranscurridos_fechasInvertidas_cero() {
        assertEquals(0, DiasHabilesCalculator.diasHabilesTranscurridos(VIERNES, LUNES), "Nunca negativo");
    }

    @Test
    void sumarYContar_sonOperacionesInversas() {
        LocalDate limite = DiasHabilesCalculator.sumarDiasHabiles(LUNES, PRIMERA_SOLICITUD);
        assertEquals(PRIMERA_SOLICITUD, DiasHabilesCalculator.diasHabilesTranscurridos(LUNES, limite),
                "Contar hasta el límite da el mismo plazo");
    }
}
