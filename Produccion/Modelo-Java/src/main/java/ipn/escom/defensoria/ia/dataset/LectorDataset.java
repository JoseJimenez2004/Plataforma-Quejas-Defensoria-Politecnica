package ipn.escom.defensoria.ia.dataset;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

public class LectorDataset {

    private static final ObjectMapper mapper = new ObjectMapper();

    public static List<Queja> leer(Path ruta) throws IOException {
        Dataset dataset = mapper.readValue(ruta.toFile(), Dataset.class);
        System.out.println("Leido " + ruta + " : " + dataset.total + " quejas");
        return dataset.quejas;
    }
}
