package ipn.escom.defensoria.subdefensoria.service;

import ipn.escom.defensoria.subdefensoria.dto.CrearOficioDTO;
import ipn.escom.defensoria.subdefensoria.dto.OficioDTO;
import ipn.escom.defensoria.subdefensoria.entity.*;
import ipn.escom.defensoria.subdefensoria.exception.OperacionInvalidaException;
import ipn.escom.defensoria.subdefensoria.exception.RecursoNoEncontradoException;
import ipn.escom.defensoria.subdefensoria.repository.ExpedienteInvestigacionRepository;
import ipn.escom.defensoria.subdefensoria.repository.OficioInformacionRepository;
import ipn.escom.defensoria.subdefensoria.util.DiasHabilesCalculator;
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
 *   esta EN_GESTION_DIRECTOR y aun no tiene un oficio de esa fase.
 * La generacion real de PDF y el envio de correo quedan como campos
 * preparados en la entidad (rutaPdfGenerado, correoEnviado) para
 * conectar despues; aqui solo se persiste el contenido redactado.
 */
@Service
public class OficioService {

    private final OficioInformacionRepository oficioRepository;
    private final ExpedienteInvestigacionRepository expedienteRepository;
    private final int diasPrimeraSolicitud;

    public OficioService(
            OficioInformacionRepository oficioRepository,
            ExpedienteInvestigacionRepository expedienteRepository,
            @Value("${plazos.primera-solicitud-dias}") int diasPrimeraSolicitud
    ) {
        this.oficioRepository = oficioRepository;
        this.expedienteRepository = expedienteRepository;
        this.diasPrimeraSolicitud = diasPrimeraSolicitud;
    }

    public OficioDTO crearOficio(CrearOficioDTO dto) {

        ExpedienteInvestigacion expediente = expedienteRepository.findById(dto.getExpedienteId())
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No existe el expediente " + dto.getExpedienteId()));

        String fase;
        String nuevoEstatusExpediente;

        if (EstatusExpediente.RECIBIDO.equals(expediente.getEstatus())) {
            fase = FaseOficio.SOLICITUD_INFORMACION;
            nuevoEstatusExpediente = EstatusExpediente.EN_INVESTIGACION;
        } else if (EstatusExpediente.EN_GESTION_DIRECTOR.equals(expediente.getEstatus())
                && !oficioRepository.existsByExpedienteIdAndEstatus(expediente.getId(), EstatusOficio.EN_ESPERA)
                && !oficioRepository.existsByExpedienteIdAndEstatus(expediente.getId(), EstatusOficio.VENCIDO)) {
            fase = FaseOficio.GESTION_DIRECTOR;
            nuevoEstatusExpediente = EstatusExpediente.EN_GESTION_DIRECTOR;
        } else {
            throw new OperacionInvalidaException(
                    "El expediente " + expediente.getFolio() + " no admite generar un nuevo oficio en su estatus actual ("
                            + expediente.getEstatus() + ").");
        }

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

        expediente.setEstatus(nuevoEstatusExpediente);
        expediente.setFechaActualizacion(LocalDateTime.now());
        expedienteRepository.save(expediente);

        return convertirADTO(guardado);
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
