package ipn.escom.defensoria.historico_service.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Lo que manda el panel de Recepción al capturar una queja de años pasados.
 *
 * "folio" es opcional: si el expediente no traía folio (típico de las que entraron por
 * llamada), el servicio genera uno con prefijo SF- y lo marca como generado, para que se
 * distinga de un folio real.
 */
@Data
public class CapturaQuejaHistoricaRequest {

    private String folio;

    @NotBlank(message = "Indica de dónde salió el expediente (Excel, llamada, oficio físico…)")
    private String fuente;

    private LocalDate fechaPresentacionOriginal;
    private LocalDate fechaHechos;

    private String unidadAcademicaClave;
    private String motivo;

    @NotBlank(message = "La descripción de los hechos es obligatoria")
    private String descripcion;

    private String estatus;
    private String resultado;

    private String quejosoTipoIdentificacion;
    private String quejosoNumeroIdentificacion;
    @NotBlank(message = "El nombre del quejoso es obligatorio")
    private String quejosoNombre;
    @NotBlank(message = "El primer apellido del quejoso es obligatorio")
    private String quejosoApellido1;
    private String quejosoApellido2;
    private String quejosoTipoUsuario;

    private String denunciadoTipoIdentificacion;
    private String denunciadoNumeroIdentificacion;
    private String denunciadoNombre;
    private String denunciadoApellido1;
    private String denunciadoApellido2;
    private String denunciadoTipoUsuario;

    private String notasCaptura;
}
