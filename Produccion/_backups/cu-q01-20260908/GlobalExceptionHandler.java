package ipn.escom.defensoria.queja_service.config;

import ipn.escom.defensoria.queja_service.model.ErrorResponseModel;
import java.time.LocalDateTime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

// Mismo patrón que auth-service: errores de validación (RuntimeException con mensaje) se
// devuelven como {mensaje, timestamp, codigo}, formato que el frontend ya sabe leer
// (err?.error?.mensaje) desde que se implementó el login.
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ErrorResponseModel> manejarRuntimeException(RuntimeException ex) {
        // Antes esto no se registraba en ningún lado: la excepción se atrapaba aquí y solo se
        // mandaba la respuesta al cliente, así que en los logs del contenedor nunca aparecía
        // rastro de que algo había fallado. Con esto sí queda visible en "podman logs".
        log.warn("Solicitud inválida ({}): {}", ex.getClass().getSimpleName(), ex.getMessage());
        ErrorResponseModel error = new ErrorResponseModel(
                ex.getMessage(),
                LocalDateTime.now(),
                HttpStatus.BAD_REQUEST.value()
        );
        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponseModel> manejarErroresGenerales(Exception ex) {
        // Este es el handler que atrapaba los 500 silenciosamente. Ahora imprime el stack
        // trace completo para poder diagnosticar la causa real la próxima vez que pase.
        log.error("Error no controlado procesando la petición", ex);
        ErrorResponseModel error = new ErrorResponseModel(
                "Ocurrió un error inesperado en el servidor.",
                LocalDateTime.now(),
                HttpStatus.INTERNAL_SERVER_ERROR.value()
        );
        return new ResponseEntity<>(error, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
