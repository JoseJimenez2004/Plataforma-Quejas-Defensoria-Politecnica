package ipn.escom.defensoria.subdefensoria.dto;

import lombok.*;

/**
 * Fila de la bandeja unificada de expedientes: cubre todos los
 * estatus (RECIBIDO, EN_INVESTIGACION, EN_GESTION_DIRECTOR,
 * LISTO_A_DICTAMINAR, CONCLUIDO) en un solo lugar, con filtros en el
 * front en vez de bandejas separadas por fase.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExpedienteResumenDTO {

    private Long expedienteId;
    private String folio;
    private String quejosoNombre;
    private String asunto;
    private String unidadAcademica;
    private String fechaAdmision;
    private String estatus;

    /** Info del oficio vigente cuando el expediente esta EN_INVESTIGACION o EN_GESTION_DIRECTOR; null en otro caso. */
    private Long oficioIdVigente;
    private String numeroOficioVigente;
    private String destinatarioNombreVigente;
    private String faseOficioVigente;
    private String estatusOficioVigente;
    private Long diasTranscurridos;
    private Integer diasLimite;
}
