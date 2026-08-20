package ipn.escom.defensoria.primercontacto.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExpedienteTurnadoRequest {

    /*
     * Folio con el que la queja viene del área anterior.

     * Ejemplo:
     * FOL-A1B2C3D4

     * NO recibimos quejaId porque ese id pertenece
     * a la tabla del área anterior.
     */
    @NotBlank
    private String folioOrigen;

    private String tema;

    private String descripcionHechos;

    private String fechaRecepcion;

    private String prioridad;

    @Valid
    private QuejosoDTO quejoso;

    private List<EvidenciaDTO> evidencias;
}