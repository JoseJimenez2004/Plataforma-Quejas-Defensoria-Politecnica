package ipn.escom.defensoria.subdefensoria.util;

import java.util.Locale;
import java.util.Set;

/** Prioridad del expediente (SD-6): la asigna Primer Contacto y la puede cambiar el subdefensor. */
public final class Prioridad {

    public static final Set<String> VALORES = Set.of("BAJA", "MEDIA", "ALTA", "URGENTE");

    private Prioridad() {
    }

    public static boolean esValida(String valor) {
        return valor != null && VALORES.contains(valor.trim().toUpperCase(Locale.ROOT));
    }

    /** Mayúsculas si es válida; null en cualquier otro caso ("Sin asignar"). */
    public static String normalizar(String valor) {
        return esValida(valor) ? valor.trim().toUpperCase(Locale.ROOT) : null;
    }
}
