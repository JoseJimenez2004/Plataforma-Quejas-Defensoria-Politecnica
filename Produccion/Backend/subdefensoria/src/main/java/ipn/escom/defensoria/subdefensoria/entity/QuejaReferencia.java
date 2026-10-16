package ipn.escom.defensoria.subdefensoria.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.DynamicUpdate;

/**
 * Espejo mínimo de la tabla quejas (dueño: queja-service / revision-service), solo para
 * reflejar en quejas.estatus el avance del expediente en Subdefensoría: es lo que ve el
 * quejoso en su panel. Solo se actualiza "estatus" (@DynamicUpdate); el resto es de lectura.
 */
@Entity
@Table(name = "quejas")
@DynamicUpdate
@Getter
@Setter
@NoArgsConstructor
public class QuejaReferencia {

    @Id
    private Long id;

    @Column(name = "numero_folio", insertable = false, updatable = false)
    private String numeroFolio;

    @Column(name = "estatus")
    private String estatus;
}
