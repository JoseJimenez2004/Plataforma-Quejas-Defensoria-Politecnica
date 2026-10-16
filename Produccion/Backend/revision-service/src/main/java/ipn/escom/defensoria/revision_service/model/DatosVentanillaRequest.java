package ipn.escom.defensoria.revision_service.model;

/** Datos del documento físico de una queja capturada en ventanilla (2026-10-16). */
public record DatosVentanillaRequest(
        String tipoDocumento,
        String numeroOficio,
        String fechaRecepcionFisica,
        String ubicacionFisica) {
}
