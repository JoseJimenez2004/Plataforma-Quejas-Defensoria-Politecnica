package ipn.escom.defensoria.primercontacto.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Centraliza el formato de error para toda la API.
 *
 * Antes, un folio inexistente (u otro RuntimeException) se traducía
 * en una respuesta 500 genérica que el front solo mandaba a
 * console.error, dejando la pantalla con los campos vacíos y sin
 * ninguna pista de qué había fallado. Con esto, el front siempre
 * recibe un JSON entendible {"error": "..."} y el código de estado
 * correcto (404 cuando no existe, 400 cuando el payload es
 * inválido, 409 cuando la operación no aplica en el estado actual,
 * 500 para lo demás).
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(RecursoNoEncontradoException.class)
    public ResponseEntity<Map<String, Object>> manejarNoEncontrado(RecursoNoEncontradoException ex) {
        return construirRespuesta(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(OperacionInvalidaException.class)
    public ResponseEntity<Map<String, Object>> manejarOperacionInvalida(OperacionInvalidaException ex) {
        return construirRespuesta(HttpStatus.CONFLICT, ex.getMessage());
    }

    /**
     * 401/403 de AnalistaAutenticadoService y de la validación de autoría de notas. Sin este
     * manejador caían en el de RuntimeException y salían como 500.
     */
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, Object>> manejarResponseStatus(ResponseStatusException ex) {
        return construirRespuesta(ex.getStatusCode(), ex.getReason());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> manejarValidacion(MethodArgumentNotValidException ex) {
        String detalle = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .reduce((a, b) -> a + "; " + b)
                .orElse("Payload inválido");

        return construirRespuesta(HttpStatus.BAD_REQUEST, detalle);
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, Object>> manejarRuntime(RuntimeException ex) {
        return construirRespuesta(HttpStatus.INTERNAL_SERVER_ERROR, ex.getMessage());
    }

    private ResponseEntity<Map<String, Object>> construirRespuesta(HttpStatusCode status, String mensaje) {
        Map<String, Object> cuerpo = new LinkedHashMap<>();
        cuerpo.put("timestamp", Instant.now().toString());
        cuerpo.put("status", status.value());
        cuerpo.put("error", mensaje);
        return ResponseEntity.status(status).body(cuerpo);
    }
}
