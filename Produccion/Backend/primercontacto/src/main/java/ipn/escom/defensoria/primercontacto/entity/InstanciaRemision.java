package ipn.escom.defensoria.primercontacto.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Immutable;

/**
 * Instancia de remisión (OAG, OIC, COSECOVI / Género) con su correo. La tabla la administra
 * catalogo-service (la Defensora edita el correo y ahí se hace la carga inicial); aquí solo se
 * lee. Las columnas deben coincidir con catalogo_service.entity.InstanciaRemision porque los
 * dos servicios usan ddl-auto=update sobre defensoria_db.
 */
@Entity
@Immutable
@Table(name = "instancias_remision")
@Getter
@NoArgsConstructor
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
