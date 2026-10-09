package ipn.escom.defensoria.queja_service.dto;

import java.time.LocalDateTime;

import ipn.escom.defensoria.queja_service.entity.AcuerdoConciliacion;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Lo que ve el quejoso de un acuerdo de conciliación.
 *
 * Existe para NO devolver la entidad tal cual: la tabla guarda "creado_por", el correo
 * institucional de la persona de Recepción que emitió el acuerdo, y ese dato no tiene por
 * qué llegarle al quejoso. En un sistema donde se tramitan quejas por hostigamiento, saber
 * qué persona del personal atendió tu caso es información sensible, y el frontend ni
 * siquiera la muestra.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AcuerdoConciliacionModel {

    private Long id;
    private String numeroFolio;
    private String correoInstitucional;
    private String asunto;
    private String terminos;
    /** PENDIENTE | ACEPTADO | RECHAZADO */
    private String estado;
    private LocalDateTime fechaEmision;
    private LocalDateTime fechaRespuesta;
    private String comentarioQuejoso;

    public static AcuerdoConciliacionModel de(AcuerdoConciliacion acuerdo) {
        return AcuerdoConciliacionModel.builder()
                .id(acuerdo.getId())
                .numeroFolio(acuerdo.getNumeroFolio())
                .correoInstitucional(acuerdo.getCorreoInstitucional())
                .asunto(acuerdo.getAsunto())
                .terminos(acuerdo.getTerminos())
                .estado(acuerdo.getEstado())
                .fechaEmision(acuerdo.getFechaEmision())
                .fechaRespuesta(acuerdo.getFechaRespuesta())
                .comentarioQuejoso(acuerdo.getComentarioQuejoso())
                .build();
    }
}
