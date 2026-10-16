package ipn.escom.defensoria.primercontacto.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Correo con el que Primer Contacto le pide al denunciado su versión de los hechos (formulario
 * del portal /denunciado/respuesta, que guarda denunciado-service). Se puede reenviar: cada
 * envío es una fila; la más reciente es la vigente.
 */
@Entity
@Table(name = "solicitudes_denunciado")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SolicitudDenunciado {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "expediente_id", nullable = false)
    private Long expedienteId;

    @Column(name = "folio", nullable = false, length = 50)
    private String folio;

    /* Folio de la queja (FOL-...): es el que lleva el formulario y con el que denunciado-service guarda la respuesta. */
    @Column(name = "folio_queja", nullable = false, length = 50)
    private String folioQueja;

    @Column(name = "correo_denunciado", nullable = false, length = 150)
    private String correoDenunciado;

    @Column(name = "correo_enviado", nullable = false)
    private boolean correoEnviado;

    @Column(name = "enviado_por_id")
    private Long enviadoPorId;

    @Column(name = "enviado_por_nombre", length = 150)
    private String enviadoPorNombre;

    @Column(name = "fecha_envio", nullable = false)
    private LocalDateTime fechaEnvio;
}
