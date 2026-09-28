package ipn.escom.defensoria.primercontacto.service;

import ipn.escom.defensoria.primercontacto.dto.ExpedienteEntranteRequest;
<<<<<<< HEAD:Produccion/Backend/primercontacto/src/main/java/ipn/escom/defensoria/primercontacto/service/SubdefensoriaClientService.java
import ipn.escom.defensoria.primercontacto.dto.SubdefensoriaIngresoResponse;
=======
>>>>>>> b41378653456fe3429bbc88f39e3f15062f0a782:Defensoria-back/Dev2/primercontacto/src/main/java/ipn/escom/defensoria/primercontacto/service/SubdefensoriaClientService.java
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

<<<<<<< HEAD:Produccion/Backend/primercontacto/src/main/java/ipn/escom/defensoria/primercontacto/service/SubdefensoriaClientService.java
=======
/**
 * Envía el expediente a Subdefensoría en el momento del acuerdo de
 * admisión (TPR-07/09 del BPMN). Llamada best-effort: si Subdefensoría
 * no responde, no se revierte el dictamen ya guardado, solo se loguea.
 */
>>>>>>> b41378653456fe3429bbc88f39e3f15062f0a782:Defensoria-back/Dev2/primercontacto/src/main/java/ipn/escom/defensoria/primercontacto/service/SubdefensoriaClientService.java
@Service
public class SubdefensoriaClientService {

    private final RestTemplate restTemplate;
    private final String subdefensoriaBaseUrl;

    public SubdefensoriaClientService(
            RestTemplate restTemplate,
<<<<<<< HEAD:Produccion/Backend/primercontacto/src/main/java/ipn/escom/defensoria/primercontacto/service/SubdefensoriaClientService.java
            @Value("${subdefensoria.base-url}")
            String subdefensoriaBaseUrl
=======
            @Value("${subdefensoria.base-url}") String subdefensoriaBaseUrl
>>>>>>> b41378653456fe3429bbc88f39e3f15062f0a782:Defensoria-back/Dev2/primercontacto/src/main/java/ipn/escom/defensoria/primercontacto/service/SubdefensoriaClientService.java
    ) {
        this.restTemplate = restTemplate;
        this.subdefensoriaBaseUrl = subdefensoriaBaseUrl;
    }

<<<<<<< HEAD:Produccion/Backend/primercontacto/src/main/java/ipn/escom/defensoria/primercontacto/service/SubdefensoriaClientService.java
    public SubdefensoriaIngresoResponse enviarExpediente(
            ExpedienteEntranteRequest expediente
    ) {

        try {

            SubdefensoriaIngresoResponse response =
                    restTemplate.postForObject(
                            subdefensoriaBaseUrl
                                    + "/api/subdefensoria/ingesta/expedientes",
                            expediente,
                            SubdefensoriaIngresoResponse.class
                    );

            if (response == null) {
                throw new RuntimeException(
                        "Subdefensoría no devolvió información del expediente."
                );
            }

            return response;

        } catch (RestClientException ex) {

            throw new RuntimeException(
                    "No se pudo enviar el expediente "
                            + expediente.getFolioOrigen()
                            + " a Subdefensoría: "
                            + ex.getMessage(),
                    ex
            );
=======
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
>>>>>>> b41378653456fe3429bbc88f39e3f15062f0a782:Defensoria-back/Dev2/primercontacto/src/main/java/ipn/escom/defensoria/primercontacto/service/SubdefensoriaClientService.java
        }
    }
}