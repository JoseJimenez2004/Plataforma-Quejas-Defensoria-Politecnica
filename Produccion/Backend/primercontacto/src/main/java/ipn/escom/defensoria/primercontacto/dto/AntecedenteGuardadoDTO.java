package ipn.escom.defensoria.primercontacto.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

/*
 * Antecedente elegido como final para un expediente. Sirve de entrada (lo que el analista
 * seleccionó en pantalla) y de salida (lo ya guardado, con quién y cuándo).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AntecedenteGuardadoDTO {

    private Long id;

    /* SISTEMA o HISTORICO. */
    @NotBlank(message = "Falta el origen del antecedente.")
    private String origen;

    @NotBlank(message = "Falta el folio del antecedente.")
    private String folioQueja;

    /* MANUAL, MODELO o REGLAS_PROVISIONAL. */
    @NotBlank(message = "Falta la fuente del antecedente.")
    private String fuente;

    private Integer similitud;
    private String asunto;
    private String fecha;
    private String nombreQuejoso;
    private String nombreDenunciado;
    private String unidadAcademica;
    private String estatus;
    private String folioPrimerContacto;
    private String extracto;
    private String descripcion;
    private String resultado;

    private String analistaNombre;
    private String fechaRegistro;
}
