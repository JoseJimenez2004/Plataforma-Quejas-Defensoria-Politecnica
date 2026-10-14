package ipn.escom.defensoria.primercontacto.service;

import ipn.escom.defensoria.primercontacto.dto.SolicitudDenunciadoDTO;
import ipn.escom.defensoria.primercontacto.entity.EstatusExpediente;
import ipn.escom.defensoria.primercontacto.entity.ExpedientePrimerContacto;
import ipn.escom.defensoria.primercontacto.entity.PersonalAdministrativo;
import ipn.escom.defensoria.primercontacto.entity.QuejaReferencia;
import ipn.escom.defensoria.primercontacto.entity.SolicitudDenunciado;
import ipn.escom.defensoria.primercontacto.exception.OperacionInvalidaException;
import ipn.escom.defensoria.primercontacto.repository.QuejaReferenciaRepository;
import ipn.escom.defensoria.primercontacto.repository.SolicitudDenunciadoRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Le pide al denunciado su versión de los hechos: le manda por correo el enlace al formulario
 * del portal (/denunciado/respuesta?folio=FOL-...). La respuesta la guarda denunciado-service
 * por folio de la queja; Primer Contacto la consulta ahí (GET /api/denunciado/respuestas).
 *
 * Es una sola dirección por queja, como la diseñó denunciado-service: no hay enlace único ni
 * plazo. Reenviar solo vuelve a mandar el correo (a la misma dirección o a una corregida).
 */
@Service
public class SolicitudDenunciadoService {

    private static final String FIRMA = "\n\nDefensoría de los Derechos Politécnicos.";

    private final SolicitudDenunciadoRepository solicitudRepository;
    private final QuejaReferenciaRepository quejaRepository;
    private final TransicionExpedienteService transicionService;
    private final NotificacionQuejosoService notificacionService;
    private final String portalUrl;

    public SolicitudDenunciadoService(
            SolicitudDenunciadoRepository solicitudRepository,
            QuejaReferenciaRepository quejaRepository,
            TransicionExpedienteService transicionService,
            NotificacionQuejosoService notificacionService,
            @Value("${primer-contacto.portal.url:https://defensoria-escom.ddns.net}") String portalUrl
    ) {
        this.solicitudRepository = solicitudRepository;
        this.quejaRepository = quejaRepository;
        this.transicionService = transicionService;
        this.notificacionService = notificacionService;
        this.portalUrl = portalUrl.endsWith("/") ? portalUrl.substring(0, portalUrl.length() - 1) : portalUrl;
    }

    public List<SolicitudDenunciadoDTO> listar(String folio) {
        ExpedientePrimerContacto expediente = transicionService.obtenerPorFolio(folio);
        return solicitudRepository.findByExpedienteIdOrderByFechaEnvioDesc(expediente.getId()).stream()
                .map(this::convertirADTO)
                .toList();
    }

    public SolicitudDenunciadoDTO enviar(String folio, String correo, PersonalAdministrativo analista) {
        ExpedientePrimerContacto expediente = transicionService.obtenerPorFolio(folio);
        if (!EstatusExpediente.estaAbierto(expediente.getEstatus())) {
            throw new OperacionInvalidaException(
                    "Solo se le pide su versión al denunciado mientras el expediente está en análisis.");
        }
        String folioQueja = expediente.getFolioOrigen();
        if (folioQueja == null || folioQueja.isBlank()) {
            throw new OperacionInvalidaException("El expediente no tiene folio de queja.");
        }
        String destinatario = correo.trim();

        boolean enviado = notificacionService.enviarCorreoExterno(
                destinatario,
                "Defensoría de los Derechos Politécnicos — Solicitud de su versión de los hechos (" + folioQueja + ")",
                cuerpo(folioQueja));

        SolicitudDenunciado solicitud = solicitudRepository.save(SolicitudDenunciado.builder()
                .expedienteId(expediente.getId())
                .folio(expediente.getFolio())
                .folioQueja(folioQueja)
                .correoDenunciado(destinatario)
                .correoEnviado(enviado)
                .enviadoPorId(analista.getId())
                .enviadoPorNombre(analista.getNombreCompleto())
                .fechaEnvio(LocalDateTime.now())
                .build());

        if (!enviado) {
            throw new OperacionInvalidaException(
                    "Se registró la solicitud, pero el correo no se pudo enviar. Revisa la dirección e inténtalo de nuevo.");
        }
        return convertirADTO(solicitud);
    }

    private String cuerpo(String folioQueja) {
        String nombre = quejaRepository.findByNumeroFolio(folioQueja)
                .map(this::nombreDenunciado)
                .orElse(null);
        return "Estimado(a) " + (nombre != null ? nombre : "servidor(a) politécnico(a)") + ",\n\n"
                + "La Defensoría de los Derechos Politécnicos recibió una queja (folio " + folioQueja + ") "
                + "en la que se le menciona. Como parte del análisis del caso, y para conocer todas las "
                + "versiones de lo ocurrido, le solicitamos que comparta su versión de los hechos en el "
                + "siguiente formulario:\n\n"
                + portalUrl + "/denunciado/respuesta?folio=" + folioQueja + "\n\n"
                + "Le pedimos responder a la brevedad. Podrá adjuntar su credencial y, si lo desea, "
                + "evidencias que respalden su versión. La información se trata conforme al aviso de "
                + "privacidad que aparece en el formulario."
                + FIRMA;
    }

    private String nombreDenunciado(QuejaReferencia q) {
        String nombre = Stream.of(q.getNombreDenunciado(), q.getApellido1Denunciado(), q.getApellido2Denunciado())
                .filter(p -> p != null && !p.isBlank())
                .map(String::strip)
                .collect(Collectors.joining(" "));
        return nombre.isEmpty() ? null : nombre;
    }

    private SolicitudDenunciadoDTO convertirADTO(SolicitudDenunciado s) {
        return SolicitudDenunciadoDTO.builder()
                .id(s.getId())
                .folio(s.getFolio())
                .folioQueja(s.getFolioQueja())
                .correoDenunciado(s.getCorreoDenunciado())
                .correoEnviado(s.isCorreoEnviado())
                .enviadoPorNombre(s.getEnviadoPorNombre())
                .fechaEnvio(s.getFechaEnvio() != null ? s.getFechaEnvio().toString() : null)
                .build();
    }
}
