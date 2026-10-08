package ipn.escom.defensoria.primercontacto.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/*
 * Cancelación de una cita por parte del quejoso: el motivo es obligatorio para que el
 * analista pueda proponer una nueva fecha.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CancelacionCitaDTO {

    @NotBlank(message = "Indica el motivo de la cancelación.")
    @Size(max = 1000, message = "El motivo no puede exceder 1000 caracteres.")
    private String motivo;
}
