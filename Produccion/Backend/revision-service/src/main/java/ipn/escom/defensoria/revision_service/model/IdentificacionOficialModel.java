package ipn.escom.defensoria.revision_service.model;

/**
 * Un documento del catálogo de identificación (2026-10-16).
 *
 * @param grupo   "Identificación oficial", "Identificación institucional (IPN)" o "Representación"
 * @param detalle texto corto de apoyo para la recepcionista
 */
public record IdentificacionOficialModel(String clave, String nombre, String grupo, String detalle) {
}
