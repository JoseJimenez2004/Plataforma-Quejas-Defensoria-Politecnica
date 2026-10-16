package ipn.escom.defensoria.primercontacto.dto;

import lombok.*;

/*
 * Analista autenticado según el JWT. El front lo usa para saber qué
 * notas son suyas (CU-PC-05) sin tener que decodificar el token.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AnalistaSesionDTO {

    private Long id;
    private String nombreCompleto;
    private String correo;
}
