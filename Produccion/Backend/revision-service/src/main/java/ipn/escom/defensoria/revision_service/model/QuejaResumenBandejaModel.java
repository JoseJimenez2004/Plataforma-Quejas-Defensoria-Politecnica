package ipn.escom.defensoria.revision_service.model;

import java.time.LocalDateTime;

/**
 * Una fila de la cola de la Bandeja de Entrada.
 *
 * @param documentacionAparenteCompleta true si tiene al menos un archivo adjunto y los datos
 *        mínimos -- solo es una señal visual; la validación real la hace la recepcionista.
 * @param revisandoPorNombre quién la tiene abierta (solo cuando estatus es EN_VALIDACION)
 * @param esMia              true si quien consulta es quien la tiene abierta
 * @param origen             WEB (formulario público) o MANUAL (capturada en ventanilla)
 * @param rechazadaAntesPor  en una queja CORREGIDA, quién la había regresado al quejoso
 */
public record QuejaResumenBandejaModel(
        String numeroFolio,
        LocalDateTime fechaCreacion,
        String nombreQuejoso,
        String unidadAcademicaClave,
        boolean documentacionAparenteCompleta,
        String estatus,
        String revisandoPorNombre,
        boolean esMia,
        String origen,
        String rechazadaAntesPor) {
}
