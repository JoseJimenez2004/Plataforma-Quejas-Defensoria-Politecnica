package ipn.escom.defensoria.revision_service.service;

import java.util.Collections;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import ipn.escom.defensoria.revision_service.entity.Queja;
import ipn.escom.defensoria.revision_service.entity.QuejaEvidencia;
import ipn.escom.defensoria.revision_service.model.PrimerContactoIngresoRequest;
import ipn.escom.defensoria.revision_service.model.PrimerContactoIngresoResponse;
import ipn.escom.defensoria.revision_service.repository.QuejaEvidenciaRepository;

@Service
public class PrimerContactoClientService {

    private final RestTemplate restTemplate;
    private final QuejaEvidenciaRepository evidenciaRepository;

    @Value("${primer-contacto.base-url}")
    private String primerContactoBaseUrl;

    public PrimerContactoClientService(
            RestTemplate restTemplate,
            QuejaEvidenciaRepository evidenciaRepository
    ) {
        this.restTemplate = restTemplate;
        this.evidenciaRepository = evidenciaRepository;
    }

    public PrimerContactoIngresoResponse enviarAPrimerContacto(
            Queja queja
    ) {

        /*
         * IMPORTANTE:
         * No enviamos queja.id a Primer Contacto.
         *
         * La relacion entre las etapas se realiza mediante:
         *
         * Quejas/Revision: FOL-...
         *        ->
         * Primer Contacto: folioOrigen = FOL-...
         */

        PrimerContactoIngresoRequest.QuejosoRequest quejoso =
                new PrimerContactoIngresoRequest.QuejosoRequest(
                        null,
                        construirNombreCompleto(queja),
                        queja.getCorreoInstitucional(),
                        null,
                        queja.getUnidadAcademicaClave(),
                        resolverTipoUsuario(queja)
                );

        List<QuejaEvidencia> evidenciasOrigen =
                queja.getId() == null
                        ? Collections.emptyList()
                        : evidenciaRepository.findByQuejaId(queja.getId());

        List<PrimerContactoIngresoRequest.EvidenciaRequest> evidencias =
                evidenciasOrigen.stream()
                        .map(this::convertirEvidencia)
                        .toList();

        PrimerContactoIngresoRequest request =
                new PrimerContactoIngresoRequest(
                        queja.getNumeroFolio(),
                        queja.getMotivo(),
                        queja.getDescripcion(),
                        resolverFechaRecepcion(queja),
                        null,
                        quejoso,
                        evidencias
                );

        PrimerContactoIngresoResponse response =
                restTemplate.postForObject(
                        primerContactoBaseUrl
                                + "/api/primer-contacto/ingesta/expedientes",
                        request,
                        PrimerContactoIngresoResponse.class
                );

        if (response == null) {
            throw new RuntimeException(
                    "Primer Contacto no devolvio informacion del expediente."
            );
        }

        if (response.getFolio() == null
                || response.getFolio().isBlank()) {
            throw new RuntimeException(
                    "Primer Contacto no devolvio un folio valido."
            );
        }

        return response;
    }

    private PrimerContactoIngresoRequest.EvidenciaRequest convertirEvidencia(
            QuejaEvidencia evidencia
    ) {

        return new PrimerContactoIngresoRequest.EvidenciaRequest(
                evidencia.getId(),
                evidencia.getNombreArchivo(),
                evidencia.getTipoMime(),
                null,
                evidencia.getFechaSubida() != null
                        ? evidencia.getFechaSubida().toString()
                        : null
        );
    }

    private String construirNombreCompleto(Queja queja) {

        StringBuilder nombre = new StringBuilder();

        agregarParteNombre(nombre, queja.getNombreQuejoso());
        agregarParteNombre(nombre, queja.getApellidoPaternoQuejoso());
        agregarParteNombre(nombre, queja.getApellidoMaternoQuejoso());

        String resultado = nombre.toString().trim();

        return resultado.isEmpty() ? null : resultado;
    }

    private void agregarParteNombre(
            StringBuilder nombre,
            String parte
    ) {

        if (parte != null && !parte.isBlank()) {

            if (nombre.length() > 0) {
                nombre.append(" ");
            }

            nombre.append(parte.trim());
        }
    }

    private String resolverTipoUsuario(Queja queja) {

        /*
         * Una queja registrada manualmente puede incluir
         * alumno, empleado o externo.
         */
        if ("MANUAL".equalsIgnoreCase(queja.getOrigenRegistro())
                && queja.getTipoUsuarioManual() != null
                && !queja.getTipoUsuarioManual().isBlank()) {

            return queja.getTipoUsuarioManual();
        }

        return queja.getTipoIdentificacionQuejoso();
    }

    private String resolverFechaRecepcion(Queja queja) {

        /*
         * Para documentos recibidos fisicamente usamos
         * la fecha real de recepcion.
         */
        if ("MANUAL".equalsIgnoreCase(queja.getOrigenRegistro())
                && queja.getFechaRecepcionFisica() != null) {

            return queja.getFechaRecepcionFisica().toString();
        }

        /*
         * Para quejas web/publicas usamos la fecha
         * en que la queja fue creada.
         */
        if (queja.getFechaCreacion() != null) {
            return queja.getFechaCreacion().toString();
        }

        return null;
    }
}