package ipn.escom.defensoria.quejoso.config;

import ipn.escom.defensoria.quejoso.dto.ErrorRespuestaDTO;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;

@RestControllerAdvice
public class GlobalExceptionHandler {

    // Maneja las excepciones de negocio (las que lanzamos con throw new RuntimeException)
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ErrorRespuestaDTO> manejarRuntimeException(RuntimeException ex) {
        ErrorRespuestaDTO error = new ErrorRespuestaDTO(
                ex.getMessage(),
                LocalDateTime.now(),
                HttpStatus.BAD_REQUEST.value()
        );
        // Usamos 400 Bad Request para errores de validación de negocio
        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }

    // Maneja errores inesperados del sistema
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorRespuestaDTO> manejarErroresGenerales(Exception ex) {
        ErrorRespuestaDTO error = new ErrorRespuestaDTO(
                "Ocurrió un error inesperado en el servidor.",
                LocalDateTime.now(),
                HttpStatus.INTERNAL_SERVER_ERROR.value()
        );
        return new ResponseEntity<>(error, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}