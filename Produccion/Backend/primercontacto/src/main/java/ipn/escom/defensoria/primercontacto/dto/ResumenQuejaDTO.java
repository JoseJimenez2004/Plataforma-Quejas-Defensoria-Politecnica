package ipn.escom.defensoria.primercontacto.dto;

import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

/** Borrador de resumen de una queja que genera resumen-service (D9/MMR + editor Qwen). */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ResumenQuejaDTO {

    /* Borrador redactado por Qwen a partir de los fragmentos. */
    private String resumen;

    /* Fragmentos de la queja que eligió D9, tal cual los escribió el quejoso. */
    private List<String> fragmentos;

    /* Siempre true: es una sugerencia y requiere revisión humana. */
    private boolean requiereRevision;

    private String versionPipeline;

    /* Cuándo y quién lo generó (null si no está guardado por folio). */
    private LocalDateTime fechaGeneracion;

    private String generadoPor;
}
