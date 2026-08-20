package ipn.escom.defensoria.primercontacto.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "expedientes_primer_contacto",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_expediente_pc_folio",
                        columnNames = "folio"
                ),
                @UniqueConstraint(
                        name = "uk_expediente_pc_folio_origen",
                        columnNames = "folio_origen"
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExpedientePrimerContacto {

    /*
     * ID INTERNO de Primer Contacto.
     * No corresponde al id de la tabla quejas.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /*
     * Folio generado cuando el expediente entra
     * formalmente a Primer Contacto.
     * Ejemplo:
     * PC-A1B2C3D4
     */
    @Column(name = "folio", nullable = false, length = 50)
    private String folio;

    /*
     * Folio con el que el expediente llegó desde
     * el área anterior.
     * Ejemplo:
     * FOL-12345678
     * Esta es la referencia entre áreas.
     * NO utilizamos quejas.id.
     */
    @Column(name = "folio_origen", nullable = false, length = 50)
    private String folioOrigen;

    /*
     * Estado del expediente dentro de Primer Contacto.
     */
    @Column(name = "estatus", nullable = false, length = 40)
    private String estatus;

    @Column(name = "fecha_creacion", nullable = false)
    private LocalDateTime fechaCreacion;

    @Column(name = "fecha_actualizacion")
    private LocalDateTime fechaActualizacion;
}