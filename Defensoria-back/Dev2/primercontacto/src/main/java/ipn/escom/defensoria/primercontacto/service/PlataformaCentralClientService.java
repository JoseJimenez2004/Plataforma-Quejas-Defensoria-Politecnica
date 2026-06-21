package ipn.escom.defensoria.primercontacto.service;

import ipn.escom.defensoria.primercontacto.dto.BandejaAnalisisDTO;
import ipn.escom.defensoria.primercontacto.dto.EvidenciaDTO;
import ipn.escom.defensoria.primercontacto.dto.ExpedienteAnalisisDTO;
import ipn.escom.defensoria.primercontacto.dto.QuejosoDTO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@Service
public class PlataformaCentralClientService {

    private final RestTemplate restTemplate;

    @Value("${plataforma.central.base-url}")
    private String baseUrl;

    public PlataformaCentralClientService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public List<BandejaAnalisisDTO> obtenerBandejaAnalisis(String token) {
        return List.of(
                BandejaAnalisisDTO.builder()
                        .quejaId(1L)
                        .folio("DDP-2026-001")
                        .nombreQuejoso("Juan Pérez")
                        .unidadAcademica("ESCOM")
                        .tema("Género")
                        .prioridad("ALTA")
                        .estatus("PENDIENTE")
                        .fechaRecepcion("2026-04-22")
                        .build(),
                BandejaAnalisisDTO.builder()
                        .quejaId(2L)
                        .folio("DDP-2026-002")
                        .nombreQuejoso("María López")
                        .unidadAcademica("ESCOM")
                        .tema("Académico")
                        .prioridad("MEDIA")
                        .estatus("PENDIENTE")
                        .fechaRecepcion("2026-04-22")
                        .build()
        );
    }
    public ExpedienteAnalisisDTO obtenerExpediente(Long quejaId, String token) {
        String url = baseUrl + "/quejas/" + quejaId + "/expediente";

        HttpEntity<Void> entity = new HttpEntity<>(crearHeaders(token));

        ResponseEntity<ExpedienteAnalisisDTO> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                entity,
                ExpedienteAnalisisDTO.class
        );

        return response.getBody();
    }

    public ExpedienteAnalisisDTO obtenerExpedientePorFolio(String folio, String token) {
        String url = baseUrl + "/quejas/folio/" + folio + "/expediente";

        HttpEntity<Void> entity = new HttpEntity<>(crearHeaders(token));

        ResponseEntity<ExpedienteAnalisisDTO> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                entity,
                ExpedienteAnalisisDTO.class
        );

        return response.getBody();
    }

    public QuejosoDTO obtenerQuejoso(Long quejosoId, String token) {
        String url = baseUrl + "/usuarios/" + quejosoId;

        HttpEntity<Void> entity = new HttpEntity<>(crearHeaders(token));

        ResponseEntity<QuejosoDTO> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                entity,
                QuejosoDTO.class
        );

        return response.getBody();
    }

    public List<EvidenciaDTO> obtenerEvidencias(Long quejaId, String token) {
        String url = baseUrl + "/evidencias/queja/" + quejaId;

        HttpEntity<Void> entity = new HttpEntity<>(crearHeaders(token));

        ResponseEntity<List<EvidenciaDTO>> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                entity,
                new ParameterizedTypeReference<List<EvidenciaDTO>>() {}
        );

        return response.getBody();
    }

    public void actualizarEstatusQueja(Long quejaId, String nuevoEstatus, String token) {
        String url = baseUrl + "/quejas/" + quejaId + "/estatus";

        HttpHeaders headers = crearHeaders(token);
        headers.setContentType(MediaType.APPLICATION_JSON);

        String body = """
                {
                    "estatus": "%s"
                }
                """.formatted(nuevoEstatus);

        HttpEntity<String> entity = new HttpEntity<>(body, headers);

        restTemplate.exchange(
                url,
                HttpMethod.PUT,
                entity,
                Void.class
        );
    }

    public void enviarNotificacion(Long usuarioId, String asunto, String mensaje, String token) {
        String url = baseUrl + "/notificaciones";

        HttpHeaders headers = crearHeaders(token);
        headers.setContentType(MediaType.APPLICATION_JSON);

        String body = """
                {
                    "usuarioId": %d,
                    "asunto": "%s",
                    "mensaje": "%s"
                }
                """.formatted(usuarioId, asunto, mensaje);

        HttpEntity<String> entity = new HttpEntity<>(body, headers);

        restTemplate.exchange(
                url,
                HttpMethod.POST,
                entity,
                Void.class
        );
    }

    private HttpHeaders crearHeaders(String token) {
        HttpHeaders headers = new HttpHeaders();

        if (token != null && !token.isBlank()) {
            headers.setBearerAuth(token.replace("Bearer ", ""));
        }

        return headers;
    }
}