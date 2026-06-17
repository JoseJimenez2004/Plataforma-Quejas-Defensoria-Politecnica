package ipn.escom.defensoria.recepcion.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.Id;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Column;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import java.time.LocalDateTime;

@Entity
@Table(name = "quejas", schema = "defensoria")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Queja {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "folio_ds", unique = true, nullable = false)
    private String folioDs;

    @Column(name = "estatus")
    private String estatus;

    @Column(name = "resumen_asunto")
    private String resumenAsunto;

    @Column(name = "correo_quejoso")
    private String correoQuejoso;

    @Column(name = "boleta_quejoso")
    private String boletaQuejoso;

    @Column(name = "documentacion_valida")
    private boolean documentacionValida;

    @Column(name = "fecha_recepcion")
    private LocalDateTime fechaRecepcion;
}