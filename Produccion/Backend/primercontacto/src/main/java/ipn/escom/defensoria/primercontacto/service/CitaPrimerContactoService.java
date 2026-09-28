package ipn.escom.defensoria.primercontacto.service;

import ipn.escom.defensoria.primercontacto.dto.CitaDTO;
import ipn.escom.defensoria.primercontacto.dto.CrearCitaDTO;
import ipn.escom.defensoria.primercontacto.dto.ReagendarCitaDTO;
import ipn.escom.defensoria.primercontacto.entity.CitaPrimerContacto;
import ipn.escom.defensoria.primercontacto.entity.EstatusExpediente;
import ipn.escom.defensoria.primercontacto.entity.ExpedientePrimerContacto;
import ipn.escom.defensoria.primercontacto.entity.PersonalAdministrativo;
import ipn.escom.defensoria.primercontacto.exception.OperacionInvalidaException;
import ipn.escom.defensoria.primercontacto.exception.RecursoNoEncontradoException;
import ipn.escom.defensoria.primercontacto.repository.CitaPrimerContactoRepository;
import ipn.escom.defensoria.primercontacto.repository.ExpedientePrimerContactoRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

/**
 * Citas de Primer Contacto (CU-PC-04).
 *
 * Quién agenda, confirma, reagenda o cancela sale siempre del JWT. Cada movimiento se le
 * avisa al quejoso (centro de notificaciones + correo).
 */
@Service
public class CitaPrimerContactoService {

    public static final String PROGRAMADA = "PROGRAMADA";
    public static final String CONFIRMADA = "CONFIRMADA";
    public static final String CANCELADA = "CANCELADA";

    private static final DateTimeFormatter FECHA_AVISO =
            DateTimeFormatter.ofPattern("EEEE d 'de' MMMM 'de' yyyy", Locale.forLanguageTag("es-MX"));

    private final CitaPrimerContactoRepository citaPrimerContactoRepository;
    private final ExpedientePrimerContactoRepository expedienteRepository;
    private final NotificacionQuejosoService notificacionService;

    public CitaPrimerContactoService(
            CitaPrimerContactoRepository citaPrimerContactoRepository,
            ExpedientePrimerContactoRepository expedienteRepository,
            NotificacionQuejosoService notificacionService
    ) {
        this.citaPrimerContactoRepository = citaPrimerContactoRepository;
        this.expedienteRepository = expedienteRepository;
        this.notificacionService = notificacionService;
    }

    public CitaDTO crearCita(
            CrearCitaDTO dto,
            PersonalAdministrativo analista
    ) {

        /*
         * El cliente manda el folio propio de Primer Contacto.
         * Con ese folio obtenemos el expediente interno.
         */
        ExpedientePrimerContacto expediente =
                expedienteRepository.findByFolio(dto.getFolio())
                        .orElseThrow(() ->
                                new RecursoNoEncontradoException(
                                        "No existe un expediente de Primer Contacto con folio "
                                                + dto.getFolio()
                                )
                        );

        if (!EstatusExpediente.estaAbierto(expediente.getEstatus())) {
            throw new OperacionInvalidaException(
                    "Solo se agendan citas para expedientes en análisis."
            );
        }

        boolean yaTieneCita = citaPrimerContactoRepository
                .existsByExpedienteIdAndEstatusNot(
                        expediente.getId(),
                        CANCELADA
                );

        if (yaTieneCita) {
            throw new OperacionInvalidaException(
                    "El expediente ya tiene una cita activa. Reagéndala o cancélala primero."
            );
        }

        LocalDateTime ahora = LocalDateTime.now();

        CitaPrimerContacto cita =
                CitaPrimerContacto.builder()
                        .expedienteId(expediente.getId())
                        .folio(expediente.getFolio())
                        /*
                         * El quejoso sale del expediente, no del payload:
                         * así la cita siempre apunta a la persona real.
                         */
                        .quejosoId(expediente.getQuejosoId())
                        .quejosoNombre(
                                expediente.getQuejosoNombre() != null
                                        ? expediente.getQuejosoNombre()
                                        : dto.getQuejosoNombre()
                        )
                        .analistaId(analista.getId())
                        .analistaNombre(analista.getNombreCompleto())
                        .fechaCita(LocalDate.parse(dto.getFechaCita()))
                        .horaCita(LocalTime.parse(dto.getHoraCita()))
                        .tipoCita(dto.getTipoCita())
                        .motivo(dto.getMotivo())
                        .estatus(PROGRAMADA)
                        .fechaCreacion(ahora)
                        .fechaActualizacion(ahora)
                        .actualizadoPorId(analista.getId())
                        .actualizadoPorNombre(analista.getNombreCompleto())
                        .build();

        CitaPrimerContacto guardada =
                citaPrimerContactoRepository.save(cita);

        avisar(guardada, expediente,
                "Se agendó una cita de primer contacto",
                "Primer Contacto agendó una cita para atender tu queja " + expediente.getFolioOrigen()
                        + ": " + describir(guardada) + "."
                        + "\nMotivo: " + guardada.getMotivo());

        return convertirADTO(guardada);
    }

    public List<CitaDTO> listarPorExpediente(Long expedienteId) {
        return citaPrimerContactoRepository
                .findByExpedienteIdOrderByFechaCitaDescHoraCitaDesc(
                        expedienteId
                )
                .stream()
                .map(this::convertirADTO)
                .toList();
    }

    public List<CitaDTO> listarPorFolio(String folio) {
        return citaPrimerContactoRepository
                .findByFolioOrderByFechaCitaDescHoraCitaDesc(folio)
                .stream()
                .map(this::convertirADTO)
                .toList();
    }

    public List<CitaDTO> obtenerAgendaDia(String fecha) {
        return citaPrimerContactoRepository
                .findByFechaCitaOrderByHoraCitaAsc(LocalDate.parse(fecha))
                .stream()
                .map(this::convertirADTO)
                .toList();
    }

    public List<CitaDTO> obtenerAgendaAnalista(Long analistaId) {
        return citaPrimerContactoRepository
                .findByAnalistaIdOrderByFechaCitaAscHoraCitaAsc(analistaId)
                .stream()
                .map(this::convertirADTO)
                .toList();
    }

    public CitaDTO confirmarCita(Long id, PersonalAdministrativo analista) {

        CitaPrimerContacto cita = obtenerActiva(id);

        if (CONFIRMADA.equals(cita.getEstatus())) {
            throw new OperacionInvalidaException("La cita ya estaba confirmada.");
        }

        cita.setEstatus(CONFIRMADA);
        registrarMovimiento(cita, analista);

        CitaPrimerContacto guardada = citaPrimerContactoRepository.save(cita);

        avisar(guardada, null,
                "Tu cita de primer contacto fue confirmada",
                "Tu cita para la queja " + folioQueja(guardada) + " quedó confirmada: "
                        + describir(guardada) + ".");

        return convertirADTO(guardada);
    }

    /**
     * Reagendar de verdad: se mueve la MISMA cita (antes el front cancelaba y creaba otra,
     * y si fallaba el segundo paso el quejoso se quedaba sin cita). Vuelve a PROGRAMADA
     * porque la nueva fecha todavía no está confirmada.
     */
    public CitaDTO reagendarCita(
            Long id,
            ReagendarCitaDTO dto,
            PersonalAdministrativo analista
    ) {

        CitaPrimerContacto cita = obtenerActiva(id);

        cita.setFechaCita(LocalDate.parse(dto.getFechaCita()));
        cita.setHoraCita(LocalTime.parse(dto.getHoraCita()));

        if (dto.getTipoCita() != null && !dto.getTipoCita().isBlank()) {
            cita.setTipoCita(dto.getTipoCita());
        }

        if (dto.getMotivo() != null && !dto.getMotivo().isBlank()) {
            cita.setMotivo(dto.getMotivo());
        }

        cita.setEstatus(PROGRAMADA);
        registrarMovimiento(cita, analista);

        CitaPrimerContacto guardada = citaPrimerContactoRepository.save(cita);

        avisar(guardada, null,
                "Tu cita de primer contacto cambió de fecha",
                "Tu cita para la queja " + folioQueja(guardada) + " se reagendó para el "
                        + describir(guardada) + ".");

        return convertirADTO(guardada);
    }

    public CitaDTO cancelarCita(Long id, PersonalAdministrativo analista) {

        CitaPrimerContacto cita = obtenerActiva(id);

        cita.setEstatus(CANCELADA);
        registrarMovimiento(cita, analista);

        CitaPrimerContacto guardada = citaPrimerContactoRepository.save(cita);

        avisar(guardada, null,
                "Tu cita de primer contacto fue cancelada",
                "Se canceló tu cita del " + describir(guardada) + " para la queja "
                        + folioQueja(guardada) + ". Si hace falta, Primer Contacto te "
                        + "propondrá una nueva fecha.");

        return convertirADTO(guardada);
    }

    private CitaPrimerContacto obtenerActiva(Long id) {

        CitaPrimerContacto cita = citaPrimerContactoRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Cita no encontrada"));

        if (CANCELADA.equals(cita.getEstatus())) {
            throw new OperacionInvalidaException("La cita ya está cancelada.");
        }

        return cita;
    }

    private void registrarMovimiento(CitaPrimerContacto cita, PersonalAdministrativo analista) {
        cita.setActualizadoPorId(analista.getId());
        cita.setActualizadoPorNombre(analista.getNombreCompleto());
        cita.setFechaActualizacion(LocalDateTime.now());
    }

    private void avisar(
            CitaPrimerContacto cita,
            ExpedientePrimerContacto expediente,
            String titulo,
            String mensaje
    ) {
        ExpedientePrimerContacto destino = expediente != null
                ? expediente
                : expedienteRepository.findById(cita.getExpedienteId()).orElse(null);

        if (destino != null) {
            notificacionService.notificar(
                    destino,
                    NotificacionQuejosoService.TIPO_CITA,
                    titulo,
                    mensaje
            );
        }
    }

    private String folioQueja(CitaPrimerContacto cita) {
        return expedienteRepository.findById(cita.getExpedienteId())
                .map(ExpedientePrimerContacto::getFolioOrigen)
                .orElse(cita.getFolio());
    }

    private String describir(CitaPrimerContacto cita) {
        String modalidad = "VIRTUAL".equalsIgnoreCase(cita.getTipoCita()) ? "virtual" : "presencial";
        return cita.getFechaCita().format(FECHA_AVISO) + " a las "
                + cita.getHoraCita().toString().substring(0, 5) + " h, modalidad " + modalidad;
    }

    private CitaDTO convertirADTO(CitaPrimerContacto cita) {

        return CitaDTO.builder()
                .id(cita.getId())
                .expedienteId(cita.getExpedienteId())
                .folio(cita.getFolio())
                .quejosoId(cita.getQuejosoId())
                .quejosoNombre(cita.getQuejosoNombre())
                .analistaId(cita.getAnalistaId())
                .analistaNombre(cita.getAnalistaNombre())
                .fechaCita(cita.getFechaCita() != null ? cita.getFechaCita().toString() : null)
                .horaCita(cita.getHoraCita() != null ? cita.getHoraCita().toString() : null)
                .tipoCita(cita.getTipoCita())
                .motivo(cita.getMotivo())
                .estatus(cita.getEstatus())
                .fechaCreacion(cita.getFechaCreacion() != null ? cita.getFechaCreacion().toString() : null)
                .actualizadoPorNombre(cita.getActualizadoPorNombre())
                .fechaActualizacion(cita.getFechaActualizacion() != null ? cita.getFechaActualizacion().toString() : null)
                .build();
    }
}
