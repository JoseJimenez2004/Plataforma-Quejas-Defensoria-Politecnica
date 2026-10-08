package ipn.escom.defensoria.primercontacto.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Queja que el analista eligió como ANTECEDENTE FINAL de un expediente, venga de la
 * búsqueda manual (por quejoso o denunciado) o de la del modelo.
 *
 * Se guarda una copia de los datos que se vieron al elegirla (asunto, nombres, extracto...),
 * porque puede venir del histórico o del índice del modelo, que no viven en esta base.
 * Una misma queja (origen + folio) solo se guarda una vez por expediente.
 */
@Entity
@Table(
        name = "antecedentes_primer_contacto",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_antecedente_expediente",
                columnNames = {"expediente_id", "origen", "folio_antecedente"}
        )
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AntecedenteExpediente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "expediente_id", nullable = false)
    private Long expedienteId;

    /* SISTEMA o HISTORICO. */
    @Column(name = "origen", nullable = false, length = 20)
    private String origen;

    @Column(name = "folio_antecedente", nullable = false, length = 60)
    private String folioAntecedente;

    /* Cómo se encontró: MANUAL, MODELO o REGLAS_PROVISIONAL. */
    @Column(name = "fuente", nullable = false, length = 30)
    private String fuente;

    /* 0-100; null si salió de la búsqueda manual. */
    @Column(name = "similitud")
    private Integer similitud;

    @Column(name = "asunto", length = 500)
    private String asunto;

    @Column(name = "fecha_antecedente", length = 30)
    private String fechaAntecedente;

    @Column(name = "nombre_quejoso", length = 300)
    private String nombreQuejoso;

    @Column(name = "nombre_denunciado", length = 300)
    private String nombreDenunciado;

    @Column(name = "unidad_academica", length = 60)
    private String unidadAcademica;

    @Column(name = "estatus_antecedente", length = 50)
    private String estatusAntecedente;

    @Column(name = "folio_primer_contacto", length = 50)
    private String folioPrimerContacto;

    @Column(name = "extracto", columnDefinition = "TEXT")
    private String extracto;

    @Column(name = "analista_id", nullable = false)
    private Long analistaId;

    @Column(name = "analista_nombre", length = 150)
    private String analistaNombre;

    @Column(name = "fecha_registro", nullable = false)
    private LocalDateTime fechaRegistro;
}
