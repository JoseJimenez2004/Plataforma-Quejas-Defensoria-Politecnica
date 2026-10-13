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
 * CU-SD-09: los dos acuerdos con que termina la investigación (filas 17 a 20 del diagrama).
 *
 *   1. Acuerdo de CONCILIACIÓN (textoAcuerdo): el subdefensor lo redacta en ELABORO_ACUERDO
 *      (concluir=false guarda el borrador) y lo envía al quejoso (concluir=true) ->
 *      PENDIENTE_CONCILIACION. El quejoso lo acepta -> PENDIENTE_CONCLUSION, o lo rechaza ->
 *      EN_INVESTIGACION, para otro oficio de gestión y un nuevo acuerdo.
 *   2. Acuerdo de CONCLUSIÓN (textoConclusion): con el de conciliación aceptado, el
 *      subdefensor lo registra -> CONCLUIDO. Es el único camino a CONCLUIDO.
 *
 * Ambos viven en la misma fila de acuerdos_conclusion (una por expediente).
 */
@Service
public class AcuerdoConclusionService {

    private static final String RESPUESTA_ACEPTADO = "ACEPTADO";
    private static final String RESPUESTA_RECHAZADO = "RECHAZADO";

    private final AcuerdoConclusionRepository acuerdoRepository;
    private final ExpedienteInvestigacionRepository expedienteRepository;
    private final TransicionExpedienteService transicionService;

    public AcuerdoConclusionService(
            AcuerdoConclusionRepository acuerdoRepository,
            ExpedienteInvestigacionRepository expedienteRepository,
            TransicionExpedienteService transicionService
    ) {
        this.acuerdoRepository = acuerdoRepository;
        this.expedienteRepository = expedienteRepository;
        this.transicionService = transicionService;
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

        String estatus = expediente.getEstatus();
        if (Boolean.TRUE.equals(dto.getConcluir())) {
            // "concluir" significa ENVIAR el acuerdo de conciliación al quejoso (fila 17).
            // Si es un acuerdo nuevo tras un rechazo, la respuesta anterior se limpia.
            acuerdo.setFechaEnvioSecretarial(LocalDateTime.now());
            acuerdo.setRespuestaQuejoso(null);
            acuerdo.setComentarioQuejoso(null);
            acuerdo.setFechaRespuestaQuejoso(null);
            estatus = transicionService.cambiar(expediente, EstatusExpediente.PENDIENTE_CONCILIACION).getEstatus();
        }

        AcuerdoConclusion guardado = acuerdoRepository.save(acuerdo);

        return convertirADTO(guardado, estatus);
    }

    public AcuerdoConclusionDTO obtenerPorExpediente(Long expedienteId) {
        ExpedienteInvestigacion expediente = expedienteRepository.findById(expedienteId)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe el expediente " + expedienteId));

        return acuerdoRepository.findByExpedienteId(expedienteId)
                .map(a -> convertirADTO(a, expediente.getEstatus()))
                .orElse(null);
    }

    /**
     * El quejoso ACEPTA el acuerdo de conciliación (fila 19): queda pendiente el acuerdo de
     * conclusión que redacta el subdefensor.
     */
    public AcuerdoConclusionDTO aceptarPorQuejoso(Long expedienteId, String comentario) {
        ExpedienteInvestigacion expediente = exigirPendienteDeRespuesta(expedienteId);
        AcuerdoConclusion acuerdo = exigirAcuerdo(expedienteId);

        acuerdo.setRespuestaQuejoso(RESPUESTA_ACEPTADO);
        acuerdo.setComentarioQuejoso(comentario);
        acuerdo.setFechaRespuestaQuejoso(LocalDateTime.now());
        AcuerdoConclusion guardado = acuerdoRepository.save(acuerdo);

        transicionService.cambiar(expediente, EstatusExpediente.PENDIENTE_CONCLUSION);
        return convertirADTO(guardado, expediente.getEstatus());
    }

    /**
     * El subdefensor registra el acuerdo de CONCLUSIÓN (fila 20): el expediente se cierra
     * definitivamente. Solo con el acuerdo de conciliación aceptado.
     */
    public AcuerdoConclusionDTO registrarConclusion(Long expedienteId, String textoConclusion) {
        ExpedienteInvestigacion expediente = expedienteRepository.findById(expedienteId)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe el expediente " + expedienteId));

        if (!EstatusExpediente.PENDIENTE_CONCLUSION.equals(expediente.getEstatus())) {
            throw new OperacionInvalidaException(
                    "El expediente " + expediente.getFolio()
                            + " solo se concluye cuando el quejoso aceptó el acuerdo de conciliación (estatus actual: "
                            + expediente.getEstatus() + ").");
        }

        AcuerdoConclusion acuerdo = exigirAcuerdo(expedienteId);
        acuerdo.setTextoConclusion(textoConclusion.strip());
        acuerdo.setFechaConclusion(LocalDateTime.now());
        acuerdo.setConcluido(true);
        AcuerdoConclusion guardado = acuerdoRepository.save(acuerdo);

        transicionService.cambiar(expediente, EstatusExpediente.CONCLUIDO);
        return convertirADTO(guardado, expediente.getEstatus());
    }

    /**
     * El quejoso RECHAZA el acuerdo de conciliación (fila 18): el expediente regresa a
     * EN_INVESTIGACION para otro oficio de gestión y un nuevo acuerdo. El texto se conserva
     * como antecedente de lo que se le había propuesto.
     */
    public AcuerdoConclusionDTO rechazarPorQuejoso(Long expedienteId, String comentario) {
        ExpedienteInvestigacion expediente = exigirPendienteDeRespuesta(expedienteId);
        AcuerdoConclusion acuerdo = exigirAcuerdo(expedienteId);

        acuerdo.setRespuestaQuejoso(RESPUESTA_RECHAZADO);
        acuerdo.setComentarioQuejoso(comentario);
        acuerdo.setFechaRespuestaQuejoso(LocalDateTime.now());
        AcuerdoConclusion guardado = acuerdoRepository.save(acuerdo);

        transicionService.cambiar(expediente, EstatusExpediente.EN_INVESTIGACION);
        return convertirADTO(guardado, expediente.getEstatus());
    }

    private ExpedienteInvestigacion exigirPendienteDeRespuesta(Long expedienteId) {
        ExpedienteInvestigacion expediente = expedienteRepository.findById(expedienteId)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No existe el expediente " + expedienteId));

        if (!EstatusExpediente.PENDIENTE_CONCILIACION.equals(expediente.getEstatus())) {
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
                .textoConclusion(a.getTextoConclusion())
                .fechaConclusion(a.getFechaConclusion() != null ? a.getFechaConclusion().toString() : null)
                .build();
    }
}
