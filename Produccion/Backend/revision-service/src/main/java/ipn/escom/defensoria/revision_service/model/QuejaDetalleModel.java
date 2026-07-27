package ipn.escom.defensoria.revision_service.model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import ipn.escom.defensoria.revision_service.dto.EvidenciaResumen;
import lombok.AllArgsConstructor;
import lombok.Data;

/** Detalle completo de una queja para las pantallas "Validación de Requisitos" y
 * "Búsqueda de Antecedentes y Turnado". */
@Data
@AllArgsConstructor
public class QuejaDetalleModel {
    private String numeroFolio;
    private LocalDateTime fechaCreacion;
    private String nombreCompletoQuejoso;
    private String correoInstitucional;
    private String tipoIdentificacionQuejoso;
    private String numeroIdentificacionQuejoso;
    private String motivo;
    private String descripcion;
    private String unidadAcademicaClave;
    private LocalDate fechaHechos;
    private String nombreCompletoDenunciado;
    private String origenRegistro;
    private String estatus;
    private List<EvidenciaResumen> evidencias;

    // Presentes solo si la queja ya fue procesada -- para revisitarla desde el historial.
    private String motivoRechazo;
    private String areaTurnada;
    private String defensorAsignado;
    private String comentariosRecepcion;
}
