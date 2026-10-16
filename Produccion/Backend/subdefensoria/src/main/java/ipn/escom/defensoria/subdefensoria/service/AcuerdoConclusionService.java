package ipn.escom.defensoria.subdefensoria.service;

import ipn.escom.defensoria.subdefensoria.dto.AcuerdoConclusionDTO;
import ipn.escom.defensoria.subdefensoria.dto.AcuerdoPublicoDTO;
import ipn.escom.defensoria.subdefensoria.dto.CrearAcuerdoConclusionDTO;
import ipn.escom.defensoria.subdefensoria.entity.AcuerdoConclusion;
import ipn.escom.defensoria.subdefensoria.entity.EstatusExpediente;
import ipn.escom.defensoria.subdefensoria.entity.ExpedienteInvestigacion;
import ipn.escom.defensoria.subdefensoria.exception.OperacionInvalidaException;
import ipn.escom.defensoria.subdefensoria.exception.RecursoNoEncontradoException;
import ipn.escom.defensoria.subdefensoria.repository.AcuerdoConclusionRepository;
import ipn.escom.defensoria.subdefensoria.repository.ExpedienteInvestigacionRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

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
 *
 * El quejoso responde el de conciliación desde el ENLACE que le llega por correo y en su
 * centro de notificaciones (/acuerdo/responder?token=..., sin iniciar sesión, CU-QJ-14).
 * Cada envío genera un token nuevo: el enlace de un acuerdo anterior deja de servir.
 */
@Service
public class AcuerdoConclusionService {

    private static final String RESPUESTA_ACEPTADO = "ACEPTADO";
    private static final String RESPUESTA_RECHAZADO = "RECHAZADO";

    private final AcuerdoConclusionRepository acuerdoRepository;
    private final ExpedienteInvestigacionRepository expedienteRepository;
    private final TransicionExpedienteService transicionService;
    private final NotificacionQuejosoService notificacionService;
    private final String portalUrl;

    /* Página pública del portal del quejoso donde acepta o rechaza el acuerdo. */
    private static final String ENLACE_RESPUESTA = "/acuerdo/responder?token=";

    public AcuerdoConclusionService(
            AcuerdoConclusionRepository acuerdoRepository,
            ExpedienteInvestigacionRepository expedienteRepository,
            TransicionExpedienteService transicionService,
            NotificacionQuejosoService notificacionService,
            @Value("${subdefensoria.portal.url:https://defensoria-escom.ddns.net}") String portalUrl
    ) {
        this.acuerdoRepository = acuerdoRepository;
        this.expedienteRepository = expedienteRepository;
        this.transicionService = transicionService;
        this.notificacionService = notificacionService;
        this.portalUrl = portalUrl == null ? "" : portalUrl.strip().replaceAll("/+$", "");
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
            acuerdo.setTokenRespuesta(nuevoToken());
            estatus = transicionService.cambiar(expediente, EstatusExpediente.PENDIENTE_CONCILIACION).getEstatus();
        }

        AcuerdoConclusion guardado = acuerdoRepository.save(acuerdo);

        if (Boolean.TRUE.equals(dto.getConcluir())) {
            String enlace = ENLACE_RESPUESTA + guardado.getTokenRespuesta();
            notificacionService.notificar(expediente, NotificacionQuejosoService.TIPO_CONCILIACION,
                    "Tienes un acuerdo de conciliación por responder",
                    "Subdefensoría te propone el siguiente acuerdo para resolver tu queja:\n\n"
                            + guardado.getTextoAcuerdo()
                            + "\n\nPuedes aceptarlo o rechazarlo indicando el motivo en este enlace:\n"
                            + portalUrl + enlace
                            + "\n\nSi lo rechazas, la Defensoría continuará la investigación para buscar otro acuerdo.",
                    enlace);
        }

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

        notificacionService.notificar(expediente, NotificacionQuejosoService.TIPO_CAMBIO_ESTATUS,
                "Tu caso ha concluido",
                "La Defensoría emitió el acuerdo de conclusión de tu queja:\n\n" + guardado.getTextoConclusion()
                        + "\n\nGracias por confiar en la Defensoría de los Derechos Politécnicos.",
                null);

        return convertirADTO(guardado, expediente.getEstatus());
    }

    // ---------------------------------------------------------------------------------
    // Respuesta del quejoso desde el enlace del correo / notificación (CU-QJ-14).
    // ---------------------------------------------------------------------------------

    public AcuerdoPublicoDTO acuerdoPorToken(String token) {
        return convertirAPublico(obtenerPorToken(token));
    }

    public AcuerdoPublicoDTO aceptarPorToken(String token, String comentario) {
        AcuerdoConclusion acuerdo = obtenerPorToken(token);
        aceptarPorQuejoso(acuerdo.getExpedienteId(), limpiar(comentario));
        return convertirAPublico(exigirAcuerdo(acuerdo.getExpedienteId()));
    }

    public AcuerdoPublicoDTO rechazarPorToken(String token, String motivo) {
        AcuerdoConclusion acuerdo = obtenerPorToken(token);
        if (motivo == null || motivo.isBlank()) {
            throw new OperacionInvalidaException("Indica el motivo por el que rechazas el acuerdo.");
        }
        rechazarPorQuejoso(acuerdo.getExpedienteId(), motivo.strip());
        return convertirAPublico(exigirAcuerdo(acuerdo.getExpedienteId()));
    }

    private AcuerdoConclusion obtenerPorToken(String token) {
        if (token == null || token.isBlank()) {
            throw new RecursoNoEncontradoException("El enlace del acuerdo no es válido.");
        }
        return acuerdoRepository.findByTokenRespuesta(token.strip())
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "El enlace del acuerdo no es válido o ya fue sustituido por uno más reciente."));
    }

    private AcuerdoPublicoDTO convertirAPublico(AcuerdoConclusion a) {
        ExpedienteInvestigacion expediente = expedienteRepository.findById(a.getExpedienteId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Acuerdo no encontrado."));
        return AcuerdoPublicoDTO.builder()
                .folioQueja(expediente.getFolioQueja() != null ? expediente.getFolioQueja() : expediente.getFolio())
                .quejosoNombre(expediente.getQuejosoNombre())
                .asunto(expediente.getAsunto())
                .textoAcuerdo(a.getTextoAcuerdo())
                .fechaEnvio(a.getFechaEnvioSecretarial() != null ? a.getFechaEnvioSecretarial().toString() : null)
                .respuestaQuejoso(a.getRespuestaQuejoso())
                .comentarioQuejoso(a.getComentarioQuejoso())
                .fechaRespuesta(a.getFechaRespuestaQuejoso() != null ? a.getFechaRespuestaQuejoso().toString() : null)
                .concluido(Boolean.TRUE.equals(a.getConcluido()))
                .puedeResponder(EstatusExpediente.PENDIENTE_CONCILIACION.equals(expediente.getEstatus())
                        && a.getRespuestaQuejoso() == null)
                .build();
    }

    private static String nuevoToken() {
        return UUID.randomUUID().toString().replace("-", "") + UUID.randomUUID().toString().replace("-", "");
    }

    private static String limpiar(String texto) {
        return texto == null || texto.isBlank() ? null : texto.strip();
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
