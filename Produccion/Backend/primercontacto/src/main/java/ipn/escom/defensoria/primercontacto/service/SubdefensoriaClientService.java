package ipn.escom.defensoria.primercontacto.service;

import ipn.escom.defensoria.primercontacto.dto.ExpedienteEntranteRequest;
import ipn.escom.defensoria.primercontacto.dto.SubdefensoriaIngresoResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Service
public class SubdefensoriaClientService {

    private final RestTemplate restTemplate;
    private final String subdefensoriaBaseUrl;

    public SubdefensoriaClientService(
            RestTemplate restTemplate,
            @Value("${subdefensoria.base-url}")
            String subdefensoriaBaseUrl
    ) {
        this.restTemplate = restTemplate;
        this.subdefensoriaBaseUrl = subdefensoriaBaseUrl;
    }

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
        }
    }
}