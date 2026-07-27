package ipn.escom.defensoria.admin_service.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Registro de acciones críticas del panel de administración (creación de usuarios, cambios
 * de contraseña, bajas, cambios de plantillas, respaldos manuales, inicios de sesión). Se
 * muestra en "Seguridad y Respaldos > Historial de Acciones Críticas". */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "bitacora_acciones")
public class BitacoraAccion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String usuario;

    @Column(name = "accion_realizada", nullable = false)
    private String accionRealizada;

    @Column(nullable = false)
    private String ip;

    @Column(nullable = false)
    private LocalDateTime fecha = LocalDateTime.now();

    public BitacoraAccion(String usuario, String accionRealizada, String ip) {
        this.usuario = usuario;
        this.accionRealizada = accionRealizada;
        this.ip = ip;
        this.fecha = LocalDateTime.now();
    }
}
