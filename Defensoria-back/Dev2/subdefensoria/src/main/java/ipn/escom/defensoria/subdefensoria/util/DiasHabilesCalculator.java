package ipn.escom.defensoria.subdefensoria.util;

import java.time.DayOfWeek;
import java.time.LocalDate;

/**
 * Calcula fechas contando solo dias habiles (lunes a viernes), tal
 * como lo pide la politica de operacion #7 del Manual DDP-PO-02:
 * 10 dias habiles para la primera solicitud de informacion, 5 dias
 * habiles para las subsecuentes.
 *
 * Nota: esto NO descuenta dias festivos oficiales del calendario del
 * IPN todavia (no hay ese catalogo disponible aun); si mas adelante
 * se agrega un catalogo de dias inhabiles, este es el unico lugar
 * que habria que tocar.
 */
public final class DiasHabilesCalculator {

    private DiasHabilesCalculator() {
    }

    public static LocalDate sumarDiasHabiles(LocalDate fechaInicio, int diasHabiles) {
        LocalDate fecha = fechaInicio;
        int sumados = 0;
        while (sumados < diasHabiles) {
            fecha = fecha.plusDays(1);
            if (esDiaHabil(fecha)) {
                sumados++;
            }
        }
        return fecha;
    }

    public static long diasHabilesTranscurridos(LocalDate desde, LocalDate hasta) {
        if (hasta.isBefore(desde)) {
            return 0;
        }
        long contador = 0;
        LocalDate cursor = desde;
        while (cursor.isBefore(hasta)) {
            cursor = cursor.plusDays(1);
            if (esDiaHabil(cursor)) {
                contador++;
            }
        }
        return contador;
    }

    private static boolean esDiaHabil(LocalDate fecha) {
        DayOfWeek dia = fecha.getDayOfWeek();
        return dia != DayOfWeek.SATURDAY && dia != DayOfWeek.SUNDAY;
    }
}
