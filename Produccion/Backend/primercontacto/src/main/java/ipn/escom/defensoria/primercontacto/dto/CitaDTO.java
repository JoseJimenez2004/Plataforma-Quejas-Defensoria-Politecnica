package ipn.escom.defensoria.primercontacto.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CitaDTO {

    private Long id;
    private Long expedienteId;
    private String folio;
    private Long quejosoId;
    private String quejosoNombre;
    private Long analistaId;
    private String analistaNombre;
    private String fechaCita;
    private String horaCita;
    private String tipoCita;
    private String motivo;
    private String estatus;
    private String fechaCreacion;

    /* Último analista que confirmó, reagendó o canceló. */
    private String actualizadoPorNombre;
    private String fechaActualizacion;

    /* Folio de la queja (FOL-...), el que conoce el quejoso. */
    private String folioQueja;

    /* Respuesta del quejoso a la cita. */
    private String fechaLimiteRespuesta;
    private String fechaRespuestaQuejoso;
    private String motivoCancelacionQuejoso;
    private String respuestaRegistradaPor;
}