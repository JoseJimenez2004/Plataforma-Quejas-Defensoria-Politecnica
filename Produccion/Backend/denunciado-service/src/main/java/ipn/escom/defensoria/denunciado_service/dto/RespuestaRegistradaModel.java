package ipn.escom.defensoria.denunciado_service.dto;

import java.time.LocalDateTime;

/** Acuse que se le devuelve al denunciado al registrar su respuesta. */
public record RespuestaRegistradaModel(String folioRespuesta, String folioQueja, String nombreCompleto,
                                       LocalDateTime fechaRegistro, int credenciales, int evidencias) {
}
