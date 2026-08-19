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
     * Datos propios del expediente recibido por Primer Contacto.
     * Se guardan aquí para no depender de información en memoria.
     */

    @Column(name = "tema", length = 200)
    private String tema;

    @Column(name = "descripcion_hechos", columnDefinition = "TEXT")
    private String descripcionHechos;

    /*
     * Fecha con la que el asunto fue recibido desde el área anterior.
     * Se conserva como información de origen.
     */
    @Column(name = "fecha_recepcion_origen", length = 50)
    private String fechaRecepcionOrigen;

    /*
     * Prioridad recibida desde Revisión.
     */
    @Column(name = "prioridad", length = 30)
    private String prioridad;

    @Column(name = "quejoso_nombre", length = 200)
    private String quejosoNombre;

    @Column(name = "quejoso_correo", length = 150)
    private String quejosoCorreo;

    @Column(name = "unidad_academica", length = 200)
    private String unidadAcademica;

    /*
     * Estado del expediente dentro de Primer Contacto.
     */
    @Column(name = "estatus", nullable = false, length = 40)
    private String estatus;

    @Column(name = "fecha_creacion", nullable = false)
    private LocalDateTime fechaCreacion;

    @Column(name = "fecha_actualizacion")
    private LocalDateTime fechaActualizacion;

    /*
     * Folio generado por Subdefensoría cuando el expediente
     * continúa a esa etapa.
     *
     * Ejemplo: SD-A1B2C3D4
     *
     * No es una llave foránea.
     */
    @Column(name = "folio_subdefensoria", unique = true, length = 50)
    private String folioSubdefensoria;

    @Column(name = "quejoso_id")
    private Long quejosoId;

    @Column(name = "quejoso_telefono", length = 30)
    private String quejosoTelefono;

    @Column(name = "quejoso_tipo_usuario", length = 50)
    private String quejosoTipoUsuario;
}