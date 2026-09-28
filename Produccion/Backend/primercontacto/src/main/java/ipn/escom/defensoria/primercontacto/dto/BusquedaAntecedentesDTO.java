package ipn.escom.defensoria.primercontacto.dto;

import lombok.*;

import java.util.List;

/*
 * Resultado de buscar antecedentes de un expediente.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BusquedaAntecedentesDTO {

    /* Folio PC-... del expediente analizado. */
    private String folio;
    private String folioQueja;

    /* Qué motor produjo el resultado, ej. REGLAS_PROVISIONAL o MODELO. */
    private String motor;
    private String descripcionMotor;

    private String generadoEn;
    private int quejasAnalizadas;

    private List<AntecedenteDTO> resultados;
}
