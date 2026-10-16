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
@Builder(toBuilder = true)
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

    /* Para reconocer al mismo quejoso aunque el nombre venga escrito distinto. */
    private String correoQuejoso;
    private String identificacionQuejoso;

    // ---- Modelo V5 ----

    private boolean mismoDenunciado;

    /* true si la coincidencia de persona es por número de identificación (no solo por nombre). */
    private boolean identidadVerificada;

    /* Cómo coincidió la persona, ej. "Identificación del quejoso + Nombre del denunciado (por verificar)". */
    private String tipoCoincidencia;

    /* Alta / Moderada / Baja, según la similitud de los hechos. */
    private String nivelSimilitud;

    /* Solo coincidencias fuertes: por qué el modelo la marcó como prioritaria. */
    private String razonCoincidencia;

    /* Días entre la fecha de los hechos de ambas quejas, si las dos la tienen. */
    private Integer diasDiferencia;
}
