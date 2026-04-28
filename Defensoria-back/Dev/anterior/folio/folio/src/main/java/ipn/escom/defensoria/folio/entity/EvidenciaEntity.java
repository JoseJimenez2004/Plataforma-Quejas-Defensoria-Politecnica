package ipn.escom.defensoria.folio.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "evidencias")
@Data
@NoArgsConstructor
public class EvidenciaEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nombreArchivo;
    private String tipoArchivo; 
    private String rutaAlmacenamiento; 
    
    private String codigoFolio;

    @ManyToOne
    @JoinColumn(name = "folio_id")
    private FolioEntity folio;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNombreArchivo() {
        return nombreArchivo;
    }

    public void setNombreArchivo(String nombreArchivo) {
        this.nombreArchivo = nombreArchivo;
    }

    public String getTipoArchivo() {
        return tipoArchivo;
    }

    public void setTipoArchivo(String tipoArchivo) {
        this.tipoArchivo = tipoArchivo;
    }

    public String getRutaAlmacenamiento() {
        return rutaAlmacenamiento;
    }

    public void setRutaAlmacenamiento(String rutaAlmacenamiento) {
        this.rutaAlmacenamiento = rutaAlmacenamiento;
    }

    public FolioEntity getFolio() {
        return folio;
    }

    public void setFolio(FolioEntity folio) {
        this.folio = folio;
    }

    public String getCodigoFolio() {
        return codigoFolio;
    }

    public void setCodigoFolio(String codigoFolio) {
        this.codigoFolio = codigoFolio;
    }
    
}