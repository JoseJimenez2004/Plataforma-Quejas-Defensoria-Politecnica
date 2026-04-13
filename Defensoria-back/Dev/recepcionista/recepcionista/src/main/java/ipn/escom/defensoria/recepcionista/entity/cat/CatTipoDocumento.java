package ipn.escom.defensoria.recepcionista.entity.cat;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Entidad de catálogo que mapea la tabla "cat_tipo_documento".
 *
 * Indica la naturaleza formal del documento que da origen a la queja.
 * Ejemplos: Queja formal, Denuncia, Solicitud de orientación.
 *
 * La recepcionista revisa este campo en la pantalla de Validación
 * para determinar el tipo de trámite que corresponde.
 */
@Entity
@Table(name = "cat_tipo_documento")
@Data
@NoArgsConstructor
public class CatTipoDocumento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(unique = true, nullable = false, length = 50)
    private String nombre;
}
