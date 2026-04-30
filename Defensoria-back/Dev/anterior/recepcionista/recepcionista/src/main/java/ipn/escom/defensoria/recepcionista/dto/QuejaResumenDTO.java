package ipn.escom.defensoria.recepcionista.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * DTO de respuesta para el Dashboard de Recepción.
 *
 * Contiene solo los campos necesarios para mostrar cada fila
 * en la tabla del panel principal. No expone información sensible
 * ni datos innecesarios al frontend.
 *
 * Campos:
 * - id              → ID interno de la queja (para navegar al detalle)
 * - noFolio         → Identificador visible (ej. TEMP-001)
 * - fechaRecepcion  → Fecha en que se registró la queja
 * - nombreQuejoso   → Nombre completo del quejoso
 * - documentacionCompleta → true si tiene descripción, tema y tipo de documento
 * - estatus         → Nombre del estatus actual (RECIBIDA, EN_REVISION, etc.)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QuejaResumenDTO {

    private Long id;
    private String noFolio;
    private LocalDate fechaRecepcion;
    private String nombreQuejoso;
    private Boolean documentacionCompleta;
    private String estatus;
}
