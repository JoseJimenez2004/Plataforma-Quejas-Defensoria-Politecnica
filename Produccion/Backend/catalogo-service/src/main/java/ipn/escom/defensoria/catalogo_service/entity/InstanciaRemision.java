package ipn.escom.defensoria.catalogo_service.entity;

import java.time.LocalDateTime;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

/**
 * Instancias a las que Primer Contacto remite un expediente improcedente (OAG, OIC,
 * COSECOVI / Género). La Defensora edita el correo desde su panel; Primer Contacto lee esta
 * misma tabla (entidad de solo lectura en primercontacto) para mandar el oficio.
 * Las columnas deben coincidir con la entidad de primercontacto: ambas usan ddl-auto=update.
 */
@Data
@Entity
@Table(name = "instancias_remision")
public class InstanciaRemision {

    @Id
    @Column(length = 30)
    private String clave;

    @Column(nullable = false, length = 60)
    private String sigla;

    @Column(nullable = false, length = 300)
    private String nombre;

    @Column(length = 150)
    private String correo;

    @Column(nullable = false)
    private Integer orden;

    @Column(name = "fecha_actualizacion")
    private LocalDateTime fechaActualizacion;

    @Column(name = "actualizado_por", length = 150)
    private String actualizadoPor;
}
