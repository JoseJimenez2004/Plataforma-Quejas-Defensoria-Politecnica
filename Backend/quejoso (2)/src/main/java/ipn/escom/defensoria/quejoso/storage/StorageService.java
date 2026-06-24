package ipn.escom.defensoria.quejoso.storage;

import org.springframework.web.multipart.MultipartFile;

/**
 * Abstracción del servicio de almacenamiento de archivos de evidencias.
 *
 * Implementación actual: sistema de archivos local (LocalStorageServiceImpl).
 * Para migrar a la nube (GCS, S3, Azure Blob), basta con crear una nueva
 * clase que implemente esta interfaz y anotarla con @Primary — ningún otro
 * archivo del proyecto requiere cambios.
 */
public interface StorageService {

    /**
     * Guarda el archivo y retorna la clave única (ruta relativa) con la que
     * se puede leer o eliminar posteriormente. Esa clave se almacena en
     * EvidenciaEntity.urlAlmacenamiento.
     *
     * @param archivo archivo recibido del multipart
     * @param folio   folio de la queja, usado para organizar las carpetas
     * @return clave única del archivo (por ejemplo "DDP-2026-0001/uuid_foto.jpg")
     */
    String guardar(MultipartFile archivo, String folio);

    /**
     * Lee y retorna los bytes del archivo identificado por su clave.
     *
     * @param clave clave retornada por {@link #guardar}
     */
    byte[] leer(String clave);

    /**
     * Elimina el archivo del almacenamiento. No lanza excepción si el archivo
     * ya no existe.
     *
     * @param clave clave retornada por {@link #guardar}
     */
    void eliminar(String clave);
}
