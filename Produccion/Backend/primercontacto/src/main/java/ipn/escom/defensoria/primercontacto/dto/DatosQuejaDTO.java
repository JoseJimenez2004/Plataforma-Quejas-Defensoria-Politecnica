package ipn.escom.defensoria.primercontacto.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Datos de la queja original (tabla quejas) que el analista necesita ver en el detalle del
 * expediente: los mismos que ve el quejoso en su panel. Solo lectura.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DatosQuejaDTO {

    private String fechaRegistro;
    private String asunto;
    private String fechaHechos;
    private String unidadAcademicaClave;

    private String tipoIdentificacionQuejoso;
    private String numeroIdentificacionQuejoso;
    private String fechaNacimientoQuejoso;

    /* Nombre completo de la persona denunciada; null si la queja no lo trae. */
    private String denunciado;
}
