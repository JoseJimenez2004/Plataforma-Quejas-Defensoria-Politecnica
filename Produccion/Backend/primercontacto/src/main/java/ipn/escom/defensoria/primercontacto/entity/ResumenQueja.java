package ipn.escom.defensoria.primercontacto.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/*
 * Resumen de una queja generado por el modelo (resumen-service). Se guarda la primera vez que
 * el analista pulsa "Generar resumen con IA" y desde entonces se muestra el guardado: el
 * modelo tarda hasta ~2 minutos y no tiene caso volver a pedirlo. Una fila por folio.
 *
 * El resumen SIMULADO (cuando el modelo no responde) no se guarda: lo calcula el navegador y
 * el analista puede volver a intentarlo con el modelo.
 */
@Entity
@Table(name = "resumenes_queja")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ResumenQueja {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "folio", nullable = false, unique = true, length = 50)
    private String folio;

    @Column(name = "resumen", nullable = false, columnDefinition = "TEXT")
    private String resumen;

    /* Fragmentos de D9 como arreglo JSON de textos. */
    @Column(name = "fragmentos", columnDefinition = "TEXT")
    private String fragmentos;

    @Column(name = "version_pipeline", length = 100)
    private String versionPipeline;

    @Column(name = "generado_por_id")
    private Long generadoPorId;

    @Column(name = "generado_por_nombre", length = 150)
    private String generadoPorNombre;

    @Column(name = "fecha_generacion", nullable = false)
    private LocalDateTime fechaGeneracion;
}
