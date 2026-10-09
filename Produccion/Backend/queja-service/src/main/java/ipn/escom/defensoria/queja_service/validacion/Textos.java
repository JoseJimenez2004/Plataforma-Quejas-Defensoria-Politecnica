package ipn.escom.defensoria.queja_service.validacion;

/** Utilidades de limpieza de texto previas a validar y persistir. */
public final class Textos {

    private Textos() {
    }

    public static boolean esVacio(String valor) {
        return valor == null || valor.isBlank();
    }

    /**
     * Quita espacios al inicio/final y colapsa espacios internos repetidos.
     * "  Juan   Carlos " -> "Juan Carlos". Devuelve null si no queda nada.
     *
     * Se aplica ANTES de validar: si no, "Juan  Carlos" (dos espacios) fallaría el regex de
     * nombre por un motivo que el usuario no puede ver en pantalla.
     */
    public static String normalizar(String valor) {
        if (valor == null) {
            return null;
        }
        String limpio = valor.trim().replaceAll("\\s+", " ");
        return limpio.isEmpty() ? null : limpio;
    }
}
