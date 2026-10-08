package ipn.escom.defensoria.primercontacto.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/*
 * Selección de antecedentes finales que el analista guarda de una vez, aunque los haya
 * marcado en pestañas distintas (búsqueda manual y modelo).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class GuardarAntecedentesDTO {

    @NotEmpty(message = "Selecciona al menos un antecedente.")
    private List<@Valid AntecedenteGuardadoDTO> antecedentes;
}
