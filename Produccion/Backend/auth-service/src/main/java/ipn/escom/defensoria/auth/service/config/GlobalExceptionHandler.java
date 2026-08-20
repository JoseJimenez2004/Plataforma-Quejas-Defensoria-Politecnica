package ipn.escom.defensoria.auth.service.config;

import ipn.escom.defensoria.auth.service.model.ErrorResponseModel;
import java.time.LocalDateTime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ErrorResponseModel> manejarRuntimeException(RuntimeException ex) {
        // Antes no se registraba en ningún lado — se atrapaba la excepción y solo se mandaba
        // la respuesta al cliente, sin dejar rastro en "podman logs".
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
        // Handler que atrapaba los 500 en silencio. Ahora imprime el stack trace completo.
        log.error("Error no controlado procesando la petición", ex);
        ErrorResponseModel error = new ErrorResponseModel(
                "Ocurrió un error inesperado en el servidor.",
                LocalDateTime.now(),
                HttpStatus.INTERNAL_SERVER_ERROR.value()
        );
        return new ResponseEntity<>(error, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}