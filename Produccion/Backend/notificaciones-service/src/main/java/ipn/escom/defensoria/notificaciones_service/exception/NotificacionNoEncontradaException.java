package ipn.escom.defensoria.notificaciones_service.exception;

/** La notificación solicitada no existe. (antes era un RuntimeException genérico; Kiuwan: "Avoid throwing RuntimeExceptions"). */
public class NotificacionNoEncontradaException extends Exception {

    private static final long serialVersionUID = 1L;

    public NotificacionNoEncontradaException(String mensaje) {
        super(mensaje);
    }
}
