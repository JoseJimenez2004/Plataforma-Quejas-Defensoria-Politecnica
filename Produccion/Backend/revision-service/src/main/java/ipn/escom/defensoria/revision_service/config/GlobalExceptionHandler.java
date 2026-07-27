package ipn.escom.defensoria.revision_service.config;

import java.time.LocalDateTime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import ipn.escom.defensoria.revision_service.model.ErrorResponseModel;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponseModel> manejarAccesoDenegado(AccessDeniedException ex) {
        log.warn("Acceso denegado: {}", ex.getMessage());
        ErrorResponseModel error = new ErrorResponseModel(
                "No tienes permiso para realizar esta acción.",
                LocalDateTime.now(),
                HttpStatus.FORBIDDEN.value());
        return new ResponseEntity<>(error, HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ErrorResponseModel> manejarRuntimeException(RuntimeException ex) {
        log.warn("Solicitud inválida ({}): {}", ex.getClass().getSimpleName(), ex.getMessage());
        ErrorResponseModel error = new ErrorResponseModel(
                ex.getMessage(),
                LocalDateTime.now(),
                HttpStatus.BAD_REQUEST.value());
        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponseModel> manejarErroresGenerales(Exception ex) {
        log.error("Error no controlado procesando la petición", ex);
        ErrorResponseModel error = new ErrorResponseModel(
                "Ocurrió un error inesperado en el servidor.",
                LocalDateTime.now(),
                HttpStatus.INTERNAL_SERVER_ERROR.value());
        return new ResponseEntity<>(error, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
