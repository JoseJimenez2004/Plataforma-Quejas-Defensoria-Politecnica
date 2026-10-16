package ipn.escom.defensoria.queja_service.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import org.hibernate.annotations.Immutable;

/**
 * Cuenta del quejoso (tabla usuarios, la administra auth-service). Solo lectura: se usa para
 * guardar en la queja quién la presenta cuando se registra desde el panel con sesión, que
 * solo trae el correo del JWT. Sin esto la queja llegaba sin nombre a Revisión y Primer
 * Contacto.
 */
@Getter
@Entity
@Immutable
@Table(name = "usuarios")
public class CuentaQuejoso {

    @Id
    private Long id;

    @Column(name = "nombre", insertable = false, updatable = false)
    private String nombre;

    @Column(name = "correo_institucional", insertable = false, updatable = false)
    private String correoInstitucional;

    @Column(name = "boleta", insertable = false, updatable = false)
    private String boleta;
}
