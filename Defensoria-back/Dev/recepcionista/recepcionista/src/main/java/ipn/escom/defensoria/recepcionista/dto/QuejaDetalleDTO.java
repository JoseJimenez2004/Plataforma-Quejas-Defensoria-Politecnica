package ipn.escom.defensoria.recepcionista.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

/**
 * DTO de respuesta para la pantalla de Validación de Requisitos.
 *
 * Muestra el detalle completo de una queja para que la recepcionista
 * decida si acepta o rechaza. Incluye los datos del formulario del quejoso,
 * la verificación de campos obligatorios y la lista de documentos adjuntos.
 *
 * Campos de verificación (campos obligatorios):
 * - tieneDescripcion → descripcion_hechos no está vacía
 * - tieneTipoQueja   → tema asignado (tipo de problemática)
 * - tieneUbicacion   → tipo_documento asignado
 *
 * PENDIENTE: Cuando folio migre a PostgreSQL, agregar los campos
 * que vienen del formulario pero no están en el esquema actual:
 * asunto, fechaHechos, correo, fechaNacimiento, datos del tutor.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QuejaDetalleDTO {

    private Long id;
    private String noFolio;
    private LocalDate fechaRecepcion;

    // Datos del quejoso
    private String nombreQuejoso;
    private String boletaEmpleado;

    // Datos de la queja ingresados en el formulario
    private String descripcionHechos;
    private String tema;
    private String tipoDocumento;

    // Verificación de campos obligatorios para la pantalla de validación
    private Boolean tieneDescripcion;
    private Boolean tieneTipoQueja;
    private Boolean tieneUbicacion;

    // Nombres de los archivos adjuntos (de la tabla documentos)
    private List<String> documentosAdjuntos;

    // "Documentación Completa" o "Documentación Incompleta"
    private String estatusDocumentacion;
}
