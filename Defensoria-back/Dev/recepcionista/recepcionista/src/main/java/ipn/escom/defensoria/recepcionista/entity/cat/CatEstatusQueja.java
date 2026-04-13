package ipn.escom.defensoria.recepcionista.entity.cat;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Entidad de catálogo que mapea la tabla "cat_estatus_queja".
 *
 * Contiene los posibles estados de una queja en el sistema.
 * Los valores se insertan en init.sql y no se modifican desde el código:
 *   - RECIBIDA   → queja registrada, pendiente de revisión por recepcionista
 *   - EN_REVISION → queja aceptada, en manos de un abogado
 *   - ATENDIDA   → queja resuelta o rechazada formalmente
 *
 * Es de solo lectura desde este microservicio — la recepcionista cambia
 * el estatus de las quejas pero no modifica este catálogo.
 */
@Entity
@Table(name = "cat_estatus_queja")
@Data
@NoArgsConstructor
public class CatEstatusQueja {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(unique = true, nullable = false, length = 50)
    private String nombre;

    @Column(length = 255)
    private String descripcion;
}
