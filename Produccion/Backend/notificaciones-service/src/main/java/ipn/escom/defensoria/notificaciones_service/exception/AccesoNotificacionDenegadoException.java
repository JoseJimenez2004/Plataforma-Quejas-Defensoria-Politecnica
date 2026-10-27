package ipn.escom.defensoria.notificaciones_service.exception;

/** Un usuario intentó operar sobre una notificación que no es suya. (antes era un RuntimeException genérico; Kiuwan: "Avoid throwing RuntimeExceptions"). */
public class AccesoNotificacionDenegadoException extends Exception {

    private static final long serialVersionUID = 1L;

    public AccesoNotificacionDenegadoException(String mensaje) {
        super(mensaje);
    }
}
