package ipn.escom.defensoria.antecedentes_service;

import ipn.escom.defensoria.ia.antecedentes.BuscadorAntecedentes;
import ipn.escom.defensoria.ia.dataset.LectorDataset;
import ipn.escom.defensoria.ia.dataset.Queja;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@Configuration
public class AntecedentesServiceConfig {

    @Value("${ia.dataset.historico:Dataset/quejas_sinteticas_20.json}")
    private String historicoPath;

    @Value("${ia.dataset.nuevo:Dataset/quejas_prueba_30.json}")
    private String nuevoPath;

    @Bean
    public BuscadorAntecedentes buscadorAntecedentes() throws Exception {
        Path historico = Paths.get(historicoPath);
        Path nuevo = Paths.get(nuevoPath);

        List<Queja> historicoQuejas = LectorDataset.leer(historico);
        List<Queja> nuevoQuejas = LectorDataset.leer(nuevo);

        return new BuscadorAntecedentes(historicoQuejas, nuevoQuejas);
    }
}
