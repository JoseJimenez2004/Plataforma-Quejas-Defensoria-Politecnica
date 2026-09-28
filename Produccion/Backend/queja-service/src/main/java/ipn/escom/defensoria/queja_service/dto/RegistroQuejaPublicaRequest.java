package ipn.escom.defensoria.queja_service.dto;

import java.time.LocalDate;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.format.annotation.DateTimeFormat.ISO;
import org.springframework.web.multipart.MultipartFile;

import lombok.Data;

/**
 * Datos que manda el formulario público (registro-queja-publico), sin necesidad de sesión
 * iniciada. Se recibe como multipart/form-data (@ModelAttribute), no JSON, para poder incluir
 * los archivos de evidencia en la misma petición — mismo patrón que ya usaba /registrar.
 *
 * Las validaciones NO viven aquí como anotaciones: están en
 * validacion/ValidadorQuejaPublica, porque varias son reglas de negocio (rango de fecha de
 * nacimiento, reglas de correo por dominio, tipos de archivo por firma binaria) que no se
 * expresan con Bean Validation y que además necesitan normalizar el valor, no solo aceptarlo
 * o rechazarlo.
 */
@Data
public class RegistroQuejaPublicaRequest {

    // Datos del quejoso
    private String nombre;
    private String apellido1;
    private String apellido2;
    private String correo;

    @DateTimeFormat(iso = ISO.DATE)
    private LocalDate fechaNacimiento;

    /** "alumno" | "empleado" */
    private String tipoIdentificacion;
    private String numeroIdentificacion;

    // Datos de la queja
    private String unidadAcademicaClave;

    @DateTimeFormat(iso = ISO.DATE)
    private LocalDate fechaHechos;

    private String nombreDenunciado;
    private String apellido1Denunciado;
    /** Segundo apellido del denunciado — opcional: el quejoso puede no conocerlo. */
    private String apellido2Denunciado;
    private String descripcion;
    private List<MultipartFile> archivos;

    // Aviso de privacidad — el formulario obliga a leerlo hasta el final antes de habilitar
    // la casilla, y el backend rechaza la queja si no llega en true.
    private Boolean avisoPrivacidadAceptado;
    /** Versión del aviso que aceptó el quejoso (queda registrada con la queja). */
    private String avisoPrivacidadVersion;

    // Datos del tutor — solo presentes cuando el quejoso es menor de edad
    private String tutorNombre;
    private String tutorApellido1;
    private String tutorApellido2;
    private String tutorParentesco;
    private String tutorCorreo;
    private String tutorTelefono;

    public boolean tieneTutor() {
        return tutorNombre != null && !tutorNombre.isBlank();
    }
}
