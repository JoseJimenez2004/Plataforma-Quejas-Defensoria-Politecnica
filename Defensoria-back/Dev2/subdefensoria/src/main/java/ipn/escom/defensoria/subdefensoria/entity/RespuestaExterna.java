package ipn.escom.defensoria.subdefensoria.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Respuesta recibida de la Unidad Academica / autoridad para un
 * oficio dado (P15 "Gestion de Respuesta Externa - El Puente"). Al
 * registrarse, marca el/los oficio(s) del expediente como
 * RESPONDIDO y habilita el expediente para pasar a
 * LISTO_A_DICTAMINAR (act. 11 "hay respuesta" y 13 del DDP-PO-02).
 */
@Entity
@Table(name = "respuestas_externas")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RespuestaExterna {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "expediente_id", nullable = false)
    private Long expedienteId;

    @Column(name = "oficio_id", nullable = false)
    private Long oficioId;

    @Column(name = "canal_recepcion", nullable = false, length = 100)
    private String canalRecepcion;

    @Column(name = "numero_oficio_respuesta_ua", length = 100)
    private String numeroOficioRespuestaUA;

    @Column(name = "archivo_pdf_path", length = 300)
    private String archivoPdfPath;

    @Column(name = "resumen", nullable = false, columnDefinition = "TEXT")
    private String resumen;

    @Column(name = "fecha_recepcion", nullable = false)
    private LocalDateTime fechaRecepcion;
}
