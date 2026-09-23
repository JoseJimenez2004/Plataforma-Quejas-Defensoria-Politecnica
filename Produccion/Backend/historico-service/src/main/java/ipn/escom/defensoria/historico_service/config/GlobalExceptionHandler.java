package ipn.escom.defensoria.historico_service.config;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Convierte las validaciones en mensajes que el recepcionista pueda entender, en vez de un
 * 500 genérico. Mismo criterio que el GlobalExceptionHandler de queja-service.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> datosInvalidos(IllegalArgumentException ex) {
        return ResponseEntity.badRequest().body(Map.of("mensaje", ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> camposFaltantes(MethodArgumentNotValidException ex) {
        String mensaje = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> e.getDefaultMessage())
                .findFirst()
                .orElse("Faltan datos obligatorios.");
        return ResponseEntity.badRequest().body(Map.of("mensaje", mensaje));
    }

    // A PROPÓSITO no hay un @ExceptionHandler(Exception.class): atraparía también la
    // AccessDeniedException que lanza @PreAuthorize y la convertiría en un 500, tapando
    // los 403 de autorización. Spring Security necesita que esa excepción le llegue.
}
