package ipn.escom.defensoria.recepcionista.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * DTO de respuesta para la tabla de resultados en la pantalla
 * de Búsqueda de Antecedentes y Canalización.
 *
 * Muestra el historial de quejas previas de un quejoso buscado por nombre,
 * permitiendo a la recepcionista identificar si ya tiene casos abiertos
 * o cerrados antes de proceder con la canalización.
 *
 * Campos:
 * - idQueja → no_folio de la queja (ej. QJ-001-2023, TEMP-001)
 * - fecha   → fecha_recepcion de la queja
 * - estatus → nombre del estatus actual (RECIBIDA, EN_REVISION, ATENDIDA)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QuejosoBusquedaResultDTO {

    private String idQueja;
    private LocalDate fecha;
    private String estatus;
}
