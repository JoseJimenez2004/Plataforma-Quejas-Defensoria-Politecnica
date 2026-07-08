package ipn.escom.defensoria.auth.service.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Table(name = "usuarios")
@Data
public class Usuario {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nombre;

    @Column(unique = true, nullable = false)
    private String correoInstitucional;

    @Column(unique = true, nullable = false)
    private String boleta;

    private String password; 
    
    private String unidadAcademica;
    
    private boolean activo = false;

    private String correoPersonal;
    
    private String telefonoCelular;

    private String nombreTutor;
    
    private String parentescoTutor;
    
    private String telefonoTutor;

    private String codigoRecuperacion;
    
    private LocalDateTime fechaExpiracionCodigo;
}