package ipn.escom.defensoria.subdefensoria.service;

import ipn.escom.defensoria.subdefensoria.dto.CrearOficioDTO;
import ipn.escom.defensoria.subdefensoria.dto.OficioDTO;
import ipn.escom.defensoria.subdefensoria.entity.*;
import ipn.escom.defensoria.subdefensoria.exception.OperacionInvalidaException;
import ipn.escom.defensoria.subdefensoria.exception.RecursoNoEncontradoException;
import ipn.escom.defensoria.subdefensoria.repository.ExpedienteInvestigacionRepository;
import ipn.escom.defensoria.subdefensoria.repository.OficioInformacionRepository;
import ipn.escom.defensoria.subdefensoria.util.DiasHabilesCalculator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Redacta y envia los oficios de los dos ciclos del BPMN:
 * - TS-01 (fase SOLICITUD_INFORMACION), disparado cuando el
 *   expediente esta RECIBIDO.
 * - TS-04 (fase GESTION_DIRECTOR), disparado cuando el expediente
 *   esta EN_ESPERA_OFICIO y aun no tiene un oficio de esa fase.
 * La generacion real de PDF y el envio de correo quedan como campos
 * preparados en la entidad (rutaPdfGenerado, correoEnviado) para
 * conectar despues; aqui solo se persiste el contenido redactado.
 */
@Service
public class OficioService {

    private static final Logger log = LoggerFactory.getLogger(OficioService.class);

    private final OficioInformacionRepository oficioRepository;
    private final ExpedienteInvestigacionRepository expedienteRepository;
    private final int diasPrimeraSolicitud;
    private final TransicionExpedienteService transicionService;
    private final DocumentoService documentoService;
    private final CorreoUnidadAcademicaService correoService;

    public OficioService(
            OficioInformacionRepository oficioRepository,
            ExpedienteInvestigacionRepository expedienteRepository,
            @Value("${plazos.primera-solicitud-dias}") int diasPrimeraSolicitud,
            TransicionExpedienteService transicionService,
            DocumentoService documentoService,
            CorreoUnidadAcademicaService correoService
    ) {
        this.oficioRepository = oficioRepository;
        this.expedienteRepository = expedienteRepository;
        this.diasPrimeraSolicitud = diasPrimeraSolicitud;
        this.transicionService = transicionService;
        this.documentoService = documentoService;
        this.correoService = correoService;
    }

    public OficioDTO crearOficio(CrearOficioDTO dto) {

        ExpedienteInvestigacion expediente = expedienteRepository.findById(dto.getExpedienteId())
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No existe el expediente " + dto.getExpedienteId()));

        // Si se oficia sin haber abierto el detalle, primero pasa por EN_INVESTIGACION (fila 14).
        if (EstatusExpediente.RECIBIDO.equals(expediente.getEstatus())) {
            expediente = transicionService.cambiar(expediente, EstatusExpediente.EN_INVESTIGACION);
        }

        if (!EstatusExpediente.EN_INVESTIGACION.equals(expediente.getEstatus())) {
            throw new OperacionInvalidaException(
                    "El expediente " + expediente.getFolio() + " no admite generar un nuevo oficio en su estatus actual ("
                            + expediente.getEstatus() + ").");
        }
        if (oficioRepository.existsByExpedienteIdAndEstatus(expediente.getId(), EstatusOficio.EN_ESPERA)
                || oficioRepository.existsByExpedienteIdAndEstatus(expediente.getId(), EstatusOficio.VENCIDO)) {
            throw new OperacionInvalidaException(
                    "El expediente " + expediente.getFolio() + " ya tiene un oficio esperando respuesta.");
        }

        // Un solo tipo de oficio: la solicitud de información, cuya respuesta es la del
        // director. También es el que se emite de nuevo cuando el quejoso rechaza un acuerdo.
        String fase = FaseOficio.SOLICITUD_INFORMACION;

        LocalDate hoy = LocalDate.now();
        LocalDate fechaLimite = DiasHabilesCalculator.sumarDiasHabiles(hoy, diasPrimeraSolicitud);

        long consecutivo = oficioRepository.findByExpedienteIdOrderByFechaEnvioDesc(expediente.getId()).size() + 1;
        String numeroOficio = "OF%03d-%s-%03d".formatted(consecutivo, siglasUA(dto.getUnidadAcademica()), expediente.getId());

        OficioInformacion oficio = OficioInformacion.builder()
                .expedienteId(expediente.getId())
                .folio(expediente.getFolio())
                .numeroOficio(numeroOficio)
                .fase(fase)
                .destinatarioNombre(dto.getDestinatarioNombre())
                .destinatarioCorreo(dto.getDestinatarioCorreo())
                .unidadAcademica(dto.getUnidadAcademica())
                .contenidoRedactado(dto.getContenidoRedactado())
                .correoEnviado(false)
                .tipoPlazo("PRIMERA")
                .fechaEnvio(hoy)
                .fechaLimite(fechaLimite)
                .estatus(EstatusOficio.EN_ESPERA)
                .fechaCreacion(LocalDateTime.now())
                .build();

        OficioInformacion guardado = oficioRepository.save(oficio);

        // Fila 15: oficio enviado, se espera la respuesta.
        transicionService.cambiar(expediente, EstatusExpediente.EN_ESPERA_OFICIO);

        enviarPorCorreo(guardado, expediente);

        return convertirADTO(guardado);
    }

    /** Correo a la unidad académica con el oficio en PDF; si falla, el oficio ya está guardado. */
    private void enviarPorCorreo(OficioInformacion oficio, ExpedienteInvestigacion expediente) {
        try {
            boolean enviado = correoService.enviar(
                    oficio.getDestinatarioCorreo(),
                    "Oficio " + oficio.getNumeroOficio() + " — solicitud de información (" + expediente.getFolio() + ")",
                    "Estimado(a) " + oficio.getDestinatarioNombre() + ",\n\nSe adjunta el oficio "
                            + oficio.getNumeroOficio() + " con la solicitud de información del expediente "
                            + expediente.getFolio() + ". Le agradeceremos su respuesta a más tardar el "
                            + oficio.getFechaLimite() + ".",
                    documentoService.oficio(oficio.getId()));
            if (enviado) {
                oficio.setCorreoEnviado(true);
                oficioRepository.save(oficio);
            }
        } catch (Exception ex) {
            log.error("No se pudo enviar el oficio {} por correo: {}", oficio.getNumeroOficio(), ex.getMessage());
        }
    }

    public List<OficioDTO> historialPorFolio(String folio) {
        return oficioRepository.findByFolioOrderByFechaEnvioDesc(folio).stream()
                .map(this::convertirADTO)
                .toList();
    }

    public OficioDTO obtenerPorId(Long oficioId) {
        return oficioRepository.findById(oficioId)
                .map(this::convertirADTO)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe el oficio " + oficioId));
    }

    private String siglasUA(String unidadAcademica) {
        if (unidadAcademica == null || unidadAcademica.isBlank()) {
            return "UA";
        }
        return unidadAcademica.trim().toUpperCase().replaceAll("[^A-Z0-9]", "");
    }

    OficioDTO convertirADTO(OficioInformacion o) {
        return OficioDTO.builder()
                .id(o.getId())
                .expedienteId(o.getExpedienteId())
                .folio(o.getFolio())
                .numeroOficio(o.getNumeroOficio())
                .fase(o.getFase())
                .destinatarioNombre(o.getDestinatarioNombre())
                .destinatarioCorreo(o.getDestinatarioCorreo())
                .unidadAcademica(o.getUnidadAcademica())
                .contenidoRedactado(o.getContenidoRedactado())
                .tipoPlazo(o.getTipoPlazo())
                .fechaEnvio(o.getFechaEnvio() != null ? o.getFechaEnvio().toString() : null)
                .fechaLimite(o.getFechaLimite() != null ? o.getFechaLimite().toString() : null)
                .estatus(o.getEstatus())
                .build();
    }
}
