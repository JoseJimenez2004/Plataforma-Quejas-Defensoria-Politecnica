package ipn.escom.defensoria.primercontacto.dto;

import lombok.*;

import java.util.List;

/*
 * Una queja previa que podría ser antecedente de la que se analiza.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AntecedenteDTO {

    /* Folio de la queja previa (FOL-...). */
    private String folioQueja;

    /* Si también pasó por Primer Contacto, su folio PC-... (para abrirla). */
    private String folioPrimerContacto;

    private String fecha;
    private String asunto;
    private String extracto;
    private String unidadAcademica;
    private String nombreQuejoso;
    private String estatus;

    /* 0-100: qué tanto se parece según el motor de búsqueda. */
    private int similitud;

    /* Por qué salió: "Mismo quejoso", "Misma unidad académica", "Hechos similares"... */
    private List<String> coincidencias;

    private boolean mismoQuejoso;

    /* De dónde sale: SISTEMA (quejas de defensoria_db) o HISTORICO (casos históricos). */
    private String origen;

    private String nombreDenunciado;

    /* Narrativa completa, para el resumen. El extracto es solo para la tarjeta. */
    private String descripcion;

    /* Solo históricos: cómo terminó el caso. */
    private String resultado;
}
