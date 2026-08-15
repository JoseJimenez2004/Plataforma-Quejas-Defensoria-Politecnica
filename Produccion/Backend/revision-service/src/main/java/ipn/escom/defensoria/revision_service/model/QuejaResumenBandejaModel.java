package ipn.escom.defensoria.revision_service.model;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Data;

/** Una fila de la tabla "Panel de Gestión de Recepción" en la Bandeja de Entrada. */
@Data
@AllArgsConstructor
public class QuejaResumenBandejaModel {
    private String numeroFolio;
    private LocalDateTime fechaCreacion;
    private String nombreQuejoso;
    /** true si tiene al menos una evidencia adjunta Y los datos mínimos del quejoso -- solo
     * es una señal visual (✓/✗) en la bandeja; la validación real la hace el recepcionista a
     * mano en "Validación de Requisitos". */
    private boolean documentacionAparenteCompleta;
    private String estatus;
}
