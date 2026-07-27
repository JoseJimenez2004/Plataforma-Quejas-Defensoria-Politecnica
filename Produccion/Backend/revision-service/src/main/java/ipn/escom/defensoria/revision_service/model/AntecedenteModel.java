package ipn.escom.defensoria.revision_service.model;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Data;

/** Una fila de "Historial del Usuario en el Sistema" en Búsqueda de Antecedentes. */
@Data
@AllArgsConstructor
public class AntecedenteModel {
    private String numeroFolio;
    private LocalDateTime fecha;
    private String asunto;
    private String estadoActual;
}
