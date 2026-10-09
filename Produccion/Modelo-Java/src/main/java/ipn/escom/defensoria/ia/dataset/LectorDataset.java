package ipn.escom.defensoria.ia.dataset;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;

public final class LectorDataset {

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .setSerializationInclusion(JsonInclude.Include.NON_NULL);

    private LectorDataset() {
    }

    public static List<Queja> leer(Path ruta) throws IOException {
        if (!Files.exists(ruta)) {
            return List.of();
        }
        Dataset dataset = MAPPER.readValue(ruta.toFile(), Dataset.class);
        return dataset.quejas != null ? dataset.quejas : List.of();
    }

    public static void guardar(Path ruta, String nombre, List<Queja> quejas) throws IOException {
        Dataset dataset = new Dataset();
        dataset.dataset = nombre;
        dataset.formato = "json";
        dataset.normalizacion = "texto_preprocesado en minusculas, sin tildes";
        dataset.total = quejas.size();
        dataset.fuente = "aprendidas_en_linea";
        dataset.quejas = quejas;

        if (ruta.getParent() != null) {
            Files.createDirectories(ruta.getParent());
        }
        Path temporal = ruta.resolveSibling(ruta.getFileName() + ".tmp");
        MAPPER.writerWithDefaultPrettyPrinter().writeValue(temporal.toFile(), dataset);
        Files.move(temporal, ruta, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
    }
}
