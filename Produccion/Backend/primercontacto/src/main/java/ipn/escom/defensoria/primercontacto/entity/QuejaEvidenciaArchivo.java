package ipn.escom.defensoria.primercontacto.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import org.hibernate.annotations.Immutable;

/**
 * Espejo de SOLO LECTURA de "queja_evidencias" (dueño: queja-service), donde vive el
 * contenido real (BYTEA) de cada evidencia.
 *
 * Primer Contacto solo copia los metadatos al recibir el expediente
 * (EvidenciaPrimerContacto.evidenciaOrigenId apunta aquí). Para que el analista pueda ABRIR
 * la evidencia al dictaminar (CU-PC-06/07) se lee el archivo directo de esta tabla: la
 * descarga de revision-service exige rol RECEPCIONISTA, así que reenviarle la petición con
 * el token del analista respondería 403. Mismo patrón que PersonalAdministrativo.
 */
@Getter
@Entity
@Immutable
@Table(name = "queja_evidencias")
public class QuejaEvidenciaArchivo {

    @Id
    private Long id;

    @Column(name = "nombre_archivo")
    private String nombreArchivo;

    @Column(name = "tipo_mime")
    private String tipoMime;

    @Column(name = "contenido", columnDefinition = "bytea")
    private byte[] contenido;
}
