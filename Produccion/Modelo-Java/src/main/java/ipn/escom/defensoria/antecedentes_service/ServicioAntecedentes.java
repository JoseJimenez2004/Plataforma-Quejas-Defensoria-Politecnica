package ipn.escom.defensoria.antecedentes_service;

import ipn.escom.defensoria.ia.antecedentes.BuscadorAntecedentes;
import ipn.escom.defensoria.ia.antecedentes.ResultadoAntecedente;
import ipn.escom.defensoria.ia.dataset.LectorDataset;
import ipn.escom.defensoria.ia.dataset.Queja;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class ServicioAntecedentes {

    private static final Logger log = LoggerFactory.getLogger(ServicioAntecedentes.class);

    private final BuscadorAntecedentes buscador = new BuscadorAntecedentes();
    private final List<Queja> aprendidas = new ArrayList<>();
    private final AtomicLong secuencia = new AtomicLong();
    private final Path rutaAprendidas;

    public ServicioAntecedentes(IaProperties props) throws IOException {
        this.rutaAprendidas = Path.of(props.dataset().aprendidas());
        cargar(props.dataset().historico());
        cargar(props.dataset().nuevo());
        aprendidas.addAll(cargar(props.dataset().aprendidas()));
        log.info("Corpus listo: {} quejas, {} terminos en vocabulario", buscador.tamano(), buscador.tamanoVocabulario());
    }

    public List<ResultadoAntecedente> buscar(String texto, double umbral, int topK, boolean incluirTexto) {
        return buscador.buscar(texto, umbral, topK, incluirTexto);
    }

    public synchronized List<Queja> entrenar(List<Queja> quejas) throws IOException {
        for (Queja q : quejas) {
            if (q == null) {
                continue;
            }
            if (q.folio == null || q.folio.isBlank()) {
                q.folio = "FOL-APN-" + System.currentTimeMillis() + "-" + secuencia.incrementAndGet();
            }
            q.es_historico = true;
        }
        List<Queja> aceptadas = buscador.agregar(quejas);
        if (!aceptadas.isEmpty()) {
            aprendidas.addAll(aceptadas);
            LectorDataset.guardar(rutaAprendidas, "quejas_aprendidas", aprendidas);
        }
        return aceptadas;
    }

    public int tamanoCorpus() {
        return buscador.tamano();
    }

    public int tamanoVocabulario() {
        return buscador.tamanoVocabulario();
    }

    public synchronized int totalAprendidas() {
        return aprendidas.size();
    }

    private List<Queja> cargar(String ruta) throws IOException {
        Path path = Path.of(ruta);
        if (!Files.exists(path)) {
            log.warn("Dataset no encontrado, se omite: {}", path.toAbsolutePath());
            return List.of();
        }
        long inicio = System.nanoTime();
        List<Queja> aceptadas = buscador.agregar(LectorDataset.leer(path));
        log.info("Cargadas {} quejas de {} en {} ms", aceptadas.size(), path, (System.nanoTime() - inicio) / 1_000_000);
        return aceptadas;
    }
}
