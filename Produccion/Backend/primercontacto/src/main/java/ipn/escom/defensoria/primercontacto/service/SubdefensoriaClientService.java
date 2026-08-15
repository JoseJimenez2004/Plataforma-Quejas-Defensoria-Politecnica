package ipn.escom.defensoria.primercontacto.service;

import ipn.escom.defensoria.primercontacto.dto.ExpedienteEntranteRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

/**
 * Envía el expediente a Subdefensoría en el momento del acuerdo de
 * admisión (TPR-07/09 del BPMN). Llamada best-effort: si Subdefensoría
 * no responde, no se revierte el dictamen ya guardado, solo se loguea.
 */
@Service
public class SubdefensoriaClientService {

    private final RestTemplate restTemplate;
    private final String subdefensoriaBaseUrl;

    public SubdefensoriaClientService(
            RestTemplate restTemplate,
            @Value("${subdefensoria.base-url}") String subdefensoriaBaseUrl
    ) {
        this.restTemplate = restTemplate;
        this.subdefensoriaBaseUrl = subdefensoriaBaseUrl;
    }

    public void enviarExpediente(ExpedienteEntranteRequest expediente) {
        try {
            restTemplate.postForEntity(
                    subdefensoriaBaseUrl + "/api/subdefensoria/ingesta/expedientes",
                    expediente,
                    Void.class
            );
        } catch (RestClientException ex) {
            System.err.println("No se pudo notificar a Subdefensoría del expediente "
                    + expediente.getFolio() + ": " + ex.getMessage());
        }
    }
}