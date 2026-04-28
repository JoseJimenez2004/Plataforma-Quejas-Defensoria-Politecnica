package ipn.escom.defensoria.recepcionista.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Manejador global de excepciones del microservicio.
 *
 * @RestControllerAdvice hace que este componente intercepte cualquier excepción
 * lanzada desde los controllers y la convierta en una respuesta JSON estructurada,
 * en lugar de devolver el stack trace de Java al cliente.
 *
 * Todas las respuestas de error tienen el mismo formato:
 * {
 *   "timestamp": "2026-04-10T18:00:00",
 *   "status": 400,
 *   "mensaje": "descripción del error"
 * }
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Captura errores de lógica de negocio.
     * En el servicio se usan RuntimeException para casos como:
     * - "Queja no encontrada con id: X"
     * - "Estatus no encontrado: EN_REVISION"
     * Se devuelven como 400 BAD REQUEST porque son errores del cliente (ID inválido, etc.)
     */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, Object>> handleRuntimeException(RuntimeException ex) {
        return buildResponse(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    /**
     * Captura errores de validación del body de la petición.
     * Se dispara cuando un campo marcado con @NotBlank, @NotNull, etc.
     * viene vacío o nulo en el JSON del request.
     * Ejemplo: enviar { "motivoRechazo": "" } al endpoint /rechazar
     * devuelve: { "mensaje": "motivoRechazo: El motivo de rechazo es obligatorio" }
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationException(MethodArgumentNotValidException ex) {
        String mensaje = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> e.getField() + ": " + e.getDefaultMessage())
                .findFirst()
                .orElse("Error de validación");
        return buildResponse(HttpStatus.BAD_REQUEST, mensaje);
    }

    /**
     * Captura cualquier error inesperado del servidor (errores de BD,
     * NullPointerException no controlado, etc.).
     * Se devuelve 500 con un mensaje genérico para no exponer detalles internos.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleException(Exception ex) {
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Error interno del servidor");
    }

    /**
     * Construye el cuerpo estándar de respuesta de error.
     * Centralizado aquí para que todos los errores tengan exactamente el mismo formato.
     */
    private ResponseEntity<Map<String, Object>> buildResponse(HttpStatus status, String mensaje) {
        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", LocalDateTime.now().toString());
        body.put("status", status.value());
        body.put("mensaje", mensaje);
        return ResponseEntity.status(status).body(body);
    }
}
