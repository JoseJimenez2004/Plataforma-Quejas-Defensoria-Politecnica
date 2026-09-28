package ipn.escom.defensoria.subdefensoria.service;

import ipn.escom.defensoria.subdefensoria.dto.AcuerdoConclusionDTO;
import ipn.escom.defensoria.subdefensoria.dto.CrearAcuerdoConclusionDTO;
import ipn.escom.defensoria.subdefensoria.entity.AcuerdoConclusion;
import ipn.escom.defensoria.subdefensoria.entity.EstatusExpediente;
import ipn.escom.defensoria.subdefensoria.entity.ExpedienteInvestigacion;
import ipn.escom.defensoria.subdefensoria.exception.OperacionInvalidaException;
import ipn.escom.defensoria.subdefensoria.exception.RecursoNoEncontradoException;
import ipn.escom.defensoria.subdefensoria.repository.AcuerdoConclusionRepository;
import ipn.escom.defensoria.subdefensoria.repository.ExpedienteInvestigacionRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * TS-07/TS-08: redaccion del acuerdo de conclusion y su envio al quejoso.
 *
 * OJO con el cambio de semantica del 2026-09-18: antes, concluir=true pasaba el
 * expediente directo a CONCLUIDO. Ahora NO lo concluye: lo manda al quejoso y lo deja
 * en PENDIENTE_CONCLUSION. Quien concluye el expediente es el quejoso al aceptar el
 * acuerdo; si lo rechaza, el expediente regresa a EN_INVESTIGACION para otra ronda.
 * Asi lo exige el diagrama de estados, y tiene sentido: un acuerdo de conclusion que se
 * cierra sin que el quejoso opine no es un acuerdo.
 *
 * concluir=false sigue guardando solo el borrador.
 */
@Service
public class AcuerdoConclusionService {

    private static final String RESPUESTA_ACEPTADO = "ACEPTADO";
    private static final String RESPUESTA_RECHAZADO = "RECHAZADO";

    private final AcuerdoConclusionRepository acuerdoRepository;
    private final ExpedienteInvestigacionRepository expedienteRepository;

    public AcuerdoConclusionService(
            AcuerdoConclusionRepository acuerdoRepository,
            ExpedienteInvestigacionRepository expedienteRepository
    ) {
        this.acuerdoRepository = acuerdoRepository;
        this.expedienteRepository = expedienteRepository;
    }

    public AcuerdoConclusionDTO guardarOConcluir(CrearAcuerdoConclusionDTO dto) {

        ExpedienteInvestigacion expediente = expedienteRepository.findById(dto.getExpedienteId())
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No existe el expediente " + dto.getExpedienteId()));

        if (!EstatusExpediente.ELABORO_ACUERDO.equals(expediente.getEstatus())) {
            throw new OperacionInvalidaException(
                    "El expediente " + expediente.getFolio()
                            + " no está listo para dictaminar (estatus actual: " + expediente.getEstatus() + ").");
        }

        AcuerdoConclusion acuerdo = acuerdoRepository.findByExpedienteId(expediente.getId())
                .orElseGet(() -> AcuerdoConclusion.builder()
                        .expedienteId(expediente.getId())
                        .folio(expediente.getFolio())
                        .concluido(false)
                        .fechaCreacion(LocalDateTime.now())
                        .build());

        if (Boolean.TRUE.equals(acuerdo.getConcluido())) {
            throw new OperacionInvalidaException(
                    "El expediente " + expediente.getFolio() + " ya fue concluido.");
        }

        acuerdo.setTextoAcuerdo(dto.getTextoAcuerdo());

        if (Boolean.TRUE.equals(dto.getConcluir())) {
            // "concluir" aqui significa ENVIAR el acuerdo al quejoso. El expediente queda
            // esperando su respuesta; concluido sigue en false hasta que el quejoso acepte.
            acuerdo.setFechaEnvioSecretarial(LocalDateTime.now());
            expediente.setEstatus(EstatusExpediente.PENDIENTE_CONCLUSION);
            expediente.setFechaActualizacion(LocalDateTime.now());
            expedienteRepository.save(expediente);
        }

        AcuerdoConclusion guardado = acuerdoRepository.save(acuerdo);

        return convertirADTO(guardado, expediente.getEstatus());
    }

    public AcuerdoConclusionDTO obtenerPorExpediente(Long expedienteId) {
        ExpedienteInvestigacion expediente = expedienteRepository.findById(expedienteId)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe el expediente " + expedienteId));

        return acuerdoRepository.findByExpedienteId(expedienteId)
                .map(a -> convertirADTO(a, expediente.getEstatus()))
                .orElse(null);
    }

    /**
     * El quejoso ACEPTA el acuerdo: el expediente se concluye. Es el unico camino por el
     * que un expediente llega a CONCLUIDO.
     */
    public AcuerdoConclusionDTO aceptarPorQuejoso(Long expedienteId, String comentario) {
        ExpedienteInvestigacion expediente = exigirPendienteDeRespuesta(expedienteId);
        AcuerdoConclusion acuerdo = exigirAcuerdo(expedienteId);

        acuerdo.setConcluido(true);
        acuerdo.setRespuestaQuejoso(RESPUESTA_ACEPTADO);
        acuerdo.setComentarioQuejoso(comentario);
        acuerdo.setFechaRespuestaQuejoso(LocalDateTime.now());

        expediente.setEstatus(EstatusExpediente.CONCLUIDO);
        expediente.setFechaActualizacion(LocalDateTime.now());
        expedienteRepository.save(expediente);

        return convertirADTO(acuerdoRepository.save(acuerdo), expediente.getEstatus());
    }

    /**
     * El quejoso RECHAZA el acuerdo: el expediente regresa a EN_INVESTIGACION para otra
     * ronda de oficios. El texto del acuerdo se conserva como antecedente de lo que se le
     * habia propuesto.
     */
    public AcuerdoConclusionDTO rechazarPorQuejoso(Long expedienteId, String comentario) {
        ExpedienteInvestigacion expediente = exigirPendienteDeRespuesta(expedienteId);
        AcuerdoConclusion acuerdo = exigirAcuerdo(expedienteId);

        acuerdo.setConcluido(false);
        acuerdo.setRespuestaQuejoso(RESPUESTA_RECHAZADO);
        acuerdo.setComentarioQuejoso(comentario);
        acuerdo.setFechaRespuestaQuejoso(LocalDateTime.now());

        expediente.setEstatus(EstatusExpediente.EN_INVESTIGACION);
        expediente.setFechaActualizacion(LocalDateTime.now());
        expedienteRepository.save(expediente);

        return convertirADTO(acuerdoRepository.save(acuerdo), expediente.getEstatus());
    }

    private ExpedienteInvestigacion exigirPendienteDeRespuesta(Long expedienteId) {
        ExpedienteInvestigacion expediente = expedienteRepository.findById(expedienteId)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No existe el expediente " + expedienteId));

        if (!EstatusExpediente.PENDIENTE_CONCLUSION.equals(expediente.getEstatus())) {
            throw new OperacionInvalidaException(
                    "El expediente " + expediente.getFolio()
                            + " no tiene un acuerdo esperando respuesta (estatus actual: "
                            + expediente.getEstatus() + ").");
        }
        return expediente;
    }

    private AcuerdoConclusion exigirAcuerdo(Long expedienteId) {
        return acuerdoRepository.findByExpedienteId(expedienteId)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "El expediente " + expedienteId + " no tiene acuerdo de conclusion."));
    }

    private AcuerdoConclusionDTO convertirADTO(AcuerdoConclusion a, String estatusExpediente) {
        return AcuerdoConclusionDTO.builder()
                .id(a.getId())
                .expedienteId(a.getExpedienteId())
                .folio(a.getFolio())
                .textoAcuerdo(a.getTextoAcuerdo())
                .concluido(a.getConcluido())
                .fechaCreacion(a.getFechaCreacion() != null ? a.getFechaCreacion().toString() : null)
                .fechaEnvioSecretarial(a.getFechaEnvioSecretarial() != null ? a.getFechaEnvioSecretarial().toString() : null)
                .estatusExpediente(estatusExpediente)
                .respuestaQuejoso(a.getRespuestaQuejoso())
                .comentarioQuejoso(a.getComentarioQuejoso())
                .fechaRespuestaQuejoso(a.getFechaRespuestaQuejoso() != null
                        ? a.getFechaRespuestaQuejoso().toString() : null)
                .build();
    }
}
