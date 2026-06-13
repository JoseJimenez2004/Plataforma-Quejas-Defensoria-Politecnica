package ipn.escom.defensoria.administracion.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record LoginRequestDTO(
    @NotBlank(message = "El correo es obligatorio")
    @Email(message = "Formato de correo inválido")
    String correo,
    
    @NotBlank(message = "La contraseña es obligatoria")
    String password
) {}