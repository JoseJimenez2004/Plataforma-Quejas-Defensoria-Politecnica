package ipn.escom.defensoria.quejoso.dto;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class QuejaRegistroDTO {
    // Datos del Quejoso (Identificación institucional)
    private String nombreQuejoso;
    private String primerApellido;
    private String segundoApellido;
    private String correo;
    private String fechaNacimiento; // Para validar minoría de edad
    private String identificacionInstitucional; // Boleta o No. Empleado
    private String tipoIdentificacion; // Alumno o Empleado

    // Datos de la Queja
    private String unidadAcademica;
    private LocalDateTime fechaHechos;
    private String asunto;
    private String descripcionHechos;

    // Datos del Denunciado
    private String nombreDenunciado;
    private String primerApellidoDenunciado;

    // Datos del Tutor (Opcional, solo si es menor de edad - MQ-03)
    private TutorDTO tutor;
}