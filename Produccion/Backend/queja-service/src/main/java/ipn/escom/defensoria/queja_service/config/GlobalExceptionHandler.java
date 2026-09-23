package ipn.escom.defensoria.queja_service.config;

import ipn.escom.defensoria.queja_service.model.ErrorResponseModel;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

// Mismo patrón que auth-service: errores de validación (RuntimeException con mensaje) se
// devuelven como {mensaje, timestamp, codigo}, formato que el frontend ya sabe leer
// (err?.error?.mensaje) desde que se implementó el login.
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /** Nombre técnico del campo -> cómo se le llama al usuario en el mensaje de error. */
    private static final Map<String, String> ETIQUETAS = Map.ofEntries(
            Map.entry("nombre", "el nombre"),
            Map.entry("apellido1", "el primer apellido"),
            Map.entry("apellido2", "el segundo apellido"),
            Map.entry("correo", "el correo electrónico"),
            Map.entry("fechaNacimiento", "la fecha de nacimiento"),
            Map.entry("fechaHechos", "la fecha de los hechos"),
            Map.entry("numeroIdentificacion", "el número de boleta o de empleado"),
            Map.entry("unidadAcademicaClave", "el lugar de los hechos"),
            Map.entry("nombreDenunciado", "el nombre del denunciado"),
            Map.entry("apellido1Denunciado", "el primer apellido del denunciado"),
            Map.entry("apellido2Denunciado", "el segundo apellido del denunciado"),
            Map.entry("descripcion", "la descripción de los hechos"));

    /**
     * Errores al convertir lo que llegó del formulario a los tipos del DTO — por ejemplo una
     * fecha que no existe ("2004-21-19") llegando a un LocalDate.
     *
     * Antes esto caía en el manejador genérico de abajo y salía como **500 "Ocurrió un error
     * inesperado en el servidor"**, que es engañoso por partida doble: no es un error del
     * servidor sino del dato enviado, y no le dice al usuario qué corregir. Ahora responde 400
     * nombrando el campo y el valor rechazado.
     *
     * MethodArgumentNotValidException hereda de BindException, así que este único manejador
     * cubre tanto los fallos de conversión como los de Bean Validation.
     */
    @ExceptionHandler(BindException.class)
    public ResponseEntity<ErrorResponseModel> manejarErroresDeEnlace(BindException ex) {
        String mensaje = ex.getFieldErrors().stream()
                .map(this::describir)
                .collect(Collectors.joining(" "));

        if (mensaje.isBlank()) {
            mensaje = "Alguno de los datos enviados no tiene el formato esperado.";
        }

        log.warn("Datos inválidos en la petición: {}", mensaje);
        return respuesta(mensaje, HttpStatus.BAD_REQUEST);
    }

    private String describir(FieldError error) {
        String campo = ETIQUETAS.getOrDefault(error.getField(), "el campo " + error.getField());
        Object rechazado = error.getRejectedValue();

        // Los errores de conversión de tipo traen un mensaje pensado para el desarrollador
        // ("Failed to convert property value of type java.lang.String..."), inservible para
        // quien está llenando el formulario. Se reemplaza por uno legible.
        if (error.contains(org.springframework.beans.TypeMismatchException.class)) {
            return "El valor de " + campo + " no es válido: \"" + rechazado + "\".";
        }
        String detalle = error.getDefaultMessage();
        return detalle == null || detalle.isBlank()
                ? "Revisa " + campo + "."
                : "Revisa " + campo + ": " + detalle;
    }

    /**
     * Archivo por encima del tope de multipart. Sin esto sale un 500 genérico, cuando en
     * realidad el usuario solo necesita saber que su archivo pesa demasiado.
     */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ErrorResponseModel> manejarArchivoDemasiadoGrande(
            MaxUploadSizeExceededException ex) {
        log.warn("Carga rechazada por tamaño: {}", ex.getMessage());
        return respuesta(
                "Alguno de los archivos supera el tamaño permitido. El máximo es 30 MB por archivo "
                        + "(3 MB para la identificación oficial) y 100 MB en total.",
                HttpStatus.PAYLOAD_TOO_LARGE);
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ErrorResponseModel> manejarRuntimeException(RuntimeException ex) {
        // Antes esto no se registraba en ningún lado: la excepción se atrapaba aquí y solo se
        // mandaba la respuesta al cliente, así que en los logs del contenedor nunca aparecía
        // rastro de que algo había fallado. Con esto sí queda visible en "podman logs".
        log.warn("Solicitud inválida ({}): {}", ex.getClass().getSimpleName(), ex.getMessage());
        return respuesta(ex.getMessage(), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponseModel> manejarErroresGenerales(Exception ex) {
        // Este es el handler que atrapaba los 500 silenciosamente. Ahora imprime el stack
        // trace completo para poder diagnosticar la causa real la próxima vez que pase.
        log.error("Error no controlado procesando la petición", ex);
        return respuesta("Ocurrió un error inesperado en el servidor.",
                HttpStatus.INTERNAL_SERVER_ERROR);
    }

    private ResponseEntity<ErrorResponseModel> respuesta(String mensaje, HttpStatus estado) {
        return new ResponseEntity<>(
                new ErrorResponseModel(mensaje, LocalDateTime.now(), estado.value()), estado);
    }
}
