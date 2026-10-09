package ipn.escom.defensoria.antecedentes_service;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "ia")
public record IaProperties(@DefaultValue Dataset dataset, @DefaultValue Busqueda busqueda, @DefaultValue Resumen resumen) {

    public record Dataset(
            @DefaultValue("Dataset/quejas_sinteticas.json") String historico,
            @DefaultValue("Dataset/quejas_prueba_30.json") String nuevo,
            @DefaultValue("Dataset/quejas_aprendidas.json") String aprendidas) {
    }

    public record Busqueda(
            @DefaultValue("0.25") double umbral,
            @DefaultValue("5") int topK,
            @DefaultValue("50") int topKMaximo) {
    }

    public record Resumen(
            @DefaultValue("3") int numeroOraciones,
            @DefaultValue("20") int numeroOracionesMaximo,
            @DefaultValue("0.60") double redundancia) {
    }
}
