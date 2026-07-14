package ipn.escom.defensoria.catalogo_service.entity;

import java.time.LocalDateTime;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

/**
 * Catálogo de dependencias del IPN (áreas centrales, secretarías, direcciones, y las
 * unidades académicas: CECyT, ESIME, ESCOM, etc.). Lo usa el formulario de "Presentar una
 * queja" del frontend para tener un selector real de la dependencia involucrada, en vez de
 * texto libre — y a futuro, cualquier otro microservicio que necesite este catálogo lo
 * consulta aquí en vez de duplicarlo.
 *
 * La jerarquía se modela con una clave propia (no el id autogenerado) para que la carga
 * inicial (seed) sea legible y no dependa de que los ids se generen en un orden exacto —
 * ver resources/seed/dependencias_ipn.csv y dependencias_seed.sql.
 */
@Data
@Entity
@Table(name = "dependencias")
public class Dependencia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 20)
    private String clave;

    @Column(name = "clave_padre", length = 20)
    private String clavePadre;

    @Column(nullable = false)
    private String nombre;

    @Column(length = 30)
    private String abreviatura;

    @Column(nullable = false, length = 60)
    private String tipo;

    @Column(length = 150)
    private String categoria;

    @Column(nullable = false)
    private Integer nivel = 1;

    @Column(name = "pagina_manual")
    private Integer paginaManual;

    @Column(nullable = false)
    private Boolean activo = true;

    @Column(columnDefinition = "TEXT")
    private String notas;

    @Column(name = "creado_en")
    private LocalDateTime creadoEn = LocalDateTime.now();
}
