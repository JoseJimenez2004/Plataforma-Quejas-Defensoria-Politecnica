package ipn.escom.defensoria.queja_service.dto;

import java.time.LocalDate;
import lombok.Data;

/** Body de PUT /api/quejoso/quejas/mias/{folio} -- campos que el propio quejoso puede seguir
 * editando MIENTRAS su queja siga en estatus "RECIBIDA" (ver QuejaService.editarMiQueja). No
 * se permite cambiar identidad del quejoso ni el folio. */
@Data
public class EditarQuejaRequest {
    private String descripcion;
    private String unidadAcademicaClave;
    private LocalDate fechaHechos;
    private String nombreDenunciado;
    private String apellidoDenunciado;
}
