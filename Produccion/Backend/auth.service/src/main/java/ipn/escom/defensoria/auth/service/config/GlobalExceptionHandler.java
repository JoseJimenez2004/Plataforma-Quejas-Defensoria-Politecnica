package ipn.escom.defensoria.auth.service.config;

import ipn.escom.defensoria.auth.service.model.ErrorResponseModel;
import java.time.LocalDateTime;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ErrorResponseModel> manejarRuntimeException(RuntimeException ex) {
        ErrorResponseModel error = new ErrorResponseModel(
                ex.getMessage(),
                LocalDateTime.now(),
                HttpStatus.BAD_REQUEST.value()
        );
        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponseModel> manejarErroresGenerales(Exception ex) {
        ErrorResponseModel error = new ErrorResponseModel(
                "Ocurrió un error inesperado en el servidor.",
                LocalDateTime.now(),
                HttpStatus.INTERNAL_SERVER_ERROR.value()
        );
        return new ResponseEntity<>(error, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}