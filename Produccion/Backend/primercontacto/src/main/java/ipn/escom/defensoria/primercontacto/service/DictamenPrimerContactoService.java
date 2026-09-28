package ipn.escom.defensoria.primercontacto.service;

import ipn.escom.defensoria.primercontacto.dto.CompetenciaDTO;
import ipn.escom.defensoria.primercontacto.dto.DictamenDTO;
import ipn.escom.defensoria.primercontacto.dto.ExpedienteEntranteRequest;
import ipn.escom.defensoria.primercontacto.dto.ImprocedenciaDTO;
import ipn.escom.defensoria.primercontacto.dto.QuejosoResumenRequest;
import ipn.escom.defensoria.primercontacto.dto.SubdefensoriaIngresoResponse;
import ipn.escom.defensoria.primercontacto.entity.AcuerdoConciliacion;
import ipn.escom.defensoria.primercontacto.entity.DictamenPrimerContacto;
import ipn.escom.defensoria.primercontacto.entity.EstatusExpediente;
import ipn.escom.defensoria.primercontacto.entity.ExpedientePrimerContacto;
import ipn.escom.defensoria.primercontacto.entity.PersonalAdministrativo;
import ipn.escom.defensoria.primercontacto.exception.OperacionInvalidaException;
import ipn.escom.defensoria.primercontacto.exception.RecursoNoEncontradoException;
import ipn.escom.defensoria.primercontacto.repository.AcuerdoConciliacionRepository;
import ipn.escom.defensoria.primercontacto.repository.DictamenPrimerContactoRepository;
import ipn.escom.defensoria.primercontacto.repository.ExpedientePrimerContactoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Dictámenes de Primer Contacto (CU-PC-06, 07 y 08).
 *
 *   Competencia:   EN_ANALISIS -> PROCEDENTE, y luego se envía a Subdefensoría.
 *   Improcedencia: EN_ANALISIS -> IMPROCEDENTE; el flujo sigue en la remisión externa
 *                  (IMPROCEDENTE -> REMITIDA), ya no es un cierre por sí sola.
 *
 * Quién dictaminó sale siempre del JWT (analistaId/analistaNombre), nunca del payload.
 */
@Service
public class DictamenPrimerContactoService {

    private static final Logger log = LoggerFactory.getLogger(DictamenPrimerContactoService.class);

    public static final String RESULTADO_COMPETENTE = "COMPETENTE";
    public static final String RESULTADO_IMPROCEDENTE = "IMPROCEDENTE";

    private final DictamenPrimerContactoRepository dictamenRepository;
    private final ExpedientePrimerContactoRepository expedienteRepository;
    private final AcuerdoConciliacionRepository acuerdoRepository;
    private final SubdefensoriaClientService subdefensoriaClientService;
    private final TransicionExpedienteService transicionService;
    private final NotificacionQuejosoService notificacionService;

    public DictamenPrimerContactoService(
            DictamenPrimerContactoRepository dictamenRepository,
            ExpedientePrimerContactoRepository expedienteRepository,
            AcuerdoConciliacionRepository acuerdoRepository,
            SubdefensoriaClientService subdefensoriaClientService,
            TransicionExpedienteService transicionService,
            NotificacionQuejosoService notificacionService
    ) {
        this.dictamenRepository = dictamenRepository;
        this.expedienteRepository = expedienteRepository;
        this.acuerdoRepository = acuerdoRepository;
        this.subdefensoriaClientService = subdefensoriaClientService;
        this.transicionService = transicionService;
        this.notificacionService = notificacionService;
    }

    /*
     * Sin @Transactional a propósito: el dictamen y el estado PROCEDENTE
     * deben quedar guardados AUNQUE Subdefensoría no responda. Antes todo
     * iba en una sola transacción y un fallo de red borraba el dictamen.
     * Si el envío falla, el expediente queda PROCEDENTE sin
     * folioSubdefensoria y se reintenta con reenviarASubdefensoria().
     */
    public DictamenDTO registrarCompetencia(
            CompetenciaDTO dto,
            PersonalAdministrativo analista
    ) {

        ExpedientePrimerContacto expediente =
                transicionService.obtenerPorFolio(dto.getFolio());

        validarQueSePuedeDictaminar(expediente);

        DictamenPrimerContacto guardado =
                guardarDictamen(
                        expediente,
                        analista.getId(),
                        analista.getNombreCompleto(),
                        RESULTADO_COMPETENTE,
                        dto.getJustificacion(),
                        dto.getAreaTurno(),
                        dto.getResponsableTurno(),
                        dto.getObservaciones()
                );

        turnarComoProcedente(expediente, guardado);

        return convertirADTO(guardado);
    }

    public DictamenDTO registrarImprocedencia(
            ImprocedenciaDTO dto,
            PersonalAdministrativo analista
    ) {

        ExpedientePrimerContacto expediente =
                transicionService.obtenerPorFolio(dto.getFolio());

        validarQueSePuedeDictaminar(expediente);

        DictamenPrimerContacto guardado =
                guardarDictamen(
                        expediente,
                        analista.getId(),
                        analista.getNombreCompleto(),
                        RESULTADO_IMPROCEDENTE,
                        dto.getJustificacion(),
                        null,
                        null,
                        null
                );

        transicionService.cambiarEstatus(
                expediente,
                EstatusExpediente.IMPROCEDENTE
        );

        notificacionService.notificar(
                expediente,
                NotificacionQuejosoService.TIPO_CAMBIO_ESTATUS,
                "Tu queja fue dictaminada como improcedente",
                "Después de analizar tu queja " + expediente.getFolioOrigen()
                        + ", Primer Contacto determinó que el asunto no es competencia de la "
                        + "Defensoría de los Derechos Politécnicos. Tu caso será remitido a la "
                        + "instancia que puede atenderlo; te avisaremos en cuanto se envíe la remisión."
        );

        return convertirADTO(guardado);
    }

    /**
     * Dictamen automático cuando el quejoso ACEPTA un acuerdo de conciliación propuesto por
     * Primer Contacto (CU-PC-10): el asunto es procedente y pasa a Subdefensoría para
     * elaborar el acuerdo de conclusión. Queda firmado por quien propuso el acuerdo.
     */
    public void registrarCompetenciaPorConciliacion(
            ExpedientePrimerContacto expediente,
            AcuerdoConciliacion acuerdo,
            Long analistaId,
            String analistaNombre
    ) {

        if (dictamenRepository.existsByExpedienteId(expediente.getId())) {
            return;
        }

        DictamenPrimerContacto guardado =
                guardarDictamen(
                        expediente,
                        analistaId,
                        analistaNombre,
                        RESULTADO_COMPETENTE,
                        "El quejoso aceptó el acuerdo de conciliación \""
                                + acuerdo.getAsunto() + "\" el "
                                + acuerdo.getFechaRespuesta().toLocalDate()
                                + ". Procede elaborar el acuerdo de conclusión.",
                        "Subdefensoría",
                        null,
                        "Acuerdo de conciliación aceptado. Términos: " + acuerdo.getTerminos()
                                + (acuerdo.getComentarioQuejoso() != null
                                ? " | Comentario del quejoso: " + acuerdo.getComentarioQuejoso()
                                : "")
                );

        turnarComoProcedente(expediente, guardado);
    }

    /**
     * Reintenta el envío a Subdefensoría de un expediente PROCEDENTE que se quedó sin
     * folio SD- porque Subdefensoría no respondió en su momento.
     */
    public DictamenDTO reenviarASubdefensoria(String folio) {

        ExpedientePrimerContacto expediente =
                transicionService.obtenerPorFolio(folio);

        if (!EstatusExpediente.PROCEDENTE.equals(expediente.getEstatus())) {
            throw new OperacionInvalidaException(
                    "Solo se puede reenviar a Subdefensoría un expediente procedente."
            );
        }

        if (expediente.getFolioSubdefensoria() != null) {
            throw new OperacionInvalidaException(
                    "El expediente ya fue recibido por Subdefensoría con folio "
                            + expediente.getFolioSubdefensoria() + "."
            );
        }

        DictamenPrimerContacto dictamen =
                dictamenRepository.findByExpedienteId(expediente.getId())
                        .orElseThrow(() -> new RecursoNoEncontradoException(
                                "El expediente no tiene dictamen registrado."
                        ));

        if (!enviarASubdefensoria(expediente, dictamen)) {
            throw new OperacionInvalidaException(
                    "Subdefensoría sigue sin responder. Intenta de nuevo en unos minutos."
            );
        }

        return convertirADTO(dictamen);
    }

    public DictamenDTO obtenerPorExpediente(
            Long expedienteId
    ) {

        return dictamenRepository
                .findByExpedienteId(expedienteId)
                .map(this::convertirADTO)
                .orElseThrow(() ->
                        new RecursoNoEncontradoException(
                                "Dictamen no encontrado"
                        )
                );
    }

    public DictamenDTO obtenerPorFolio(
            String folio
    ) {

        return dictamenRepository
                .findByFolio(folio)
                .map(this::convertirADTO)
                .orElseThrow(() ->
                        new RecursoNoEncontradoException(
                                "El expediente " + folio + " todavía no tiene dictamen registrado."
                        )
                );
    }

    private void validarQueSePuedeDictaminar(
            ExpedientePrimerContacto expediente
    ) {

        /*
         * Un expediente solamente puede tener
         * un dictamen de Primer Contacto.
         */
        if (dictamenRepository.existsByExpedienteId(expediente.getId())) {
            throw new OperacionInvalidaException(
                    "El expediente " + expediente.getFolio()
                            + " ya cuenta con un dictamen registrado. Consúltalo en lugar de registrar otro."
            );
        }

        if (!EstatusExpediente.estaAbierto(expediente.getEstatus())) {
            throw new OperacionInvalidaException(
                    "El expediente ya no está en análisis (estatus "
                            + expediente.getEstatus() + ")."
            );
        }

        boolean conciliacionPendiente =
                acuerdoRepository
                        .findByNumeroFolioAndFechaEmisionGreaterThanEqualOrderByFechaEmisionDesc(
                                expediente.getFolioOrigen(),
                                expediente.getFechaCreacion()
                        )
                        .stream()
                        .anyMatch(a -> AcuerdoConciliacion.PENDIENTE.equals(a.getEstado()));

        if (conciliacionPendiente) {
            throw new OperacionInvalidaException(
                    "Hay un acuerdo de conciliación esperando la respuesta del quejoso. "
                            + "Espera su respuesta antes de dictaminar."
            );
        }
    }

    private DictamenPrimerContacto guardarDictamen(
            ExpedientePrimerContacto expediente,
            Long analistaId,
            String analistaNombre,
            String resultado,
            String justificacion,
            String areaTurno,
            String responsableTurno,
            String observaciones
    ) {

        DictamenPrimerContacto dictamen =
                DictamenPrimerContacto.builder()
                        .expedienteId(expediente.getId())
                        .folio(expediente.getFolio())
                        .analistaId(analistaId)
                        .analistaNombre(analistaNombre)
                        .resultado(resultado)
                        .justificacion(justificacion)
                        .areaTurno(areaTurno)
                        .responsableTurno(responsableTurno)
                        .fechaDictamen(LocalDateTime.now())
                        .observaciones(observaciones)
                        .build();

        return dictamenRepository.save(dictamen);
    }

    /*
     * Paso 1: queda registrado el dictamen como PROCEDENTE.
     * Paso 2: Subdefensoría lo recibe (Recibida en Subdefensoría).
     */
    private void turnarComoProcedente(
            ExpedientePrimerContacto expediente,
            DictamenPrimerContacto dictamen
    ) {

        transicionService.cambiarEstatus(
                expediente,
                EstatusExpediente.PROCEDENTE
        );

        notificacionService.notificar(
                expediente,
                NotificacionQuejosoService.TIPO_CAMBIO_ESTATUS,
                "Tu queja fue admitida como procedente",
                "Tu queja " + expediente.getFolioOrigen()
                        + " fue analizada por Primer Contacto y se determinó que es competencia "
                        + "de la Defensoría de los Derechos Politécnicos. Se turnó a Subdefensoría, "
                        + "que dará seguimiento a tu caso."
        );

        enviarASubdefensoria(expediente, dictamen);
    }

    private boolean enviarASubdefensoria(
            ExpedientePrimerContacto expediente,
            DictamenPrimerContacto dictamen
    ) {

        /*
         * Construimos únicamente la información que
         * Subdefensoría necesita.
         *
         * La relación entre áreas se realiza mediante
         * el folio PC-..., nunca mediante IDs.
         */
        QuejosoResumenRequest quejoso =
                QuejosoResumenRequest.builder()
                        .nombreCompleto(expediente.getQuejosoNombre())
                        .correo(expediente.getQuejosoCorreo())
                        .unidadAcademica(expediente.getUnidadAcademica())
                        .build();

        ExpedienteEntranteRequest solicitud =
                ExpedienteEntranteRequest.builder()
                        .folioOrigen(expediente.getFolio())
                        .asunto(expediente.getTema())
                        .descripcionHechos(expediente.getDescripcionHechos())
                        .fechaAdmision(LocalDate.now())
                        .abogadoAsesorId(null)
                        .abogadoAsesorNombre(dictamen.getResponsableTurno())
                        .quejoso(quejoso)
                        .observacionesAnalista(dictamen.getObservaciones())
                        .build();

        try {
            SubdefensoriaIngresoResponse respuesta =
                    subdefensoriaClientService.enviarExpediente(solicitud);

            /*
             * Guardamos solamente el folio generado por
             * Subdefensoría. NO guardamos su id interno.
             */
            expediente.setFolioSubdefensoria(respuesta.getFolio());
            expediente.setFechaActualizacion(LocalDateTime.now());
            expedienteRepository.save(expediente);

            return true;

        } catch (RuntimeException ex) {
            log.error("El expediente {} quedó PROCEDENTE pero no se pudo enviar a Subdefensoría: {}",
                    expediente.getFolio(), ex.getMessage());
            return false;
        }
    }

    private DictamenDTO convertirADTO(
            DictamenPrimerContacto dictamen
    ) {

        ExpedientePrimerContacto expediente =
                expedienteRepository.findById(dictamen.getExpedienteId()).orElse(null);

        return DictamenDTO.builder()
                .id(dictamen.getId())
                .expedienteId(dictamen.getExpedienteId())
                .folio(dictamen.getFolio())
                .analistaId(dictamen.getAnalistaId())
                .analistaNombre(dictamen.getAnalistaNombre())
                .resultado(dictamen.getResultado())
                .justificacion(dictamen.getJustificacion())
                .areaTurno(dictamen.getAreaTurno())
                .responsableTurno(dictamen.getResponsableTurno())
                .fechaDictamen(
                        dictamen.getFechaDictamen() != null
                                ? dictamen.getFechaDictamen().toString()
                                : null
                )
                .observaciones(dictamen.getObservaciones())
                .estatusExpediente(expediente != null ? expediente.getEstatus() : null)
                .folioSubdefensoria(expediente != null ? expediente.getFolioSubdefensoria() : null)
                .build();
    }
}
