package ipn.escom.defensoria.denunciado_service.dto;

import java.time.LocalDateTime;
import java.util.List;

/** Lo que ve el personal de la Defensoría al consultar las respuestas de una queja. */
public record RespuestaDetalleModel(Long id, String folioRespuesta, String folioQueja, String nombre,
                                    String apellido1, String apellido2, String unidadProcedenciaClave,
                                    String tipoIdentificacion, String numeroIdentificacion,
                                    String descripcionHechos, String estatus, LocalDateTime fechaRegistro,
                                    String avisoPrivacidadVersion, List<ArchivoResumen> archivos) {
}
