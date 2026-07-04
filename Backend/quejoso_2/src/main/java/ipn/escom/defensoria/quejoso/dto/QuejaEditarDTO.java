package ipn.escom.defensoria.quejoso.dto;

import lombok.Data;
import java.util.List;

@Data
public class QuejaEditarDTO {
    private String asunto;
    private String descripcionHechos;
    // Lista de IDs de evidencias que el usuario desea eliminar
    private List<Long> evidenciasBorrarIds;
}