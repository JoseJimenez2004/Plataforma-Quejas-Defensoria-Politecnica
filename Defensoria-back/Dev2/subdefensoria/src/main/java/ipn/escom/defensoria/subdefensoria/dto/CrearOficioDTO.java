package ipn.escom.defensoria.subdefensoria.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

/**
 * Payload para redactar y enviar un oficio (sirve tanto para TS-01
 * "solicitud de informacion" como para TS-04 "oficio al director"):
 * el service decide la fase segun el estatus actual del expediente.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CrearOficioDTO {

    @NotNull
    private Long expedienteId;

    @NotBlank
    private String destinatarioNombre;

    @NotBlank
    @Email
    private String destinatarioCorreo;

    private String unidadAcademica;

    @NotBlank
    private String contenidoRedactado;
}
