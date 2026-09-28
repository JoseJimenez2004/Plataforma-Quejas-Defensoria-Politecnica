package ipn.escom.defensoria.primercontacto.dto;

import lombok.*;

import java.util.List;

/*
 * Filtros de la bandeja de análisis (CU-PC-02).
 *
 * Los campos simples (folio, prioridad, estatus, unidadAcademica) se
 * conservan por compatibilidad. Las listas permiten varios valores por
 * faceta, como los filtros de la pantalla; dentro de una misma faceta
 * se combinan con OR y entre facetas con AND. Una lista vacía o nula
 * no filtra.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FiltroExpedienteDTO {

    private String folio;
    private String nombreQuejoso;
    private String estatus;
    private String unidadAcademica;
    private String prioridad;
    private String fechaInicio;
    private String fechaFin;

    /*
     * Texto libre: busca en folio PC-, folio de la queja y nombre del quejoso.
     */
    private String texto;

    private List<String> prioridades;
    private List<String> estatusLista;
    private List<String> unidadesAcademicas;
    private List<String> temas;

    /* "recientes" (default) | "antiguos" */
    private String orden;
}
