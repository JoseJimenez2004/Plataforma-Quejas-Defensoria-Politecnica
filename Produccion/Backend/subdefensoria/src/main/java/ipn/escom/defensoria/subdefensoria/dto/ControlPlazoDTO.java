package ipn.escom.defensoria.subdefensoria.dto;

import lombok.*;

/**
 * Fila de la pantalla P14 "Seguimiento de Etapa de Investigacion -
 * El Semaforo". Refleja el oficio vigente (el mas reciente sin
 * respuesta) de cada expediente que ya entro a investigacion.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ControlPlazoDTO {

    private Long expedienteId;
    private String folio;
    private String unidadAcademica;
    private Long oficioId;
    private String numeroOficio;
    private String fase;
    /** EN_ESPERA, VENCIDO o LISTO_A_DICTAMINAR (cuando ya no hay oficio pendiente). */
    private String estatusOficio;
    private long diasTranscurridos;
    private int diasLimite;
}
