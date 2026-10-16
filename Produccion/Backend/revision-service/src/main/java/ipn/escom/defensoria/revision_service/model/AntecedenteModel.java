package ipn.escom.defensoria.revision_service.model;

import java.time.LocalDateTime;

/**
 * Un antecedente de la queja que se va a turnar (2026-10-16).
 *
 * @param relacion         "MISMA_PERSONA" (misma boleta/número o correo del quejoso) o
 *                         "MISMO_DENUNCIADO" (misma persona señalada)
 * @param defensorAsignado abogado que la lleva o la llevó (null si no se ha turnado)
 * @param enCurso          true si sigue en trámite (no concluida, improcedente, remitida ni rechazada)
 */
public record AntecedenteModel(
        String numeroFolio,
        LocalDateTime fecha,
        String asunto,
        String estadoActual,
        String relacion,
        String unidadAcademicaClave,
        String defensorAsignado,
        boolean enCurso) {
}
