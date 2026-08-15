package ipn.escom.defensoria.revision_service.model;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Data;

/** Una fila de "Historial de Trámites Recibidos". */
@Data
@AllArgsConstructor
public class HistorialItemModel {
    private String numeroFolio;
    private LocalDateTime fechaCreacion;
    private String nombreQuejoso;
    /** "Web" | "Manual" */
    private String tipoIngreso;
    /** "TURNADO" | "RECHAZADO" */
    private String estatusFinal;
    private String motivoRechazo;
}
