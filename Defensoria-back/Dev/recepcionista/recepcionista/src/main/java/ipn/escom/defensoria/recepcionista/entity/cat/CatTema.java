package ipn.escom.defensoria.recepcionista.entity.cat;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Entidad de catálogo que mapea la tabla "cat_tema".
 *
 * Clasifica el tipo de problemática reportada en la queja.
 * Ejemplos: Acoso escolar, Discriminación, Abuso de autoridad,
 * Irregularidades académicas, Violencia.
 *
 * El quejoso selecciona el tema al llenar su formulario en el
 * microservicio folio. La recepcionista lo ve en la pantalla
 * de Validación de Requisitos para entender de qué trata la queja.
 */
@Entity
@Table(name = "cat_tema")
@Data
@NoArgsConstructor
public class CatTema {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(unique = true, nullable = false, length = 100)
    private String nombre;
}
