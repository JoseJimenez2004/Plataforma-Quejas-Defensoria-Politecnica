package ipn.escom.defensoria.cuenta.dto;

import lombok.Builder;
import lombok.Data;

/**
 * DTO para enviar datos al Frontend de forma segura.
 * NO incluye el campo 'password'.
 */
@Data
@Builder // Facilita la creación del objeto desde la Entity
public class UsuarioResponse {

    private Long id;
    private String nombre;
    private String apellidoPaterno;
    private String boletaEmpleado;
    private String unidadAcademica;
    private String correo;
    
    private String mensaje;
}