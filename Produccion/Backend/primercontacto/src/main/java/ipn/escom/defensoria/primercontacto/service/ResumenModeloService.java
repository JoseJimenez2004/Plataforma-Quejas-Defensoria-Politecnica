package ipn.escom.defensoria.primercontacto.service;

import ipn.escom.defensoria.primercontacto.dto.ResumenQuejaDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Cliente del modelo de resumen del compañero (resumen-service, POST /resumir). En CPU tarda
 * de decenas de segundos a ~2 minutos (D9 entrena por queja y Qwen redacta), así que:
 *  - el tiempo de espera es largo (resumen.timeout-segundos) pero menor que el de nginx;
 *  - cada resumen se guarda en memoria por el texto de la queja, para no generarlo dos veces.
 * Si el modelo no responde, la pantalla se queda con el resumen simulado y lo avisa.
 */
@Service
public class ResumenModeloService {

    private static final Logger log = LoggerFactory.getLogger(ResumenModeloService.class);
    private static final String ENDPOINT = "/resumir";
    private static final int MAX_EN_MEMORIA = 300;
    private static final int MAX_CARACTERES = 20000;

    private final RestTemplate restTemplate;
    private final String url;
    private final Map<String, ResumenQuejaDTO> generados = new ConcurrentHashMap<>();

    public ResumenModeloService(
            @Value("${resumen.service.url:http://localhost:8090}") String url,
            @Value("${resumen.timeout-segundos:110}") int timeoutSegundos
    ) {
        SimpleClientHttpRequestFactory fabrica = new SimpleClientHttpRequestFactory();
        fabrica.setConnectTimeout(5_000);
        fabrica.setReadTimeout(timeoutSegundos * 1_000);
        this.restTemplate = new RestTemplate(fabrica);
        this.url = url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }

    /** Vacío si el modelo no está disponible o no respondió a tiempo. */
    @SuppressWarnings("unchecked")
    public Optional<ResumenQuejaDTO> resumir(String texto) {
        String limpio = texto == null ? "" : texto.strip();
        if (limpio.isEmpty()) {
            return Optional.empty();
        }
        if (limpio.length() > MAX_CARACTERES) {
            limpio = limpio.substring(0, MAX_CARACTERES);
        }

        String clave = huella(limpio);
        ResumenQuejaDTO guardado = generados.get(clave);
        if (guardado != null) {
            return Optional.of(guardado);
        }

        long inicio = System.currentTimeMillis();
        try {
            Map<String, Object> r = restTemplate.postForObject(
                    url + ENDPOINT, Map.of("descripcion", limpio), Map.class);
            if (r == null || r.get("resumen") == null) {
                return Optional.empty();
            }
            ResumenQuejaDTO dto = ResumenQuejaDTO.builder()
                    .resumen(String.valueOf(r.get("resumen")).strip())
                    .fragmentos(fragmentos(r.get("captura_d9")))
                    .requiereRevision(true)
                    .versionPipeline(r.get("version_pipeline") != null ? String.valueOf(r.get("version_pipeline")) : null)
                    .build();
            if (generados.size() >= MAX_EN_MEMORIA) {
                generados.clear();
            }
            generados.put(clave, dto);
            log.info("Resumen generado por el modelo en {} ms", System.currentTimeMillis() - inicio);
            return Optional.of(dto);
        } catch (Exception ex) {
            log.warn("El modelo de resumen no respondió ({} ms): {}",
                    System.currentTimeMillis() - inicio, ex.getMessage());
            return Optional.empty();
        }
    }

    /** captura_d9 llega como un solo texto: se separa en oraciones para listarlas. */
    private static List<String> fragmentos(Object captura) {
        if (captura == null) {
            return List.of();
        }
        return Arrays.stream(String.valueOf(captura).split("(?<=[.!?;])\\s+"))
                .map(String::strip)
                .filter(f -> f.length() > 3)
                .toList();
    }

    private static String huella(String texto) {
        try {
            return HexFormat.of().formatHex(
                    MessageDigest.getInstance("SHA-256").digest(texto.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            return Integer.toHexString(texto.hashCode());
        }
    }
}
