package ipn.escom.defensoria.antecedentes_service;

import ipn.escom.defensoria.ia.antecedentes.ResultadoAntecedente;
import ipn.escom.defensoria.ia.dataset.Queja;
import ipn.escom.defensoria.ia.resumen.TextRankResumen;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/antecedentes")
@CrossOrigin(origins = "*")
public class AntecedenteController {

    private final ServicioAntecedentes servicio;
    private final IaProperties props;

    public AntecedenteController(ServicioAntecedentes servicio, IaProperties props) {
        this.servicio = servicio;
        this.props = props;
    }

    @PostMapping("/buscar")
    public Map<String, Object> buscar(@RequestBody ConsultaRequest request) {
        requerirTexto(request.texto());
        double umbral = request.umbral() != null ? request.umbral() : props.busqueda().umbral();
        int topK = acotar(request.topK(), props.busqueda().topK(), props.busqueda().topKMaximo());

        long inicio = System.nanoTime();
        List<ResultadoAntecedente> resultados = servicio.buscar(request.texto(), umbral, topK, Boolean.TRUE.equals(request.incluirTexto()));

        return respuesta(
                "umbral", umbral,
                "topK", topK,
                "total", resultados.size(),
                "corpus", servicio.tamanoCorpus(),
                "tiempo_ms", milisegundos(inicio),
                "antecedentes", resultados);
    }

    @PostMapping("/entrenar")
    public Map<String, Object> entrenar(@RequestBody EntrenamientoRequest request) throws IOException {
        if (request.queja() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El campo 'queja' es obligatorio");
        }
        requerirTexto(request.queja().textoParaVectorizar());
        return aprender(List.of(request.queja()));
    }

    @PostMapping("/entrenar-batch")
    public Map<String, Object> entrenarBatch(@RequestBody EntrenamientoBatchRequest request) throws IOException {
        if (request.quejas() == null || request.quejas().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El campo 'quejas' no puede estar vacio");
        }
        return aprender(request.quejas());
    }

    @PostMapping("/resumir")
    public Map<String, Object> resumir(@RequestBody ResumenRequest request) {
        String texto = request.textoAResumir();
        requerirTexto(texto);
        int numOraciones = acotar(request.numeroOraciones(), props.resumen().numeroOraciones(), props.resumen().numeroOracionesMaximo());

        long inicio = System.nanoTime();
        TextRankResumen.Resultado r = TextRankResumen.resumir(texto, numOraciones, props.resumen().redundancia());
        int original = texto.length();

        return respuesta(
                "folio", request.folioQueja(),
                "resumen", r.resumen(),
                "oraciones", r.oraciones(),
                "total_oraciones", r.totalOraciones(),
                "numero_oraciones", r.oraciones().size(),
                "longitud_original", original,
                "longitud_resumen", r.resumen().length(),
                "reduccion_porcentaje", Math.round((1 - (double) r.resumen().length() / original) * 1000) / 10.0,
                "tiempo_ms", milisegundos(inicio));
    }

    @GetMapping("/corpus")
    public Map<String, Object> corpus() {
        return respuesta(
                "total", servicio.tamanoCorpus(),
                "vocabulario", servicio.tamanoVocabulario(),
                "aprendidas", servicio.totalAprendidas());
    }

    @GetMapping("/health")
    public Map<String, Object> health() {
        return respuesta("status", "ok", "service", "antecedentes-service", "corpus", servicio.tamanoCorpus());
    }

    private Map<String, Object> aprender(List<Queja> quejas) throws IOException {
        long inicio = System.nanoTime();
        List<Queja> aceptadas = servicio.entrenar(quejas);
        return respuesta(
                "mensaje", aceptadas.size() + " queja(s) aprendida(s)",
                "recibidas", quejas.size(),
                "aprendidas", aceptadas.size(),
                "descartadas", quejas.size() - aceptadas.size(),
                "folios", aceptadas.stream().map(q -> q.folio).toList(),
                "corpus", servicio.tamanoCorpus(),
                "tiempo_ms", milisegundos(inicio));
    }

    private static void requerirTexto(String texto) {
        if (texto == null || texto.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El campo 'texto' es obligatorio");
        }
    }

    private static int acotar(Integer valor, int porDefecto, int maximo) {
        return Math.min(valor != null && valor > 0 ? valor : porDefecto, maximo);
    }

    private static double milisegundos(long inicio) {
        return Math.round((System.nanoTime() - inicio) / 10_000.0) / 100.0;
    }

    private static Map<String, Object> respuesta(Object... claveValor) {
        Map<String, Object> mapa = new LinkedHashMap<>();
        for (int i = 0; i < claveValor.length; i += 2) {
            mapa.put((String) claveValor[i], claveValor[i + 1]);
        }
        return mapa;
    }
}
