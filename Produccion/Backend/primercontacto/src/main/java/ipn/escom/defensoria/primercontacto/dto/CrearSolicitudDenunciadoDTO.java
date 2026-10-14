package ipn.escom.defensoria.primercontacto.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CrearSolicitudDenunciadoDTO {

    @NotBlank(message = "Escribe el correo del denunciado.")
    @Email(message = "El correo del denunciado no es válido.")
    private String correo;
}
