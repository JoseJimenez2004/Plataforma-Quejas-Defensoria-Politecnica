package ipn.escom.defensoria.queja_service.validacion;

/**
 * Error de validación de datos de entrada. Se traduce a HTTP 400 con el mensaje tal cual
 * (GlobalExceptionHandler ya atrapa RuntimeException y arma {mensaje, timestamp, codigo},
 * formato que el frontend lee en err.error.mensaje).
 *
 * El mensaje SIEMPRE debe ser legible para el quejoso, no para el desarrollador: es lo que
 * se le muestra en pantalla.
 */
public class ValidacionException extends RuntimeException {

    public ValidacionException(String mensaje) {
        super(mensaje);
    }
}
