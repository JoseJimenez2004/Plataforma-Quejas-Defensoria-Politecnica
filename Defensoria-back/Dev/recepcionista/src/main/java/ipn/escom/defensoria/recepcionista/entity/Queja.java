package ipn.escom.defensoria.recepcionista.entity;

import jakarta.persistence.Id;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.EnumType;
import jakarta.persistence.PrePersist;

import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "quejas")
@Data
public class Queja {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Campos base
    private String folio;             // CU29: Generar número de folio
    private String nombrePromovente;
    private String descripcion;
    private String turnoAsignado;
    private LocalDateTime fechaCreacion;

    // Campos para CU33 (Búsqueda de antecedentes)
    private String correoPromovente;  // CU33.1: Buscar por correo
    private String boleta;            // CU33.2: Buscar por boleta

    // Campos para CU34/35 (Canalización)
    private String abogadoAsignado;   // Para canalizar al abogado correspondiente
    
    @Enumerated(EnumType.STRING)
    private EstadoQueja estado;       // CU46: Actualizar status (PENDIENTE, VALIDADA, RECHAZADA, EN_REVISION)

    @PrePersist
    protected void onCreate() {
        this.fechaCreacion = LocalDateTime.now();
        // El estado inicial siempre es PENDIENTE al registrar (CU26)
        if (this.estado == null) {
            this.estado = EstadoQueja.PENDIENTE;
        }
    }
}