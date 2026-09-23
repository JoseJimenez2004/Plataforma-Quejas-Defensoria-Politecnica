package ipn.escom.defensoria.historico_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Forma EXACTA del contrato de antecedentes (docs/CONTRATO-ANTECEDENTES-v1.md), la misma
 * que entrega queja-service para las quejas del sistema. Que ambos servicios devuelvan
 * esta estructura es lo que le permite al modelo consumir las dos fuentes sin distinguirlas
 * más allá del campo "origen".
 *
 * Los campos internos de captura (capturadoPor, fechaCaptura, notasCaptura) NO se exponen
 * aquí a propósito: son trazabilidad nuestra, no señal para el modelo.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuejaHistoricaDTO {

    private String folio;

    /** Siempre "HISTORICO" desde este servicio. */
    private String origen;

    /** EXCEL | LLAMADA | OFICIO_FISICO | SISTEMA_ANTERIOR | OTRO */
    private String fuente;

    /**
     * Cuándo se presentó ante la Defensoría, NO cuándo se capturó. Si aquí fuera la fecha de
     * captura, todos los históricos parecerían de 2026 y el orden temporal de los
     * antecedentes quedaría inservible.
     */
    private String fechaRegistro;

    private String fechaHechos;

    private String unidadAcademicaClave;
    private String motivo;
    private String descripcion;

    private String estatus;
    private String resultado;

    private PersonaDTO quejoso;
    private PersonaDTO denunciado;
}
