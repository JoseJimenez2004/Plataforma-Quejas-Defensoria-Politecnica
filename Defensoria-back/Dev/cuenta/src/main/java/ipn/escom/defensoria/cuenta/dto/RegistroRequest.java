package ipn.escom.defensoria.cuenta.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * DTO para capturar todos los campos del formulario "Crear Cuenta de Usuario".
 * Esto separa la capa de entrada de la Entidad de Base de Datos.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RegistroRequest {

    private String nombre;
    private String apellidoPaterno;
    private String apellidoMaterno;
    private Integer edad;
    
    // Identidad Politécnica
    private String boletaEmpleado;
    private String unidadAcademica;
    
    // Datos de Acceso
    private String correo;
    private String password;
    private String confirmarPassword; 

}