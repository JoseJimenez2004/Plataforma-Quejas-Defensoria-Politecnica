package ipn.escom.defensoria.denunciado_service.validacion;

public final class Textos {

    private Textos() {
    }

    public static boolean esVacio(String valor) {
        return valor == null || valor.isBlank();
    }

    /** "  Juan   Carlos " -> "Juan Carlos"; null si no queda nada. */
    public static String normalizar(String valor) {
        if (valor == null) {
            return null;
        }
        String limpio = valor.trim().replaceAll("\\s+", " ");
        return limpio.isEmpty() ? null : limpio;
    }
}
