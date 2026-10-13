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

    /*
     * Antecedentes del mismo quejoso y/o denunciado (lo mismo que porInvolucrados). Se
     * conserva para la búsqueda manual y para clientes anteriores.
     */
    private List<AntecedenteDTO> resultados;

    /* Solo búsqueda con el modelo: las tres listas del modelo V5. */
    private List<AntecedenteDTO> porHechos;
    private List<AntecedenteDTO> porInvolucrados;
    private List<AntecedenteDTO> coincidenciasFuertes;

    /* Avisos para el analista: el modelo no respondió, el histórico no está disponible... */
    private List<String> avisos;
}
