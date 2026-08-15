package ipn.escom.defensoria.subdefensoria.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RespuestaExternaDTO {

    private Long id;
    private Long expedienteId;
    private Long oficioId;
    private String folio;
    private String canalRecepcion;
    private String numeroOficioRespuestaUA;
    private String archivoPdfPath;
    private String resumen;
    private String fechaRecepcion;
    /** Estatus del expediente despues de registrar esta respuesta (normalmente LISTO_A_DICTAMINAR). */
    private String estatusExpediente;
}
