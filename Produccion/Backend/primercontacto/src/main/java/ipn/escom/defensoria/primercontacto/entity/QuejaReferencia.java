package ipn.escom.defensoria.primercontacto.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.DynamicUpdate;

import java.time.LocalDateTime;

/**
 * Espejo MÍNIMO de la tabla compartida "quejas" (dueño: queja-service / revision-service).
 *
 * Primer Contacto solo necesita tres cosas de ahí:
 *   - el correo del quejoso, para dirigirle acuerdos de conciliación exactamente igual que
 *     lo hace revision-service (el quejoso los ve por ese correo en su panel);
 *   - escribir quejas.estatus cuando avanza el flujo, para que todos los módulos vean el
 *     mismo estado (ver SincronizacionQuejaService);
 *   - leer el historial de quejas para la búsqueda de antecedentes.
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

    // ---- Solo lectura: búsqueda de antecedentes ----

    @Column(name = "motivo", insertable = false, updatable = false)
    private String motivo;

    /*
     * columnDefinition = "TEXT" es obligatorio aquí: la columna real ya es TEXT (la narrativa
     * de la queja no cabe en 255 caracteres). Sin esto, ddl-auto=update intenta angostarla a
     * varchar(255) en cada arranque y Postgres rechaza el ALTER porque ya hay descripciones
     * más largas -- error inofensivo (no se pierde nada, la columna se queda como TEXT) pero
     * ensucia el log en cada reinicio.
     */
    @Column(name = "descripcion", insertable = false, updatable = false, columnDefinition = "TEXT")
    private String descripcion;

    @Column(name = "unidad_academica_clave", insertable = false, updatable = false)
    private String unidadAcademicaClave;

    @Column(name = "fecha_creacion", insertable = false, updatable = false)
    private LocalDateTime fechaCreacion;

    @Column(name = "nombre_quejoso", insertable = false, updatable = false)
    private String nombreQuejoso;

    @Column(name = "apellido1_quejoso", insertable = false, updatable = false)
    private String apellido1Quejoso;

    @Column(name = "folio_primer_contacto", insertable = false, updatable = false)
    private String folioPrimerContacto;
}
