package ipn.escom.defensoria.notificaciones_service.exception;

/** Faltan datos obligatorios al registrar una notificación. (antes era un RuntimeException genérico; Kiuwan: "Avoid throwing RuntimeExceptions"). */
public class DatosNotificacionInvalidosException extends Exception {

    private static final long serialVersionUID = 1L;

    public DatosNotificacionInvalidosException(String mensaje) {
        super(mensaje);
    }
}
