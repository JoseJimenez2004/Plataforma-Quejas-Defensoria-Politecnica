package ipn.escom.defensoria.quejoso.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "quejas")
@Data
public class Queja {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String folio; // DDP-YYYY-XXXX

    @Column(nullable = false)
    private String asunto; // Texto libre

    @Lob
    private String descripcion;

    private String unidaddondeOcurrio; // Catálogo UA
    private LocalDateTime fechaHechos;
    private LocalDateTime fechaRegistro = LocalDateTime.now();

    @Enumerated(EnumType.STRING)
    private EstatusQuejaEntity estatus = EstatusQuejaEntity.RECIBIDA;

    // Relación automática: si la boleta/correo existe, se liga al usuario
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id")
    private Usuario quejoso;

    // Datos del denunciado (Solo si el quejoso los conoce)
    private String nombreDenunciado;

    @OneToOne(mappedBy = "queja", cascade = CascadeType.ALL)
    private Tutor tutor; // Solo si es menor de edad

    // En Queja.java
    @OneToMany(mappedBy = "queja", cascade = CascadeType.ALL)
    private List<EvidenciaEntity> evidencias = new java.util.ArrayList<>(); // <-- Asegúrate de tener el "= new ArrayList<>()"

    private String correoQuejoso;            // El correo donde se enviará el folio
    private String identificacionInstitucional; // Boleta o Número de empleado
    private String tipoIdentificacion;       // ALUMNO o EMPLEADO
    private String fechaNacimiento;          // Para validar minoría de edad (MQ-03)


}