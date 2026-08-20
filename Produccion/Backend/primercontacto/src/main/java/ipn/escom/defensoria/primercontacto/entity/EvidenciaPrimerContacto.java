package ipn.escom.defensoria.primercontacto.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "evidencias_primer_contacto")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EvidenciaPrimerContacto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /*
     * ID interno del expediente de Primer Contacto.
     */
    @Column(name = "expediente_id", nullable = false)
    private Long expedienteId;

    /*
     * ID que traía la evidencia en el área de origen.
     * Solo se conserva como referencia.
     */
    @Column(name = "evidencia_origen_id")
    private Long evidenciaOrigenId;

    @Column(name = "nombre_archivo", length = 255)
    private String nombreArchivo;

    @Column(name = "tipo_archivo", length = 100)
    private String tipoArchivo;

    @Column(name = "url_archivo", length = 1000)
    private String urlArchivo;

    @Column(name = "fecha_carga", length = 50)
    private String fechaCarga;
}