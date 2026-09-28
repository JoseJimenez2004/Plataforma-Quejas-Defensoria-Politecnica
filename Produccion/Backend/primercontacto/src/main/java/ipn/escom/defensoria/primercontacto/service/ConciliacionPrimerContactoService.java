package ipn.escom.defensoria.primercontacto.service;

import ipn.escom.defensoria.primercontacto.dto.AcuerdoConciliacionDTO;
import ipn.escom.defensoria.primercontacto.dto.CrearConciliacionDTO;
import ipn.escom.defensoria.primercontacto.entity.AcuerdoConciliacion;
import ipn.escom.defensoria.primercontacto.entity.EstatusExpediente;
import ipn.escom.defensoria.primercontacto.entity.ExpedientePrimerContacto;
import ipn.escom.defensoria.primercontacto.entity.PersonalAdministrativo;
import ipn.escom.defensoria.primercontacto.exception.OperacionInvalidaException;
import ipn.escom.defensoria.primercontacto.repository.AcuerdoConciliacionRepository;
import ipn.escom.defensoria.primercontacto.repository.ExpedientePrimerContactoRepository;
import ipn.escom.defensoria.primercontacto.repository.PersonalAdministrativoRepository;
import ipn.escom.defensoria.primercontacto.repository.QuejaReferenciaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Acuerdos de conciliación propuestos por Primer Contacto (CU-PC-10).
 *
 * Se reutiliza la tabla compartida acuerdos_conciliacion: el quejoso los ve y responde
 * desde su panel (queja-service) exactamente igual que los que emite Revisión.
 *
 *   PENDIENTE -> el analista no puede dictaminar hasta que el quejoso responda.
 *   RECHAZADO -> el análisis sigue normalmente.
 *   ACEPTADO  -> el caso es procedente y pasa a Subdefensoría para elaborar el acuerdo de
 *                conclusión. Esto ocurre solo: queja-service no avisa a Primer Contacto, así
 *                que las respuestas se revisan cada minuto y también al consultar la lista.
 */
@Service
public class ConciliacionPrimerContactoService {

    private static final Logger log = LoggerFactory.getLogger(ConciliacionPrimerContactoService.class);

    private final AcuerdoConciliacionRepository acuerdoRepository;
    private final ExpedientePrimerContactoRepository expedienteRepository;
    private final QuejaReferenciaRepository quejaRepository;
    private final PersonalAdministrativoRepository personalRepository;
    private final TransicionExpedienteService transicionService;
    private final DictamenPrimerContactoService dictamenService;
    private final NotificacionQuejosoService notificacionService;

    public ConciliacionPrimerContactoService(
            AcuerdoConciliacionRepository acuerdoRepository,
            ExpedientePrimerContactoRepository expedienteRepository,
            QuejaReferenciaRepository quejaRepository,
            PersonalAdministrativoRepository personalRepository,
            TransicionExpedienteService transicionService,
            DictamenPrimerContactoService dictamenService,
            NotificacionQuejosoService notificacionService
    ) {
        this.acuerdoRepository = acuerdoRepository;
        this.expedienteRepository = expedienteRepository;
        this.quejaRepository = quejaRepository;
        this.personalRepository = personalRepository;
        this.transicionService = transicionService;
        this.dictamenService = dictamenService;
        this.notificacionService = notificacionService;
    }

    public AcuerdoConciliacionDTO proponer(
            CrearConciliacionDTO dto,
            PersonalAdministrativo analista
    ) {

        ExpedientePrimerContacto expediente =
                transicionService.obtenerPorFolio(dto.getFolio());

        if (!EstatusExpediente.estaAbierto(expediente.getEstatus())) {
            throw new OperacionInvalidaException(
                    "Solo se propone conciliación mientras el expediente está en análisis."
            );
        }

        boolean hayPendiente = acuerdosDe(expediente).stream()
                .anyMatch(a -> AcuerdoConciliacion.PENDIENTE.equals(a.getEstado()));

        if (hayPendiente) {
            throw new OperacionInvalidaException(
                    "Ya hay un acuerdo esperando la respuesta del quejoso."
            );
        }

        /*
         * El quejoso consulta sus acuerdos por el correo con el que
         * registró la queja: se toma de la queja, igual que Revisión.
         */
        String correoQuejoso = quejaRepository
                .findByNumeroFolio(expediente.getFolioOrigen())
                .map(q -> q.getCorreoInstitucional())
                .orElse(expediente.getQuejosoCorreo());

        if (correoQuejoso == null || correoQuejoso.isBlank()) {
            throw new OperacionInvalidaException(
                    "La queja no tiene correo del quejoso; no puede recibir el acuerdo."
            );
        }

        AcuerdoConciliacion acuerdo = new AcuerdoConciliacion();
        acuerdo.setNumeroFolio(expediente.getFolioOrigen());
        acuerdo.setCorreoInstitucional(correoQuejoso);
        acuerdo.setAsunto(dto.getAsunto().trim());
        acuerdo.setTerminos(dto.getTerminos().trim());
        acuerdo.setEstado(AcuerdoConciliacion.PENDIENTE);
        acuerdo.setFechaEmision(LocalDateTime.now());
        acuerdo.setCreadoPor(analista.getCorreoInstitucional());

        AcuerdoConciliacion guardado = acuerdoRepository.save(acuerdo);

        notificacionService.notificar(
                expediente,
                NotificacionQuejosoService.TIPO_CONCILIACION,
                "Nuevo acuerdo de conciliación",
                "Primer Contacto te propone un acuerdo de conciliación para tu queja "
                        + expediente.getFolioOrigen() + ": \"" + guardado.getAsunto() + "\".\n"
                        + "Revísalo en la sección Conciliación de tu panel para aceptarlo o rechazarlo."
        );

        return convertirADTO(guardado);
    }

    public List<AcuerdoConciliacionDTO> listar(String folio) {

        ExpedientePrimerContacto expediente = transicionService.obtenerPorFolio(folio);

        sincronizar(expediente);

        return acuerdosDe(expediente).stream()
                .map(this::convertirADTO)
                .toList();
    }

    /**
     * Revisa si el quejoso aceptó algún acuerdo de los expedientes que siguen abiertos.
     */
    @Scheduled(
            fixedDelayString = "${primer-contacto.conciliacion.revision-ms:60000}",
            initialDelayString = "${primer-contacto.conciliacion.revision-ms:60000}"
    )
    public void revisarRespuestas() {

        List<ExpedientePrimerContacto> abiertos =
                expedienteRepository.findByEstatusIn(EstatusExpediente.ABIERTOS);

        if (abiertos.isEmpty()) {
            return;
        }

        List<String> folios = abiertos.stream()
                .map(ExpedientePrimerContacto::getFolioOrigen)
                .toList();

        if (acuerdoRepository.findByNumeroFolioInAndEstado(folios, AcuerdoConciliacion.ACEPTADO).isEmpty()) {
            return;
        }

        abiertos.forEach(this::sincronizar);
    }

    /*
     * synchronized: la revisión programada y una consulta del analista
     * podrían detectar la misma aceptación a la vez y turnar dos veces.
     */
    private synchronized void sincronizar(ExpedientePrimerContacto expediente) {

        ExpedientePrimerContacto actual =
                expedienteRepository.findById(expediente.getId()).orElse(null);

        if (actual == null || !EstatusExpediente.estaAbierto(actual.getEstatus())) {
            return;
        }

        acuerdosDe(actual).stream()
                .filter(a -> AcuerdoConciliacion.ACEPTADO.equals(a.getEstado()))
                .findFirst()
                .ifPresent(aceptado -> turnarPorConciliacion(actual, aceptado));
    }

    private void turnarPorConciliacion(
            ExpedientePrimerContacto expediente,
            AcuerdoConciliacion aceptado
    ) {

        PersonalAdministrativo autor = aceptado.getCreadoPor() == null
                ? null
                : personalRepository.findByCorreoInstitucional(aceptado.getCreadoPor()).orElse(null);

        Long analistaId = autor != null ? autor.getId() : expediente.getAnalistaAnalisisId();
        String analistaNombre = autor != null ? autor.getNombreCompleto() : expediente.getAnalistaAnalisisNombre();

        if (analistaId == null) {
            log.warn("No se pudo identificar al analista del acuerdo {} del expediente {}; no se turnó.",
                    aceptado.getId(), expediente.getFolio());
            return;
        }

        log.info("El quejoso aceptó el acuerdo {} del expediente {}; se turna a Subdefensoría.",
                aceptado.getId(), expediente.getFolio());

        dictamenService.registrarCompetenciaPorConciliacion(
                expediente,
                aceptado,
                analistaId,
                analistaNombre
        );
    }

    /*
     * Solo los acuerdos emitidos desde que la queja llegó a Primer
     * Contacto: los que Revisión haya emitido antes no son de esta etapa.
     */
    private List<AcuerdoConciliacion> acuerdosDe(ExpedientePrimerContacto expediente) {
        return acuerdoRepository
                .findByNumeroFolioAndFechaEmisionGreaterThanEqualOrderByFechaEmisionDesc(
                        expediente.getFolioOrigen(),
                        expediente.getFechaCreacion()
                );
    }

    private AcuerdoConciliacionDTO convertirADTO(AcuerdoConciliacion acuerdo) {

        String nombreAutor = acuerdo.getCreadoPor() == null
                ? null
                : personalRepository.findByCorreoInstitucional(acuerdo.getCreadoPor())
                .map(PersonalAdministrativo::getNombreCompleto)
                .orElse(acuerdo.getCreadoPor());

        return AcuerdoConciliacionDTO.builder()
                .id(acuerdo.getId())
                .numeroFolio(acuerdo.getNumeroFolio())
                .asunto(acuerdo.getAsunto())
                .terminos(acuerdo.getTerminos())
                .estado(acuerdo.getEstado())
                .fechaEmision(acuerdo.getFechaEmision() != null ? acuerdo.getFechaEmision().toString() : null)
                .fechaRespuesta(acuerdo.getFechaRespuesta() != null ? acuerdo.getFechaRespuesta().toString() : null)
                .comentarioQuejoso(acuerdo.getComentarioQuejoso())
                .creadoPor(acuerdo.getCreadoPor())
                .creadoPorNombre(nombreAutor)
                .build();
    }
}
