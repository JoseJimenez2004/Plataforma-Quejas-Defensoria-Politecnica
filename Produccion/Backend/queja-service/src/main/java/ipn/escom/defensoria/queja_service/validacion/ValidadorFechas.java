package ipn.escom.defensoria.queja_service.validacion;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;

/**
 * Reglas de fecha del CU-Q01, aplicadas en el microservicio (fuente de verdad).
 *
 * El "hoy" y el "año en curso" se calculan con el reloj del servidor en zona
 * America/Mexico_City, NO con constantes en código: si se dejara un año fijo, la regla
 * quedaría obsoleta el 1 de enero siguiente sin que nadie se dé cuenta.
 */
public final class ValidadorFechas {

    private static final Clock RELOJ = Clock.system(ZoneId.of(ReglasQueja.ZONA_HORARIA));

    private ValidadorFechas() {
    }

    public static LocalDate hoy() {
        return LocalDate.now(RELOJ);
    }

    /**
     * Último día de nacimiento aceptable: 31 de diciembre del año pasado.
     * Regla de negocio: durante el año en curso nadie puede declarar haber nacido ese año.
     */
    public static LocalDate fechaNacimientoMaxima() {
        return LocalDate.of(hoy().getYear() - 1, 12, 31);
    }

    public static void validarFechaNacimiento(LocalDate fechaNacimiento) {
        if (fechaNacimiento == null) {
            throw new ValidacionException("Falta la fecha de nacimiento.");
        }
        if (fechaNacimiento.isBefore(ReglasQueja.FECHA_NACIMIENTO_MINIMA)) {
            throw new ValidacionException(
                    "La fecha de nacimiento no puede ser anterior al 1 de enero de 1920.");
        }
        LocalDate maxima = fechaNacimientoMaxima();
        if (fechaNacimiento.isAfter(maxima)) {
            throw new ValidacionException(
                    "La fecha de nacimiento no puede ser posterior al 31 de diciembre de "
                            + maxima.getYear() + ".");
        }
    }

    public static void validarFechaHechos(LocalDate fechaHechos, LocalDate fechaNacimiento) {
        if (fechaHechos == null) {
            throw new ValidacionException("Falta la fecha de los hechos.");
        }
        if (fechaHechos.isAfter(hoy())) {
            throw new ValidacionException("La fecha de los hechos no puede ser posterior a hoy.");
        }
        if (fechaNacimiento != null && fechaHechos.isBefore(fechaNacimiento)) {
            throw new ValidacionException(
                    "La fecha de los hechos no puede ser anterior a tu fecha de nacimiento.");
        }
        validarPlazo(fechaHechos);
    }

    /** Fecha más antigua aceptada para los hechos: hoy menos {@link ReglasQueja#PLAZO_DIAS_QUEJA}. */
    public static LocalDate fechaHechosMinima() {
        return hoy().minusDays(ReglasQueja.PLAZO_DIAS_QUEJA);
    }

    /**
     * Candado de 90 días naturales (artículo 13, párrafo tercero). Se aplica en todo registro
     * nuevo, venga del formulario público, del panel del quejoso o de ventanilla.
     */
    public static void validarPlazo(LocalDate fechaHechos) {
        if (fechaHechos != null && fechaHechos.isBefore(fechaHechosMinima())) {
            throw new ValidacionException("Solo se pueden presentar quejas por hechos ocurridos dentro de los últimos "
                    + ReglasQueja.PLAZO_DIAS_QUEJA + " días naturales (artículo 13, párrafo tercero).");
        }
    }
}
