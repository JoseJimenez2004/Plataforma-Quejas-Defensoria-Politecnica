package ipn.escom.defensoria.denunciado_service.validacion;

/** Error de datos de entrada → HTTP 400. El mensaje se le muestra tal cual al denunciado. */
public class ValidacionException extends RuntimeException {
    public ValidacionException(String mensaje) {
        super(mensaje);
    }
}
