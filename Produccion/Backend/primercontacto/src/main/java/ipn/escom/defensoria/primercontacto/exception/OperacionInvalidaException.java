package ipn.escom.defensoria.primercontacto.exception;

/**
 * La operación no aplica en el estado actual del expediente (por ejemplo, dictaminar un
 * expediente que ya tiene dictamen). Se responde 409 con el mensaje tal cual, para que el
 * front se lo muestre al analista en vez de un error genérico.
 */
public class OperacionInvalidaException extends RuntimeException {

    public OperacionInvalidaException(String message) {
        super(message);
    }
}
