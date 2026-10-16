package ipn.escom.defensoria.primercontacto.service;

import ipn.escom.defensoria.primercontacto.dto.CitaDTO;
import ipn.escom.defensoria.primercontacto.dto.CitaPublicaDTO;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * Citas de Primer Contacto (CU-PC-04).
 *
 * Quién agenda, confirma, reagenda o cancela sale siempre del JWT. Cada movimiento se le
 * avisa al quejoso (centro de notificaciones + correo).
 *
 * Respuesta del quejoso: al agendar o reagendar, el quejoso tiene un plazo (48 h por
 * defecto) para confirmar o cancelar indicando el motivo, desde su panel o avisándole al
 * analista. Ciclo de la cita:
 *
 *   PROGRAMADA ──quejoso confirma──────────▶ CONFIRMADA
 *              ──quejoso cancela (motivo)──▶ CANCELADA_QUEJOSO ─┐
 *              ──vence el plazo────────────▶ SIN_RESPUESTA ─────┤ el analista reagenda
 *                                                               │ (vuelve a PROGRAMADA con
 *   cualquiera ──el analista cancela───────▶ CANCELADA          │ un plazo nuevo) o cancela
 *
 * Reagendar NO modifica la cita: la deja en REAGENDADA (conservando lo que pasó con ella:
 * respuesta del quejoso, motivo...) y crea una cita NUEVA con su propio plazo, que apunta a
 * la anterior en citaAnteriorId. Así el expediente guarda el historial completo de citas.
 *
 * CANCELADA_QUEJOSO y SIN_RESPUESTA siguen contando como cita activa del expediente: quedan
 * pendientes de que el analista decida qué hacer.
 *
 * El quejoso responde desde el enlace que le llega por correo y en su centro de
 * notificaciones (/cita/responder?token=..., sin iniciar sesión) o desde su panel.
 *
 * Horario: cada cita ocupa duracionMinutos (60 por defecto) y un analista no puede tener dos
 * citas PROGRAMADAS o CONFIRMADAS encimadas. Dos analistas sí pueden citar a la misma hora.
 */
@Service
public class CitaPrimerContactoService {

    private static final Logger log = LoggerFactory.getLogger(CitaPrimerContactoService.class);

    public static final String PROGRAMADA = "PROGRAMADA";
    public static final String CONFIRMADA = "CONFIRMADA";
    public static final String CANCELADA = "CANCELADA";
    public static final String CANCELADA_QUEJOSO = "CANCELADA_QUEJOSO";
    public static final String SIN_RESPUESTA = "SIN_RESPUESTA";
    /* La cita se movió a otra fecha: queda en el historial y la sustituye una cita nueva. */
    public static final String REAGENDADA = "REAGENDADA";

    /* Citas que ya no cuentan como cita activa del expediente. */
    public static final List<String> CERRADAS = List.of(CANCELADA, REAGENDADA);

    /* Citas que apartan el horario del analista (las canceladas o sin respuesta lo liberan). */
    public static final List<String> OCUPAN_HORARIO = List.of(PROGRAMADA, CONFIRMADA);

    /* Página pública del portal del quejoso donde confirma o cancela. */
    private static final String ENLACE_RESPUESTA = "/cita/responder?token=";

    public static final String RESPUESTA_QUEJOSO = "QUEJOSO";
    public static final String RESPUESTA_ANALISTA = "ANALISTA";

    private static final DateTimeFormatter FECHA_AVISO =
            DateTimeFormatter.ofPattern("EEEE d 'de' MMMM 'de' yyyy", Locale.forLanguageTag("es-MX"));
    private static final DateTimeFormatter FECHA_HORA_LIMITE =
            DateTimeFormatter.ofPattern("d 'de' MMMM 'a las' HH:mm 'h'", Locale.forLanguageTag("es-MX"));

    private final CitaPrimerContactoRepository citaPrimerContactoRepository;
    private final ExpedientePrimerContactoRepository expedienteRepository;
    private final NotificacionQuejosoService notificacionService;
    private final long horasRespuesta;
    private final long duracionMinutos;
    private final String portalUrl;

    public CitaPrimerContactoService(
            CitaPrimerContactoRepository citaPrimerContactoRepository,
            ExpedientePrimerContactoRepository expedienteRepository,
            NotificacionQuejosoService notificacionService,
            @Value("${primer-contacto.citas.horas-respuesta:48}") long horasRespuesta,
            @Value("${primer-contacto.citas.duracion-minutos:60}") long duracionMinutos,
            @Value("${primer-contacto.portal.url:https://defensoria-escom.ddns.net}") String portalUrl
    ) {
        this.citaPrimerContactoRepository = citaPrimerContactoRepository;
        this.expedienteRepository = expedienteRepository;
        this.notificacionService = notificacionService;
        this.horasRespuesta = horasRespuesta;
        this.duracionMinutos = duracionMinutos;
        this.portalUrl = portalUrl == null ? "" : portalUrl.strip().replaceAll("/+$", "");
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
                .existsByExpedienteIdAndEstatusNotIn(
                        expediente.getId(),
                        CERRADAS
                );

        if (yaTieneCita) {
            throw new OperacionInvalidaException(
                    "El expediente ya tiene una cita activa. Reagéndala o cancélala primero."
            );
        }

        LocalDate fechaCita = LocalDate.parse(dto.getFechaCita());
        LocalTime horaCita = LocalTime.parse(dto.getHoraCita());
        validarSinTraslape(analista.getId(), fechaCita, horaCita, null);

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
                        .fechaCita(fechaCita)
                        .horaCita(horaCita)
                        .tipoCita(dto.getTipoCita())
                        .motivo(dto.getMotivo())
                        .estatus(PROGRAMADA)
                        .fechaCreacion(ahora)
                        .fechaActualizacion(ahora)
                        .actualizadoPorId(analista.getId())
                        .actualizadoPorNombre(analista.getNombreCompleto())
                        .fechaLimiteRespuesta(ahora.plusHours(horasRespuesta))
                        .tokenRespuesta(nuevoToken())
                        .build();

        CitaPrimerContacto guardada =
                citaPrimerContactoRepository.save(cita);

        avisar(guardada, expediente,
                "Se agendó una cita de primer contacto",
                "Primer Contacto agendó una cita para atender tu queja " + expediente.getFolioOrigen()
                        + ": " + describir(guardada) + "."
                        + "\nMotivo: " + guardada.getMotivo()
                        + instruccionesRespuesta(guardada),
                enlaceRespuesta(guardada));

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

    /** Citas vigentes del analista de hoy en adelante, de cualquier día (agenda general). */
    public List<CitaDTO> proximasDelAnalista(PersonalAdministrativo analista) {
        return citaPrimerContactoRepository
                .findByAnalistaIdAndFechaCitaGreaterThanEqualAndEstatusNotInOrderByFechaCitaAscHoraCitaAsc(
                        analista.getId(), LocalDate.now(), CERRADAS)
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

    /**
     * El analista registra que el quejoso confirmó (por ejemplo, por teléfono). Se acepta
     * aunque el plazo ya haya vencido: es el analista quien lo hace constar.
     */
    public CitaDTO confirmarCita(Long id, PersonalAdministrativo analista) {

        CitaPrimerContacto cita = obtenerActiva(id);

        if (CONFIRMADA.equals(cita.getEstatus())) {
            throw new OperacionInvalidaException("La cita ya estaba confirmada.");
        }

        cita.setEstatus(CONFIRMADA);
        registrarRespuesta(cita, RESPUESTA_ANALISTA, null);
        registrarMovimiento(cita, analista);

        CitaPrimerContacto guardada = citaPrimerContactoRepository.save(cita);

        avisar(guardada, null,
                "Tu cita de primer contacto fue confirmada",
                "Tu cita para la queja " + folioQueja(guardada) + " quedó confirmada: "
                        + describir(guardada) + ".");

        return convertirADTO(guardada);
    }

    /**
     * Reagendar: la cita actual queda en REAGENDADA (con su historia) y se crea una cita
     * nueva PROGRAMADA con plazo de respuesta nuevo, en una sola transacción lógica: si el
     * guardado de la nueva fallara, la anterior ya no estaría activa, así que el analista
     * vería el error y volvería a agendar.
     */
    public CitaDTO reagendarCita(
            Long id,
            ReagendarCitaDTO dto,
            PersonalAdministrativo analista
    ) {

        CitaPrimerContacto anterior = obtenerActiva(id);

        LocalDate fechaCita = LocalDate.parse(dto.getFechaCita());
        LocalTime horaCita = LocalTime.parse(dto.getHoraCita());
        // La cita que se mueve no estorba: su horario se libera al reagendar.
        validarSinTraslape(analista.getId(), fechaCita, horaCita, anterior.getId());

        LocalDateTime ahora = LocalDateTime.now();

        // La cita anterior queda en el historial tal como estaba (respuesta, motivo...).
        anterior.setEstatus(REAGENDADA);
        registrarMovimiento(anterior, analista);
        citaPrimerContactoRepository.save(anterior);

        CitaPrimerContacto nueva = CitaPrimerContacto.builder()
                .expedienteId(anterior.getExpedienteId())
                .folio(anterior.getFolio())
                .quejosoId(anterior.getQuejosoId())
                .quejosoNombre(anterior.getQuejosoNombre())
                .analistaId(analista.getId())
                .analistaNombre(analista.getNombreCompleto())
                .fechaCita(fechaCita)
                .horaCita(horaCita)
                .tipoCita(dto.getTipoCita() != null && !dto.getTipoCita().isBlank()
                        ? dto.getTipoCita() : anterior.getTipoCita())
                .motivo(dto.getMotivo() != null && !dto.getMotivo().isBlank()
                        ? dto.getMotivo() : anterior.getMotivo())
                .estatus(PROGRAMADA)
                .fechaCreacion(ahora)
                .fechaActualizacion(ahora)
                .actualizadoPorId(analista.getId())
                .actualizadoPorNombre(analista.getNombreCompleto())
                .fechaLimiteRespuesta(ahora.plusHours(horasRespuesta))
                .citaAnteriorId(anterior.getId())
                .tokenRespuesta(nuevoToken())
                .build();

        CitaPrimerContacto guardada = citaPrimerContactoRepository.save(nueva);

        avisar(guardada, null,
                "Tu cita de primer contacto cambió de fecha",
                "Tu cita para la queja " + folioQueja(guardada) + " se reagendó para el "
                        + describir(guardada) + "."
                        + instruccionesRespuesta(guardada),
                enlaceRespuesta(guardada));

        return convertirADTO(guardada);
    }

    /**
     * El analista registra que el quejoso canceló (por ejemplo, por teléfono), con su motivo.
     * Queda en CANCELADA_QUEJOSO para que se reagende o se cancele en definitiva.
     */
    public CitaDTO registrarCancelacionQuejoso(
            Long id,
            String motivo,
            PersonalAdministrativo analista
    ) {

        CitaPrimerContacto cita = obtenerActiva(id);

        if (!PROGRAMADA.equals(cita.getEstatus()) && !SIN_RESPUESTA.equals(cita.getEstatus())) {
            throw new OperacionInvalidaException(
                    "Solo se registra la cancelación del quejoso en citas pendientes de respuesta."
            );
        }

        cita.setEstatus(CANCELADA_QUEJOSO);
        registrarRespuesta(cita, RESPUESTA_ANALISTA, motivoObligatorio(motivo));
        registrarMovimiento(cita, analista);

        return convertirADTO(citaPrimerContactoRepository.save(cita));
    }

    // ---------------------------------------------------------------------------------
    // Respuesta del quejoso desde su panel (/api/primer-contacto/quejoso/citas).
    // El quejoso se identifica por el correo de su JWT, que debe ser el del expediente.
    // ---------------------------------------------------------------------------------

    public List<CitaDTO> citasDelQuejoso(String correo) {

        List<Long> expedientes = expedienteRepository.findByQuejosoCorreoIgnoreCase(correo)
                .stream()
                .map(ExpedientePrimerContacto::getId)
                .toList();

        if (expedientes.isEmpty()) {
            return List.of();
        }

        return citaPrimerContactoRepository
                .findByExpedienteIdInOrderByFechaCitaDescHoraCitaDesc(expedientes)
                .stream()
                .map(this::convertirADTO)
                .toList();
    }

    public CitaDTO confirmarPorQuejoso(Long id, String correo) {

        CitaPrimerContacto cita = obtenerParaRespuestaDelQuejoso(id, correo);

        cita.setEstatus(CONFIRMADA);
        registrarRespuesta(cita, RESPUESTA_QUEJOSO, null);
        cita.setFechaActualizacion(LocalDateTime.now());

        return convertirADTO(citaPrimerContactoRepository.save(cita));
    }

    public CitaDTO cancelarPorQuejoso(Long id, String motivo, String correo) {

        CitaPrimerContacto cita = obtenerParaRespuestaDelQuejoso(id, correo);

        cita.setEstatus(CANCELADA_QUEJOSO);
        registrarRespuesta(cita, RESPUESTA_QUEJOSO, motivoObligatorio(motivo));
        cita.setFechaActualizacion(LocalDateTime.now());

        return convertirADTO(citaPrimerContactoRepository.save(cita));
    }

    // ---------------------------------------------------------------------------------
    // Respuesta del quejoso desde el enlace del correo / notificación (sin sesión).
    // ---------------------------------------------------------------------------------

    public CitaPublicaDTO citaPorToken(String token) {
        return convertirAPublica(obtenerPorToken(token));
    }

    public CitaPublicaDTO confirmarPorToken(String token) {

        CitaPrimerContacto cita = obtenerPorToken(token);
        validarPendienteDeRespuesta(cita);

        cita.setEstatus(CONFIRMADA);
        registrarRespuesta(cita, RESPUESTA_QUEJOSO, null);
        cita.setFechaActualizacion(LocalDateTime.now());

        return convertirAPublica(citaPrimerContactoRepository.save(cita));
    }

    public CitaPublicaDTO cancelarPorToken(String token, String motivo) {

        CitaPrimerContacto cita = obtenerPorToken(token);
        validarPendienteDeRespuesta(cita);

        cita.setEstatus(CANCELADA_QUEJOSO);
        registrarRespuesta(cita, RESPUESTA_QUEJOSO, motivoObligatorio(motivo));
        cita.setFechaActualizacion(LocalDateTime.now());

        return convertirAPublica(citaPrimerContactoRepository.save(cita));
    }

    private CitaPrimerContacto obtenerPorToken(String token) {
        if (token == null || token.isBlank()) {
            throw new RecursoNoEncontradoException("El enlace de la cita no es válido.");
        }
        return citaPrimerContactoRepository.findByTokenRespuesta(token.strip())
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "El enlace de la cita no es válido o ya no existe."));
    }

    /**
     * Citas PROGRAMADAS cuyo plazo de respuesta ya venció pasan a SIN_RESPUESTA, para que el
     * analista las vea y decida si reagenda o cancela.
     */
    @Scheduled(
            initialDelayString = "${primer-contacto.citas.revision-ms:300000}",
            fixedDelayString = "${primer-contacto.citas.revision-ms:300000}"
    )
    public void marcarCitasSinRespuesta() {

        List<CitaPrimerContacto> vencidas = citaPrimerContactoRepository
                .findByEstatusAndFechaLimiteRespuestaBefore(PROGRAMADA, LocalDateTime.now());

        for (CitaPrimerContacto cita : vencidas) {
            cita.setEstatus(SIN_RESPUESTA);
            cita.setFechaActualizacion(LocalDateTime.now());
            citaPrimerContactoRepository.save(cita);
            log.info("Cita {} del expediente {} sin respuesta del quejoso al vencer el plazo.",
                    cita.getId(), cita.getFolio());
        }
    }

    private CitaPrimerContacto obtenerParaRespuestaDelQuejoso(Long id, String correo) {

        CitaPrimerContacto cita = citaPrimerContactoRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Cita no encontrada"));

        ExpedientePrimerContacto expediente = expedienteRepository.findById(cita.getExpedienteId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Cita no encontrada"));

        // Una cita ajena se reporta igual que una inexistente: no se revela que existe.
        if (correo == null || !correo.equalsIgnoreCase(expediente.getQuejosoCorreo())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Cita no encontrada");
        }

        validarPendienteDeRespuesta(cita);

        return cita;
    }

    private void validarPendienteDeRespuesta(CitaPrimerContacto cita) {

        if (!PROGRAMADA.equals(cita.getEstatus())) {
            throw new OperacionInvalidaException("Esta cita ya no está esperando tu respuesta.");
        }

        if (plazoVencido(cita)) {
            throw new OperacionInvalidaException(
                    "El plazo para responder esta cita ya venció. Comunícate con Primer Contacto."
            );
        }
    }

    private boolean plazoVencido(CitaPrimerContacto cita) {
        return cita.getFechaLimiteRespuesta() != null
                && LocalDateTime.now().isAfter(cita.getFechaLimiteRespuesta());
    }

    /**
     * Un analista no puede tener dos citas encimadas: cada una ocupa duracionMinutos. Solo
     * cuentan las PROGRAMADAS y CONFIRMADAS; excluirId es la cita que se está reagendando.
     */
    private void validarSinTraslape(Long analistaId, LocalDate fecha, LocalTime hora, Long excluirId) {

        for (CitaPrimerContacto otra : citaPrimerContactoRepository
                .findByAnalistaIdAndFechaCitaAndEstatusIn(analistaId, fecha, OCUPAN_HORARIO)) {

            if (otra.getId().equals(excluirId)) {
                continue;
            }

            long separacion = Math.abs(Duration.between(otra.getHoraCita(), hora).toMinutes());
            if (separacion < duracionMinutos) {
                throw new OperacionInvalidaException(
                        "Ya tienes una cita ese día a las " + otra.getHoraCita().toString().substring(0, 5)
                                + " h (expediente " + otra.getFolio() + "). Cada cita dura "
                                + duracionMinutos + " minutos: elige otro horario."
                );
            }
        }
    }

    private static String nuevoToken() {
        return UUID.randomUUID().toString().replace("-", "")
                + UUID.randomUUID().toString().replace("-", "");
    }

    /* Ruta dentro del portal; el aviso del panel navega a ella y el correo le antepone portalUrl. */
    private String enlaceRespuesta(CitaPrimerContacto cita) {
        return cita.getTokenRespuesta() == null ? null : ENLACE_RESPUESTA + cita.getTokenRespuesta();
    }

    private void registrarRespuesta(CitaPrimerContacto cita, String quien, String motivoCancelacion) {
        cita.setRespuestaRegistradaPor(quien);
        cita.setFechaRespuestaQuejoso(LocalDateTime.now());
        cita.setMotivoCancelacionQuejoso(motivoCancelacion);
    }

    private String motivoObligatorio(String motivo) {
        if (motivo == null || motivo.isBlank()) {
            throw new OperacionInvalidaException("Indica el motivo de la cancelación.");
        }
        return motivo.strip();
    }

    private String instruccionesRespuesta(CitaPrimerContacto cita) {
        if (cita.getFechaLimiteRespuesta() == null) {
            return "";
        }
        String plazo = "\n\nTienes " + horasRespuesta + " horas (hasta el "
                + cita.getFechaLimiteRespuesta().format(FECHA_HORA_LIMITE)
                + ") para confirmar tu asistencia o cancelarla indicando el motivo";
        String enlace = enlaceRespuesta(cita);
        if (enlace != null) {
            plazo += " en este enlace:\n" + portalUrl + enlace + "\n\nTambién puedes hacerlo";
        } else {
            plazo += ",";
        }
        return plazo + " desde tu panel o comunicándote con Primer Contacto. Si la cancelas, "
                + "podremos proponerte una nueva fecha.";
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

        if (REAGENDADA.equals(cita.getEstatus())) {
            throw new OperacionInvalidaException("Esta cita ya se reagendó; usa la cita nueva.");
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
        avisar(cita, expediente, titulo, mensaje, null);
    }

    private void avisar(
            CitaPrimerContacto cita,
            ExpedientePrimerContacto expediente,
            String titulo,
            String mensaje,
            String enlace
    ) {
        ExpedientePrimerContacto destino = expediente != null
                ? expediente
                : expedienteRepository.findById(cita.getExpedienteId()).orElse(null);

        if (destino != null) {
            notificacionService.notificar(
                    destino,
                    NotificacionQuejosoService.TIPO_CITA,
                    titulo,
                    mensaje,
                    enlace
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
                .folioQueja(folioQueja(cita))
                .fechaLimiteRespuesta(cita.getFechaLimiteRespuesta() != null ? cita.getFechaLimiteRespuesta().toString() : null)
                .fechaRespuestaQuejoso(cita.getFechaRespuestaQuejoso() != null ? cita.getFechaRespuestaQuejoso().toString() : null)
                .motivoCancelacionQuejoso(cita.getMotivoCancelacionQuejoso())
                .respuestaRegistradaPor(cita.getRespuestaRegistradaPor())
                .citaAnteriorId(cita.getCitaAnteriorId())
                .build();
    }

    private CitaPublicaDTO convertirAPublica(CitaPrimerContacto cita) {

        return CitaPublicaDTO.builder()
                .folioQueja(folioQueja(cita))
                .quejosoNombre(cita.getQuejosoNombre())
                .fechaCita(cita.getFechaCita() != null ? cita.getFechaCita().toString() : null)
                .horaCita(cita.getHoraCita() != null ? cita.getHoraCita().toString() : null)
                .tipoCita(cita.getTipoCita())
                .motivo(cita.getMotivo())
                .estatus(cita.getEstatus())
                .fechaLimiteRespuesta(cita.getFechaLimiteRespuesta() != null ? cita.getFechaLimiteRespuesta().toString() : null)
                .motivoCancelacionQuejoso(cita.getMotivoCancelacionQuejoso())
                .analistaNombre(cita.getAnalistaNombre())
                .puedeResponder(PROGRAMADA.equals(cita.getEstatus()) && !plazoVencido(cita))
                .build();
    }
}
