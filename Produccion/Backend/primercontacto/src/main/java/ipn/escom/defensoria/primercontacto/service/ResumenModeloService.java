package ipn.escom.defensoria.primercontacto.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import ipn.escom.defensoria.primercontacto.dto.ResumenQuejaDTO;
import ipn.escom.defensoria.primercontacto.entity.PersonalAdministrativo;
import ipn.escom.defensoria.primercontacto.entity.ResumenQueja;
import ipn.escom.defensoria.primercontacto.repository.ResumenQuejaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
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
 *  - con folio, el resumen se GUARDA en la base (tabla resumenes_queja) la primera vez y
 *    después siempre se devuelve el guardado: no se vuelve a generar;
 *  - sin folio, se guarda solo en memoria por el texto de la queja.
 * Si el modelo no responde, la pantalla se queda con el resumen simulado y lo avisa (ese no
 * se guarda, para poder reintentar con el modelo).
 */
@Service
public class ResumenModeloService {

    private static final Logger log = LoggerFactory.getLogger(ResumenModeloService.class);
    private static final String ENDPOINT = "/resumir";
    private static final int MAX_EN_MEMORIA = 300;
    private static final int MAX_CARACTERES = 20000;
    private static final ObjectMapper JSON = new ObjectMapper();

    private final RestTemplate restTemplate;
    private final String url;
    private final ResumenQuejaRepository repository;
    private final Map<String, ResumenQuejaDTO> generados = new ConcurrentHashMap<>();

    public ResumenModeloService(
            @Value("${resumen.service.url:http://localhost:8090}") String url,
            @Value("${resumen.timeout-segundos:110}") int timeoutSegundos,
            ResumenQuejaRepository repository
    ) {
        SimpleClientHttpRequestFactory fabrica = new SimpleClientHttpRequestFactory();
        fabrica.setConnectTimeout(5_000);
        fabrica.setReadTimeout(timeoutSegundos * 1_000);
        this.restTemplate = new RestTemplate(fabrica);
        this.url = url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
        this.repository = repository;
    }

    /** Resumen ya guardado de la queja, si alguien lo generó antes. */
    public Optional<ResumenQuejaDTO> guardado(String folio) {
        if (folio == null || folio.isBlank()) {
            return Optional.empty();
        }
        return repository.findByFolio(folio.strip()).map(ResumenModeloService::aDto);
    }

    /**
     * Con folio: devuelve el guardado o lo genera una sola vez y lo guarda.
     * Vacío si el modelo no está disponible o no respondió a tiempo.
     */
    public Optional<ResumenQuejaDTO> resumir(String folio, String texto, PersonalAdministrativo analista) {
        Optional<ResumenQuejaDTO> previo = guardado(folio);
        if (previo.isPresent()) {
            return previo;
        }

        Optional<ResumenQuejaDTO> generado = generar(texto);
        if (generado.isEmpty() || folio == null || folio.isBlank()) {
            return generado;
        }

        ResumenQuejaDTO dto = generado.get();
        try {
            ResumenQueja guardado = repository.save(ResumenQueja.builder()
                    .folio(folio.strip())
                    .resumen(dto.getResumen())
                    .fragmentos(JSON.writeValueAsString(dto.getFragmentos()))
                    .versionPipeline(dto.getVersionPipeline())
                    .generadoPorId(analista != null ? analista.getId() : null)
                    .generadoPorNombre(analista != null ? analista.getNombreCompleto() : null)
                    .fechaGeneracion(LocalDateTime.now())
                    .build());
            return Optional.of(aDto(guardado));
        } catch (DataIntegrityViolationException ex) {
            // Otro analista lo guardó mientras el modelo trabajaba: se usa el suyo.
            return guardado(folio).or(() -> generado);
        } catch (Exception ex) {
            log.warn("No se pudo guardar el resumen de {}: {}", folio, ex.getMessage());
            return generado;
        }
    }

    /** Llama al modelo (con caché en memoria por texto). */
    @SuppressWarnings("unchecked")
    private Optional<ResumenQuejaDTO> generar(String texto) {
        String limpio = texto == null ? "" : texto.strip();
        if (limpio.isEmpty()) {
            return Optional.empty();
        }
        if (limpio.length() > MAX_CARACTERES) {
            limpio = limpio.substring(0, MAX_CARACTERES);
        }

        String clave = huella(limpio);
        ResumenQuejaDTO enMemoria = generados.get(clave);
        if (enMemoria != null) {
            return Optional.of(enMemoria);
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

    private static ResumenQuejaDTO aDto(ResumenQueja r) {
        List<String> fragmentos;
        try {
            fragmentos = r.getFragmentos() == null ? List.of()
                    : JSON.readValue(r.getFragmentos(), new TypeReference<List<String>>() {});
        } catch (Exception ex) {
            fragmentos = List.of();
        }
        return ResumenQuejaDTO.builder()
                .resumen(r.getResumen())
                .fragmentos(fragmentos)
                .requiereRevision(true)
                .versionPipeline(r.getVersionPipeline())
                .fechaGeneracion(r.getFechaGeneracion())
                .generadoPor(r.getGeneradoPorNombre())
                .build();
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
