package ipn.escom.defensoria.primercontacto.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.DynamicUpdate;

/**
 * Espejo MÍNIMO de la tabla compartida "quejas" (dueño: queja-service / revision-service).
 *
 * Primer Contacto solo necesita dos cosas de ahí:
 *   - el correo del quejoso, para dirigirle acuerdos de conciliación exactamente igual que
 *     lo hace revision-service (el quejoso los ve por ese correo en su panel);
 *   - escribir quejas.estatus cuando avanza el flujo, para que todos los módulos vean el
 *     mismo estado (ver SincronizacionQuejaService).
 *
 * Todo lo demás es de solo lectura, y @DynamicUpdate garantiza que el UPDATE toque
 * únicamente la columna estatus, sin pisar lo que otros servicios escriben en la fila.
 */
@Getter
@Setter
@Entity
@DynamicUpdate
@Table(name = "quejas")
public class QuejaReferencia {

    @Id
    private Long id;

    @Column(name = "numero_folio", insertable = false, updatable = false)
    private String numeroFolio;

    @Column(name = "correo_institucional", insertable = false, updatable = false)
    private String correoInstitucional;

    @Column(name = "estatus")
    private String estatus;
}
