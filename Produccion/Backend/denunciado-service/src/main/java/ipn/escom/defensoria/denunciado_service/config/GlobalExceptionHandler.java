package ipn.escom.defensoria.denunciado_service.config;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import ipn.escom.defensoria.denunciado_service.validacion.ValidacionException;

/** Mismo formato {mensaje, timestamp, codigo} que el resto: el frontend lee err.error.mensaje. */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ValidacionException.class)
    public ResponseEntity<Map<String, Object>> validacion(ValidacionException ex) {
        log.warn("Datos inválidos: {}", ex.getMessage());
        return respuesta(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<Map<String, Object>> tamanio(MaxUploadSizeExceededException ex) {
        log.warn("Carga rechazada por tamaño: {}", ex.getMessage());
        return respuesta(HttpStatus.PAYLOAD_TOO_LARGE,
                "Los archivos superan el tamaño permitido (30 MB por archivo, 100 MB en total).");
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, Object>> sinPermiso(AccessDeniedException ex) {
        return respuesta(HttpStatus.FORBIDDEN, "No tienes permiso para consultar esta información.");
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> general(Exception ex) {
        log.error("Error no controlado procesando la petición", ex);
        return respuesta(HttpStatus.INTERNAL_SERVER_ERROR, "Ocurrió un error inesperado. Intenta de nuevo.");
    }

    private ResponseEntity<Map<String, Object>> respuesta(HttpStatus estado, String mensaje) {
        Map<String, Object> cuerpo = new LinkedHashMap<>();
        cuerpo.put("mensaje", mensaje);
        cuerpo.put("timestamp", LocalDateTime.now().toString());
        cuerpo.put("codigo", estado.value());
        return ResponseEntity.status(estado).body(cuerpo);
    }
}
