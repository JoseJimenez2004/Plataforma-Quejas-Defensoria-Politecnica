package ipn.escom.defensoria.antecedentes_service;

import ipn.escom.defensoria.ia.antecedentes.BuscadorAntecedentes;
import ipn.escom.defensoria.ia.antecedentes.ResultadoAntecedente;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/antecedentes")
@CrossOrigin(origins = "*")
public class AntecedenteController {

    private final BuscadorAntecedentes buscador;

    public AntecedenteController(BuscadorAntecedentes buscador) {
        this.buscador = buscador;
    }

    @PostMapping("/buscar")
    public Map<String, Object> buscar(@RequestBody ConsultaRequest request) {
        List<ResultadoAntecedente> resultados = buscador.buscar(request.getTexto(), request.getUmbral(), request.getTopK());

        return Map.of(
                "consulta", request.getTexto(),
                "umbral", request.getUmbral(),
                "total", resultados.size(),
                "antecedentes", resultados
        );
    }

    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of("status", "ok", "service", "antecedentes-service");
    }
}
