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
 */
@Data
public class RegistroQuejaPublicaRequest {

    // Datos del quejoso
    private String nombre;
    private String apellidoPaterno;
    private String apellidoMaterno;
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
    private String apellidoDenunciado;
    private String descripcion;
    private List<MultipartFile> archivos;

    // Datos del tutor — solo presentes cuando el quejoso es menor de edad
    private String tutorNombre;
    private String tutorApellidoPaterno;
    private String tutorApellidoMaterno;
    private String tutorParentesco;
    private String tutorCorreo;
    private String tutorTelefono;

    public boolean tieneTutor() {
        return tutorNombre != null && !tutorNombre.isBlank();
    }
}
