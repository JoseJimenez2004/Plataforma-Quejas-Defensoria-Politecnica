package ipn.escom.defensoria.primercontacto.entity;

import jakarta.persistence.*;
import lombok.*;
import ipn.escom.defensoria.primercontacto.entity.PersonalAdministrativo;

import java.time.LocalDateTime;

@Entity
@Table(name = "notas_analisis")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotaAnalisis {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "expediente_id", nullable = false)
    private Long expedienteId;

    @Column(name = "folio", nullable = false, length = 50)
    private String folio;

    @Column(name = "analista_id", nullable = false)
    private Long analistaId;

    @Column(name = "analista_nombre", length = 150)
    private String analistaNombre;

    @Column(name = "contenido", nullable = false, columnDefinition = "TEXT")
    private String contenido;

    @Column(name = "fecha_creacion", nullable = false)
    private LocalDateTime fechaCreacion;

    @Column(name = "fecha_actualizacion")
    private LocalDateTime fechaActualizacion;
}