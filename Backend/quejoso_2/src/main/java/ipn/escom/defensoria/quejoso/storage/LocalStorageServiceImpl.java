package ipn.escom.defensoria.quejoso.storage;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

/**
 * Implementación local de StorageService: guarda los archivos en el disco del
 * servidor bajo la ruta configurada en la propiedad {@code storage.local.path}.
 *
 * Estructura de carpetas generada:
 *   {storage.local.path}/
 *     {folio}/
 *       {uuid}_{nombreOriginal}
 *
 * Para migrar a la nube: crear GcsStorageServiceImpl o S3StorageServiceImpl
 * con la misma interfaz, anotar con @Primary y añadir el SDK correspondiente
 * al pom.xml. No se toca ningún otro archivo del proyecto.
 */
@Service
public class LocalStorageServiceImpl implements StorageService {

    /** Ruta base del almacenamiento local, configurable en application.properties. */
    @Value("${storage.local.path}")
    private String rutaBase;

    /**
     * Guarda el archivo en {rutaBase}/{folio}/{UUID}_{nombreOriginal} y retorna
     * la clave relativa "{folio}/{UUID}_{nombreOriginal}" para recuperarlo luego.
     */
    @Override
    public String guardar(MultipartFile archivo, String folio) {
        try {
            String nombreUnico = UUID.randomUUID() + "_" + archivo.getOriginalFilename();
            Path destino = Paths.get(rutaBase, folio, nombreUnico);

            // Crea los subdirectorios si no existen (equivale a mkdir -p)
            Files.createDirectories(destino.getParent());
            Files.write(destino, archivo.getBytes());

            // La clave es la ruta relativa desde rutaBase
            return folio + "/" + nombreUnico;
        } catch (IOException e) {
            throw new RuntimeException("Error al guardar el archivo en disco: " + e.getMessage());
        }
    }

    @Override
    public byte[] leer(String clave) {
        try {
            return Files.readAllBytes(Paths.get(rutaBase, clave));
        } catch (IOException e) {
            throw new RuntimeException("No se pudo leer el archivo: " + e.getMessage());
        }
    }

    @Override
    public void eliminar(String clave) {
        try {
            Files.deleteIfExists(Paths.get(rutaBase, clave));
        } catch (IOException e) {
            throw new RuntimeException("Error al eliminar el archivo: " + e.getMessage());
        }
    }
}
