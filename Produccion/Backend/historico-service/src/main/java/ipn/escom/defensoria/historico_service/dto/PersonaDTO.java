package ipn.escom.defensoria.historico_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Quejoso o denunciado, en la forma exacta del contrato de antecedentes. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PersonaDTO {

    /** BOLETA | EMPLEADO | CURP | INE | OTRO — puede ser null. */
    private String tipoIdentificacion;

    private String numeroIdentificacion;

    private String nombre;

    /** Primer apellido. Nunca va pegado al segundo. */
    private String apellido1;

    /** Segundo apellido. null es válido: hay gente con un solo apellido y registros
     *  históricos donde el archivo de origen no lo traía. */
    private String apellido2;

    /** ALUMNO | DOCENTE | ADMINISTRATIVO | EXTERNO — puede ser null. */
    private String tipoUsuario;
}
