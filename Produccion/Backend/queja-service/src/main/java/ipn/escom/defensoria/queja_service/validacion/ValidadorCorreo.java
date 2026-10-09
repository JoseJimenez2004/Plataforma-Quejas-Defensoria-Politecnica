package ipn.escom.defensoria.queja_service.validacion;

import java.util.Set;
import java.util.regex.Pattern;

/**
 * Validación de correo en DOS NIVELES.
 *
 * Nivel 1 — formato general, para cualquier dominio. Es la única regla que aplica a
 * direcciones institucionales, Outlook, Yahoo, etc.
 *
 * Nivel 2 — reglas propias de Google, que SOLO se aplican si el dominio es gmail.com o
 * googlemail.com. Son las restricciones que Google impone al CREAR una cuenta (nada de
 * & = _ ' - + , < > ni espacios en la parte local).
 *
 * El motivo de separarlas: esas restricciones son de Gmail, no del correo electrónico en
 * general. Aplicarlas a todos los dominios rechazaría direcciones válidas y en uso, como
 * juan-perez@outlook.com o maria_lopez@yahoo.com.mx — y en un formulario de queja pública
 * eso significa perder casos reales.
 */
public final class ValidadorCorreo {

    private static final int LONGITUD_MAXIMA = 254;

    /** Nivel 1: estructura general parte-local@dominio.tld */
    private static final Pattern FORMATO_GENERAL = Pattern.compile(
            "^[A-Za-z0-9._%+\\-]+@[A-Za-z0-9](?:[A-Za-z0-9\\-]*[A-Za-z0-9])?"
                    + "(?:\\.[A-Za-z0-9](?:[A-Za-z0-9\\-]*[A-Za-z0-9])?)*\\.[A-Za-z]{2,}$");

    /** Nivel 2: parte local de una cuenta Google — solo letras, dígitos y punto, 6 a 30. */
    private static final Pattern LOCAL_GOOGLE = Pattern.compile("^[A-Za-z0-9.]{6,30}$");

    private static final Set<String> DOMINIOS_GOOGLE = Set.of("gmail.com", "googlemail.com");

    private ValidadorCorreo() {
    }

    /**
     * Valida y devuelve el correo normalizado (sin espacios, en minúsculas).
     *
     * @throws ValidacionException con un mensaje que se le muestra tal cual al quejoso.
     */
    public static String validarYNormalizar(String correoCrudo) {
        if (Textos.esVacio(correoCrudo)) {
            throw new ValidacionException("Falta el correo electrónico.");
        }

        String correo = correoCrudo.trim().toLowerCase();

        if (correo.length() > LONGITUD_MAXIMA) {
            throw new ValidacionException("El correo electrónico es demasiado largo.");
        }
        if (!FORMATO_GENERAL.matcher(correo).matches()) {
            throw new ValidacionException(
                    "El correo electrónico no tiene un formato válido. Ejemplo: nombre@dominio.com");
        }

        int arroba = correo.lastIndexOf('@');
        String parteLocal = correo.substring(0, arroba);
        String dominio = correo.substring(arroba + 1);

        validarPuntos(parteLocal);

        if (DOMINIOS_GOOGLE.contains(dominio)) {
            validarReglasGoogle(parteLocal);
        }

        return correo;
    }

    /** El punto no puede abrir ni cerrar la parte local, ni aparecer dos veces seguidas. */
    private static void validarPuntos(String parteLocal) {
        if (parteLocal.startsWith(".") || parteLocal.endsWith(".")) {
            throw new ValidacionException(
                    "El correo no puede empezar ni terminar con un punto antes de la arroba.");
        }
        if (parteLocal.contains("..")) {
            throw new ValidacionException("El correo no puede tener dos puntos seguidos.");
        }
    }

    private static void validarReglasGoogle(String parteLocal) {
        if (!LOCAL_GOOGLE.matcher(parteLocal).matches()) {
            throw new ValidacionException(
                    "Una dirección de Gmail solo admite letras, números y puntos antes de la arroba, "
                            + "con un mínimo de 6 y un máximo de 30 caracteres.");
        }
    }
}
