package ipn.escom.defensoria.revision_service.service;

import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import ipn.escom.defensoria.revision_service.model.AreaOpcionModel;

/** Lee el catálogo de dependencias de catalogo-service (endpoint público, sin JWT) para
 * poblar el combo "Área a la que se turna" -- mismo patrón que admin-service usa para contar
 * dependencias en su dashboard. */
@Service
public class CatalogoRefService {

    private static final Logger log = LoggerFactory.getLogger(CatalogoRefService.class);

    private static final String ENDPOINT_DEPENDENCIAS = "/api/catalogos/dependencias";

    private final RestTemplate restTemplate;

    @Value("${catalogo.service.url}")
    private String catalogoServiceUrl;

    public CatalogoRefService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @SuppressWarnings("unchecked")
    public List<AreaOpcionModel> listarAreas() {
        try {
            Map<String, Object>[] dependencias = restTemplate.getForObject(
                    catalogoServiceUrl + ENDPOINT_DEPENDENCIAS, Map[].class);
            if (dependencias == null) {
                return List.of();
            }
            return List.of(dependencias).stream()
                    .map(d -> new AreaOpcionModel(
                            String.valueOf(d.get("clave")),
                            String.valueOf(d.get("nombre"))))
                    .toList();
        } catch (Exception ex) {
            log.warn("No se pudo obtener el catálogo de dependencias desde catalogo-service: {}", ex.getMessage());
            return List.of();
        }
    }
}
