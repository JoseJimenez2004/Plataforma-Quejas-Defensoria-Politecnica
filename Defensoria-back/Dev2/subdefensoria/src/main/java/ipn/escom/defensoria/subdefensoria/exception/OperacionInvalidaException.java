package ipn.escom.defensoria.subdefensoria.exception;

/**
 * Se lanza cuando la operacion solicitada no respeta el flujo del
 * procedimiento DDP-PO-02 (ej. generar un recordatorio sobre un
 * oficio que ya fue respondido, o enviar a firma un expediente que
 * no esta LISTO_A_DICTAMINAR).
 */
public class OperacionInvalidaException extends RuntimeException {
    public OperacionInvalidaException(String mensaje) {
        super(mensaje);
    }
}
