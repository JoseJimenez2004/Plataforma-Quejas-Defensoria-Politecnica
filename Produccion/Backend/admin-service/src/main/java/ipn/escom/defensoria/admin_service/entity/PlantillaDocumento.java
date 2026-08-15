package ipn.escom.defensoria.admin_service.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

/**
 * Plantilla de un documento/oficio oficial (ej. "Oficio de Solicitud de Información"). El
 * contenido incluye placeholders tipo {folio_queja}, {nombre_quejoso}, {nombre_subdefensor}
 * que el futuro front de revisión de quejas rellenará al generar el documento real.
 */
@Data
@Entity
@Table(name = "plantillas_documentos")
public class PlantillaDocumento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Clave estable del tipo de documento, ej. "OFICIO_SOLICITUD_INFORMACION". */
    @Column(nullable = false, unique = true, length = 80)
    private String tipo;

    @Column(nullable = false)
    private String nombre;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String contenido;

    @Column(nullable = false)
    private boolean activa = true;

    @Column(name = "actualizado_en")
    private LocalDateTime actualizadoEn = LocalDateTime.now();

    @Column(name = "actualizado_por")
    private String actualizadoPor;
}
