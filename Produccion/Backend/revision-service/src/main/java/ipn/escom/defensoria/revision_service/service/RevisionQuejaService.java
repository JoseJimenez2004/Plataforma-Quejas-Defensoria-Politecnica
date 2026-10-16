package ipn.escom.defensoria.revision_service.service;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import ipn.escom.defensoria.revision_service.dto.EvidenciaResumen;
import ipn.escom.defensoria.revision_service.entity.PersonalAdministrativo;
import ipn.escom.defensoria.revision_service.entity.Queja;
import ipn.escom.defensoria.revision_service.entity.QuejaEvidencia;
import ipn.escom.defensoria.revision_service.model.AntecedenteModel;
import ipn.escom.defensoria.revision_service.model.BandejaResumenModel;
import ipn.escom.defensoria.revision_service.model.CargaAbogadoModel;
import ipn.escom.defensoria.revision_service.model.CargaRecepcionistaModel;
import ipn.escom.defensoria.revision_service.model.DatosVentanillaRequest;
import ipn.escom.defensoria.revision_service.model.HistorialFilaModel;
import ipn.escom.defensoria.revision_service.model.HistorialPaginaModel;
import ipn.escom.defensoria.revision_service.model.HistorialResumenModel;
import ipn.escom.defensoria.revision_service.model.IndicadoresBandejaModel;
import ipn.escom.defensoria.revision_service.model.HistorialItemModel;
import ipn.escom.defensoria.revision_service.model.QuejaDetalleModel;
import ipn.escom.defensoria.revision_service.model.QuejaResumenBandejaModel;
import ipn.escom.defensoria.revision_service.repository.PersonalAdministrativoRepository;
import ipn.escom.defensoria.revision_service.repository.QuejaEvidenciaRepository;
import ipn.escom.defensoria.revision_service.repository.QuejaRepository;
import ipn.escom.defensoria.revision_service.model.PrimerContactoIngresoResponse;

@Service
public class RevisionQuejaService {

    // Estatus del flujo de revisión -- ver comentario en la entidad Queja.
    public static final String RECIBIDA = "RECIBIDA";
    public static final String EN_VALIDACION = "EN_VALIDACION";
    public static final String RECHAZADA = "RECHAZADA";
    public static final String TURNADA = "TURNADA";
    /**
     * El quejoso ya atendió las observaciones de un rechazo y reenvió su queja. Vuelve a la
     * bandeja para una segunda validación. Sin este estado, una queja rechazada quedaba
     * muerta: no se podía editar ni turnar.
     */
    public static final String CORREGIDA = "CORREGIDA";

    /**
     * Desde 2026-10-13 Primer Contacto y Subdefensoría escriben su avance en quejas.estatus
     * (diagrama de estados de la queja), así que una queja turnada ya no se queda en TURNADA.
     * Para Recepción todas estas siguen siendo "turnadas": salieron de su bandeja hacia
     * Primer Contacto.
     */
    public static final List<String> TURNADA_O_POSTERIOR = List.of(
            TURNADA,
            "EN_ANALISIS", "PROCEDENTE", "IMPROCEDENTE", "REMITIDA",
            "RECIBIDA_SUBDEFENSORIA", "EN_INVESTIGACION", "EN_ESPERA_OFICIO", "ELABORO_ACUERDO",
            "PENDIENTE_CONCILIACION", "PENDIENTE_CONCLUSION", "CONCLUIDA");

    private static final String ORIGEN_MANUAL = "MANUAL";
    private static final String PREFIJO_FOLIO = "FOL-";
    private static final int LONGITUD_UUID_FOLIO = 8;

    /** Si una queja lleva más de esto EN_VALIDACION sin que su recepcionista la rechace o
     * turne, se libera sola de vuelta a la bandeja -- ver liberarRevisionesVencidas(). */
    private static final long MINUTOS_LIMITE_REVISION = 20;

    private final QuejaRepository quejaRepository;
    private final QuejaEvidenciaRepository evidenciaRepository;
    private final NotificacionQuejaService notificacionService;
    private final PrimerContactoClientService primerContactoClientService;
    private final PersonalAdministrativoRepository personalRepository;
    private final IndicadoresBandejaService indicadoresService;

    /** Filas por página de la Bandeja (10 por defecto; el cliente puede pedir hasta 50). */
    public static final int TAMANIO_PAGINA_DEFAULT = 10;
    private static final int TAMANIO_PAGINA_MAXIMO = 50;

    public RevisionQuejaService(
            QuejaRepository quejaRepository,
            QuejaEvidenciaRepository evidenciaRepository,
            NotificacionQuejaService notificacionService,
            PrimerContactoClientService primerContactoClientService,
            PersonalAdministrativoRepository personalRepository,
            IndicadoresBandejaService indicadoresService) {
        this.quejaRepository = quejaRepository;
        this.evidenciaRepository = evidenciaRepository;
        this.notificacionService = notificacionService;
        this.primerContactoClientService = primerContactoClientService;
        this.personalRepository = personalRepository;
        this.indicadoresService = indicadoresService;
    }

    // ---------------- Bandeja de Entrada ----------------

    /**
     * Bandeja de Entrada paginada (2026-10-15). Funciona como una cola: la queja que llegó más
     * recientemente va arriba. Filtros:
     *   todas        -> por atender + en atención (de cualquiera)
     *   disponibles  -> solo las que nadie ha tomado (nuevas y corregidas)
     *   mias         -> las que tiene abiertas quien consulta
     *   corregidas   -> reenviadas por el quejoso tras un rechazo
     *   en_atencion  -> abiertas ahora mismo por cualquier recepcionista
     * texto busca en folio, nombre del quejoso y clave de la unidad.
     */
    public BandejaResumenModel bandeja(String correo, String filtro, String texto, int pagina, int tamanio) {
        // Antes de contar/listar, se liberan las que quedaron abandonadas EN_VALIDACION --
        // así la bandeja nunca muestra "en atención" a alguien que ya no está ahí.
        liberarRevisionesVencidas();

        List<String> estatus;
        boolean soloMias = false;
        switch (filtro == null ? "todas" : filtro.toLowerCase()) {
            case "disponibles" -> estatus = List.of(RECIBIDA, CORREGIDA);
            case "mias" -> { estatus = List.of(EN_VALIDACION); soloMias = true; }
            case "corregidas" -> estatus = List.of(CORREGIDA);
            case "en_atencion" -> estatus = List.of(EN_VALIDACION);
            default -> estatus = List.of(RECIBIDA, CORREGIDA, EN_VALIDACION);
        }

        String busqueda = texto == null ? "" : texto.trim().toLowerCase();
        String patron = "%" + busqueda.replace("\\", "").replace("%", "").replace("_", "") + "%";
        int tam = Math.max(1, Math.min(tamanio <= 0 ? TAMANIO_PAGINA_DEFAULT : tamanio, TAMANIO_PAGINA_MAXIMO));
        PageRequest paginaSolicitada = PageRequest.of(Math.max(0, pagina), tam,
                Sort.by(Sort.Order.desc("fechaCreacion"), Sort.Order.desc("id")));

        Page<Queja> resultado = quejaRepository.buscarBandeja(
                estatus, soloMias, correo == null ? "" : correo, busqueda, patron, paginaSolicitada);

        // Nombres del personal (tabla chica) para "la rechazó antes ..." sin una consulta por fila.
        Map<String, String> nombresPersonal = new HashMap<>();
        personalRepository.findAll().forEach(p -> {
            if (p.getCorreoInstitucional() != null) {
                nombresPersonal.put(p.getCorreoInstitucional().toLowerCase(), p.getNombreCompleto());
            }
        });

        List<QuejaResumenBandejaModel> lista = resultado.getContent().stream()
                .map(q -> new QuejaResumenBandejaModel(
                        q.getNumeroFolio(),
                        q.getFechaCreacion(),
                        nombreMostrar(q),
                        q.getUnidadAcademicaClave(),
                        documentacionAparenteCompleta(q),
                        q.getEstatus(),
                        q.getRevisandoPorNombre(),
                        EN_VALIDACION.equals(q.getEstatus()) && q.getRevisandoPor() != null
                                && q.getRevisandoPor().equalsIgnoreCase(correo),
                        ORIGEN_MANUAL.equals(q.getOrigenRegistro()) ? "MANUAL" : "WEB",
                        rechazadaAntesPor(q, nombresPersonal)))
                .toList();

        IndicadoresBandejaModel indicadores = indicadoresService.indicadores(correo);
        List<CargaRecepcionistaModel> equipo = indicadoresService.equipo(correo);

        return new BandejaResumenModel(indicadores, lista, resultado.getNumber(), resultado.getSize(),
                resultado.getTotalElements(), resultado.getTotalPages(), equipo);
    }

    /** En una queja CORREGIDA (o en revisión tras corregirse), quién la había rechazado. */
    private String rechazadaAntesPor(Queja q, Map<String, String> nombresPersonal) {
        boolean esReenvio = CORREGIDA.equals(q.getEstatus())
                || (EN_VALIDACION.equals(q.getEstatus()) && CORREGIDA.equals(q.getEstatusPrevioRevision()));
        if (!esReenvio || esVacio(q.getValidadoPor())) {
            return null;
        }
        return nombresPersonal.getOrDefault(q.getValidadoPor().toLowerCase(), q.getValidadoPor());
    }

    // ---------------- Validación de Requisitos ----------------

    /**
     * Al abrir el detalle, una queja RECIBIDA o CORREGIDA pasa a EN_VALIDACION -- así la
     * bandeja refleja que alguien ya la está trabajando (contador "En Proceso") y les muestra
     * a los demás recepcionistas quién la tiene abierta, para que no la dupliquen.
     *
     * Si YA está EN_VALIDACION porque otro recepcionista la abrió antes (y no ha pasado el
     * límite de inactividad), se rechaza con un mensaje claro -- es el respaldo del backend a
     * lo que la bandeja ya oculta/deshabilita en la interfaz. Si el que vuelve a abrirla es la
     * misma persona que ya la tenía (por ejemplo, recargó la página), simplemente continúa.
     */
    public QuejaDetalleModel detalle(String folio, String correoRecepcionista) {
        liberarRevisionesVencidas();
        Queja queja = obtenerPorFolio(folio);

        if (RECIBIDA.equals(queja.getEstatus()) || CORREGIDA.equals(queja.getEstatus())) {
            // Toma atómica: si otra recepcionista la tomó un instante antes, el UPDATE no
            // encuentra la queja libre (0 filas) y se cae al aviso de "ya la tiene X".
            quejaRepository.tomarParaRevision(folio, correoRecepcionista,
                    resolverNombre(correoRecepcionista), LocalDateTime.now());
            queja = obtenerPorFolio(folio);
        }

        if (EN_VALIDACION.equals(queja.getEstatus())) {
            if (esDeOtraPersona(queja, correoRecepcionista)) {
                throw new RuntimeException(mensajeOcupada(queja));
            }
            // Es suya (abrió de nuevo o recargó): se reinicia su tiempo de inactividad.
            quejaRepository.renovarRevision(folio, correoRecepcionista, LocalDateTime.now());
        }
        return aDetalle(queja);
    }

    /** La recepcionista suelta la queja sin rechazarla ni turnarla: regresa a la cola tal como
     * estaba (nueva o corregida) para que la tome alguien más. */
    public void liberar(String folio, String correoRecepcionista) {
        Queja queja = obtenerPorFolio(folio);
        if (!EN_VALIDACION.equals(queja.getEstatus())) {
            return; // ya estaba libre o ya se cerró: no hay nada que soltar
        }
        if (esDeOtraPersona(queja, correoRecepcionista)) {
            throw new RuntimeException(mensajeOcupada(queja));
        }
        queja.setEstatus(queja.getEstatusPrevioRevision() != null ? queja.getEstatusPrevioRevision() : RECIBIDA);
        limpiarRevision(queja);
        quejaRepository.save(queja);
    }

    /** Lo llama el front cada pocos minutos mientras la queja sigue abierta en pantalla, para
     * que no se libere sola si la recepcionista tarda en revisar una queja larga. */
    public void renovar(String folio, String correoRecepcionista) {
        Queja queja = obtenerPorFolio(folio);
        if (!EN_VALIDACION.equals(queja.getEstatus()) || esDeOtraPersona(queja, correoRecepcionista)) {
            throw new RuntimeException("Esta queja ya no está a tu nombre. Regresa a la bandeja.");
        }
        quejaRepository.renovarRevision(folio, correoRecepcionista, LocalDateTime.now());
    }

    /**
     * Solo quien tomó la queja puede rechazarla o turnarla. Sin esto, dos recepcionistas
     * podían cerrar la misma queja (una la turnaba y otra la rechazaba).
     */
    private void exigirQueSeaMia(Queja queja, String correoRecepcionista) {
        if (!EN_VALIDACION.equals(queja.getEstatus()) || queja.getRevisandoPor() == null) {
            throw new RuntimeException("Abre la queja " + queja.getNumeroFolio()
                    + " desde la Bandeja para tomarla antes de rechazarla o turnarla.");
        }
        if (esDeOtraPersona(queja, correoRecepcionista)) {
            throw new RuntimeException(mensajeOcupada(queja));
        }
    }

    private boolean esDeOtraPersona(Queja queja, String correo) {
        return queja.getRevisandoPor() != null && !queja.getRevisandoPor().equalsIgnoreCase(correo);
    }

    private String mensajeOcupada(Queja queja) {
        String quien = queja.getRevisandoPorNombre() != null ? queja.getRevisandoPorNombre() : queja.getRevisandoPor();
        return "La queja " + queja.getNumeroFolio() + " la está atendiendo " + quien + ". "
                + "Elige otra de la bandeja; esta se libera sola si " + quien
                + " la deja sin actividad más de " + MINUTOS_LIMITE_REVISION + " minutos.";
    }

    /** Quejas EN_VALIDACION cuyo recepcionista las dejó abiertas más del límite de inactividad
     * sin rechazarlas ni turnarlas -- se regresan solas a su estatus anterior para que otro
     * recepcionista las pueda tomar. Se corre al inicio de bandeja() y detalle(), no como
     * tarea programada aparte. */
    private void liberarRevisionesVencidas() {
        LocalDateTime limite = LocalDateTime.now().minusMinutes(MINUTOS_LIMITE_REVISION);
        List<Queja> vencidas = quejaRepository.findByEstatusAndFechaInicioRevisionBefore(EN_VALIDACION, limite);
        if (vencidas.isEmpty()) {
            return;
        }
        for (Queja q : vencidas) {
            q.setEstatus(q.getEstatusPrevioRevision() != null ? q.getEstatusPrevioRevision() : RECIBIDA);
            q.setRevisandoPor(null);
            q.setRevisandoPorNombre(null);
            q.setFechaInicioRevision(null);
            q.setEstatusPrevioRevision(null);
        }
        quejaRepository.saveAll(vencidas);
    }

    private String resolverNombre(String correo) {
        return personalRepository.findByCorreoInstitucional(correo)
                .map(PersonalAdministrativo::getNombreCompleto)
                .orElse(correo);
    }

    /** Limpia las marcas de "en revisión" al cerrar el caso (rechazo o turnado) -- ya no hace
     * falta bloquearla para nadie más. */
    private void limpiarRevision(Queja queja) {
        queja.setRevisandoPor(null);
        queja.setRevisandoPorNombre(null);
        queja.setFechaInicioRevision(null);
        queja.setEstatusPrevioRevision(null);
    }

    /** Estatus en los que una queja ya no está en trámite. */
    private static final List<String> ESTATUS_CERRADOS = List.of("CONCLUIDA", "IMPROCEDENTE", "REMITIDA", RECHAZADA);

    /**
     * Antecedentes de la queja (2026-10-16): otras quejas de la MISMA persona quejosa (por
     * boleta/número o por correo) y otras contra la MISMA persona señalada (nombre y primer
     * apellido). Cada una con el abogado que la lleva, para que el turnado pueda dar
     * continuidad.
     */
    public List<AntecedenteModel> antecedentes(String folio) {
        Queja queja = obtenerPorFolio(folio);
        Map<Long, AntecedenteModel> porId = new java.util.LinkedHashMap<>();

        List<Queja> mismaPersona = new java.util.ArrayList<>();
        if (!esVacio(queja.getNumeroIdentificacionQuejoso())) {
            mismaPersona.addAll(quejaRepository.findByNumeroIdentificacionQuejosoAndIdNotOrderByFechaCreacionDesc(
                    queja.getNumeroIdentificacionQuejoso(), queja.getId()));
        }
        if (!esVacio(queja.getCorreoInstitucional())) {
            mismaPersona.addAll(quejaRepository.findByCorreoInstitucionalAndIdNotOrderByFechaCreacionDesc(
                    queja.getCorreoInstitucional(), queja.getId()));
        }
        mismaPersona.forEach(q -> porId.putIfAbsent(q.getId(), aAntecedente(q, "MISMA_PERSONA")));

        if (!esVacio(queja.getNombreDenunciado()) && !esVacio(queja.getApellido1Denunciado())) {
            quejaRepository.findByNombreDenunciadoIgnoreCaseAndApellido1DenunciadoIgnoreCaseAndIdNotOrderByFechaCreacionDesc(
                            queja.getNombreDenunciado().trim(), queja.getApellido1Denunciado().trim(), queja.getId())
                    .forEach(q -> porId.putIfAbsent(q.getId(), aAntecedente(q, "MISMO_DENUNCIADO")));
        }

        return porId.values().stream()
                .sorted(java.util.Comparator.comparing(AntecedenteModel::fecha,
                        java.util.Comparator.nullsLast(java.util.Comparator.reverseOrder())))
                .toList();
    }

    private AntecedenteModel aAntecedente(Queja q, String relacion) {
        String estado = ESTATUS_ACTUAL.getOrDefault(q.getEstatus(),
                RECIBIDA.equals(q.getEstatus()) || CORREGIDA.equals(q.getEstatus()) ? "En Recepción, sin validar"
                        : EN_VALIDACION.equals(q.getEstatus()) ? "En validación" : q.getEstatus());
        return new AntecedenteModel(q.getNumeroFolio(), q.getFechaCreacion(), q.getMotivo(), estado, relacion,
                q.getUnidadAcademicaClave(), esVacio(q.getDefensorAsignado()) ? null : q.getDefensorAsignado(),
                !ESTATUS_CERRADOS.contains(q.getEstatus()));
    }

    /**
     * Abogados disponibles para turnar, con su carga (2026-10-16), del que menos quejas en
     * trámite lleva al que más. Regla de sugerencia:
     *   1. Si un abogado ya lleva en trámite un antecedente de esta queja (misma persona
     *      quejosa o mismo denunciado), se le sugiere a él para dar continuidad aunque tenga
     *      más carga.
     *   2. Si no, se sugiere al de menor carga activa (empate: menos asignadas en total).
     */
    public List<CargaAbogadoModel> abogados(String folio) {
        List<AntecedenteModel> antecedentes = antecedentes(folio);
        Map<String, long[]> cargas = indicadoresService.cargaPorAbogado(ESTATUS_CERRADOS);

        List<PersonalAdministrativo> personal = personalRepository.findByRolInAndActivoTrueOrderByNombreCompletoAsc(
                List.of(ipn.escom.defensoria.revision_service.entity.RolStaff.DEFENSOR,
                        ipn.escom.defensoria.revision_service.entity.RolStaff.SUBDEFENSOR));

        record Fila(PersonalAdministrativo p, long activa, long total, List<String> folios) {}
        List<Fila> filas = personal.stream().map(p -> {
            long[] c = cargas.getOrDefault(p.getNombreCompleto().toLowerCase(), new long[] {0, 0});
            List<String> folios = antecedentes.stream()
                    .filter(a -> a.enCurso() && a.defensorAsignado() != null
                            && a.defensorAsignado().equalsIgnoreCase(p.getNombreCompleto()))
                    .map(AntecedenteModel::numeroFolio)
                    .toList();
            return new Fila(p, c[0], c[1], folios);
        }).sorted(java.util.Comparator.comparingLong(Fila::activa).thenComparingLong(Fila::total)
                // A igual carga, primero los abogados (SUBDEFENSOR) que la persona titular (DEFENSOR).
                .thenComparing(f -> f.p().getRol() == ipn.escom.defensoria.revision_service.entity.RolStaff.DEFENSOR)
                .thenComparing(f -> f.p().getNombreCompleto())).toList();

        Fila sugerida = filas.stream().filter(f -> !f.folios().isEmpty()).findFirst()
                .orElse(filas.isEmpty() ? null : filas.get(0));

        return filas.stream().map(f -> {
            boolean esSugerida = f == sugerida;
            String motivo = !esSugerida ? null
                    : !f.folios().isEmpty() ? "Ya lleva un antecedente de esta queja (" + String.join(", ", f.folios()) + ")"
                    : "Es quien tiene menos quejas en trámite";
            return new CargaAbogadoModel(f.p().getId(), f.p().getNombreCompleto(), f.p().getRol().name(),
                    f.activa(), f.total(), f.folios(), esSugerida, motivo);
        }).toList();
    }

    // ---------------- Rechazo ----------------

    public Queja rechazar(String folio, List<String> motivos, String observaciones, String correoRecepcionista) {
        Queja queja = obtenerPorFolio(folio);

        String motivosTexto = (motivos == null || motivos.isEmpty())
                ? ""
                : String.join("; ", motivos);
        String textoCompleto = motivosTexto
                + ((observaciones != null && !observaciones.isBlank())
                        ? (motivosTexto.isBlank() ? "" : "\n\n") + observaciones
                        : "");

        if (textoCompleto.isBlank()) {
            throw new RuntimeException("Selecciona al menos un motivo o escribe una observación.");
        }
        exigirQueSeaMia(queja, correoRecepcionista);

        queja.setEstatus(RECHAZADA);
        queja.setMotivoRechazo(textoCompleto);
        queja.setValidadoPor(correoRecepcionista);
        queja.setFechaValidacion(LocalDateTime.now());
        limpiarRevision(queja);
        Queja guardada = quejaRepository.save(queja);

        notificacionService.enviarCorreoRechazo(
                guardada.getCorreoInstitucional(), nombreMostrar(guardada), guardada.getNumeroFolio(), textoCompleto);
        notificacionService.registrarCambioEstatus(
                guardada.getCorreoInstitucional(), guardada.getNumeroFolio(),
                "Tu queja necesita observaciones",
                "Tu queja " + guardada.getNumeroFolio() + " fue revisada y requiere que atiendas algunas observaciones. "
                        + "Revisa el detalle para más información.");

        return guardada;
    }

    // ---------------- Turnado ----------------

    /** Todas las quejas turnadas van a Primer Contacto -- no existe otro destino posible hoy,
     * así que el área ya no se le pregunta al recepcionista (antes ofrecía un combo de
     * dependencias del IPN que no aplicaba aquí). Se deja fijo solo para que Historial siga
     * mostrando algo coherente en la columna "Área". */
    private static final String AREA_DESTINO = "Primer Contacto";

    public Queja turnar(
            String folio,
            String defensorAsignado,
            String comentarios,
            String correoRecepcionista
    ) {

        if (esVacio(defensorAsignado)) {
            throw new RuntimeException(
                    "Selecciona el defensor responsable antes de turnar."
            );
        }

        Queja queja = obtenerPorFolio(folio);

        /*
         * Si ya fue turnada correctamente anteriormente,
         * no volvemos a crear ni enviar el expediente.
         */
        if (TURNADA_O_POSTERIOR.contains(queja.getEstatus())
                && !esVacio(queja.getFolioPrimerContacto())) {
            return queja;
        }

        /*
         * Una queja rechazada ya no puede continuar
         * al flujo de Primer Contacto.
         */
        if (RECHAZADA.equals(queja.getEstatus())) {
            throw new RuntimeException(
                    "Una queja rechazada no puede ser turnada."
            );
        }

        exigirQueSeaMia(queja, correoRecepcionista);

        /*
         * Primero pedimos a Primer Contacto que cree
         * su expediente.
         *
         * Se manda el FOLIO de la queja, NO quejas.id.
         */
        PrimerContactoIngresoResponse expedientePC =
                primerContactoClientService
                        .enviarAPrimerContacto(queja);

        /*
         * Primer Contacto devuelve su propio folio.
         *
         * Ejemplo:
         * FOL-12345678 -> PC-A1B2C3D4
         */
        queja.setFolioPrimerContacto(
                expedientePC.getFolio()
        );

        queja.setEstatus(TURNADA);
        queja.setAreaTurnada(AREA_DESTINO);
        queja.setDefensorAsignado(defensorAsignado);
        queja.setComentariosRecepcion(comentarios);
        queja.setValidadoPor(correoRecepcionista);

        LocalDateTime ahora = LocalDateTime.now();

        queja.setFechaValidacion(ahora);
        queja.setFechaTurnado(ahora);
        limpiarRevision(queja);

        Queja guardada =
                quejaRepository.save(queja);

        notificacionService.registrarCambioEstatus(
                guardada.getCorreoInstitucional(),
                guardada.getNumeroFolio(),
                "Tu queja fue turnada",
                "Tu queja "
                        + guardada.getNumeroFolio()
                        + " fue admitida y turnada a "
                        + AREA_DESTINO
                        + " para su atención."
        );

        // Aviso en la campana del defensor/subdefensor al que se le asignó la queja.
        personalRepository.findFirstByNombreCompletoAndActivoTrue(defensorAsignado)
                .ifPresent(defensor -> notificacionService.notificarPersonal(
                        defensor.getCorreoInstitucional(),
                        "Se te turnó una queja",
                        "La queja " + guardada.getNumeroFolio() + " fue validada por Recepción y turnada a "
                                + AREA_DESTINO + "; quedaste como responsable."
                                + (esVacio(comentarios) ? "" : " Comentarios de Recepción: " + comentarios),
                        ""));

        return guardada;
    }

    // ---------------- Registro Manual ----------------

    public Queja registrarManual(
            String nombre, String apellido1, String apellido2,
            String tipoUsuario, String dependenciaClave, String numeroOficio,
            LocalDate fechaRecepcionFisica, String tipoDocumento,
            String descripcion, String ubicacionFisica,
            MultipartFile archivo, String correoRecepcionista,
            String correoContacto, String telefonoContacto) {

        if (esVacio(nombre) || esVacio(apellido1) || esVacio(descripcion)) {
            throw new RuntimeException("Completa al menos nombre, primer apellido y descripción del asunto.");
        }

        Queja queja = new Queja();
        queja.setNumeroFolio(generarFolio());
        // Si el recepcionista captura un correo de contacto real, se usa como correo
        // institucional para que el quejoso reciba las notificaciones automáticas
        // (rechazo/turnado) igual que un registro digital. Cuando no se tiene un correo
        // capturable de inmediato, se usa un valor de referencia interno para no romper
        // el NOT NULL de la columna compartida con queja-service.
        if (!esVacio(correoContacto)) {
            queja.setCorreoInstitucional(correoContacto.trim());
        } else {
            queja.setCorreoInstitucional("registro-manual+" + queja.getNumeroFolio().toLowerCase() + "@defensoria.ipn.mx");
        }
        queja.setTelefonoContacto(telefonoContacto);
        queja.setMotivo("Registro manual: " + tipoDocumento);
        queja.setDescripcion(descripcion);
        queja.setNombreQuejoso(nombre);
        queja.setApellido1Quejoso(apellido1);
        queja.setApellido2Quejoso(apellido2);
        queja.setTipoUsuarioManual(tipoUsuario);
        queja.setUnidadAcademicaClave(dependenciaClave);
        queja.setNumeroOficio(numeroOficio);
        queja.setFechaRecepcionFisica(fechaRecepcionFisica);
        queja.setTipoDocumentoFisico(tipoDocumento);
        queja.setUbicacionFisicaExpediente(ubicacionFisica);
        queja.setOrigenRegistro(ORIGEN_MANUAL);
        queja.setEstatus(RECIBIDA);

        if (archivo != null && !archivo.isEmpty()) {
            queja.getEvidencias().add(convertirAEvidencia(archivo, queja));
        }

        return quejaRepository.save(queja);
    }

    // ---------------- Historial ----------------

    /** fechaDesde/fechaHasta acotan un RANGO (ambos límites incluidos); cualquiera de los dos
     * puede venir vacío para dejar ese extremo abierto (ej. solo "hasta" = todo lo procesado
     * hasta esa fecha). Antes solo se podía filtrar un día exacto. */
    public List<HistorialItemModel> historial(String texto, String estatus, LocalDate fechaDesde, LocalDate fechaHasta) {
        List<String> estatusProcesadas = new java.util.ArrayList<>(TURNADA_O_POSTERIOR);
        estatusProcesadas.add(RECHAZADA);
        List<Queja> procesadas = quejaRepository.findByEstatusInOrderByFechaCreacionDesc(estatusProcesadas);

        return procesadas.stream()
                .filter(q -> estatus == null || estatus.isBlank() || etiquetaEstatusFinal(q.getEstatus()).equalsIgnoreCase(estatus))
                .filter(q -> fechaDesde == null || !q.getFechaCreacion().toLocalDate().isBefore(fechaDesde))
                .filter(q -> fechaHasta == null || !q.getFechaCreacion().toLocalDate().isAfter(fechaHasta))
                .filter(q -> texto == null || texto.isBlank() || coincideTexto(q, texto))
                .map(q -> new HistorialItemModel(
                        q.getNumeroFolio(), q.getFechaCreacion(), nombreMostrar(q),
                        ORIGEN_MANUAL.equals(q.getOrigenRegistro()) ? "Manual" : "Web",
                        etiquetaEstatusFinal(q.getEstatus()), q.getMotivoRechazo()))
                .toList();
    }

    /** Etiqueta legible del estatus en que va una queja ya atendida por Recepción. */
    private static final Map<String, String> ESTATUS_ACTUAL = Map.ofEntries(
            Map.entry(TURNADA, "Turnada a Primer Contacto"),
            Map.entry("EN_ANALISIS", "En análisis · Primer Contacto"),
            Map.entry("PROCEDENTE", "Procedente · Primer Contacto"),
            Map.entry("IMPROCEDENTE", "Improcedente"),
            Map.entry("REMITIDA", "Remitida a otra instancia"),
            Map.entry("RECIBIDA_SUBDEFENSORIA", "Recibida en Subdefensoría"),
            Map.entry("EN_INVESTIGACION", "En investigación · Subdefensoría"),
            Map.entry("EN_ESPERA_OFICIO", "En espera de oficio · Subdefensoría"),
            Map.entry("ELABORO_ACUERDO", "Acuerdo elaborado · Subdefensoría"),
            Map.entry("PENDIENTE_CONCILIACION", "Pendiente de conciliación"),
            Map.entry("PENDIENTE_CONCLUSION", "Pendiente de conclusión"),
            Map.entry("CONCLUIDA", "Concluida"),
            Map.entry(RECHAZADA, "Esperando corrección del quejoso"));

    /**
     * Historial paginado (2026-10-16): quejas que Recepción ya turnó o regresó con
     * observaciones, la atendida más recientemente primero, con totales del mismo filtro.
     * fechaDesde/fechaHasta acotan la fecha en que LLEGÓ la queja (ambos incluidos).
     */
    public HistorialPaginaModel historialPagina(String correo, String texto, String estatus,
                                                LocalDate fechaDesde, LocalDate fechaHasta,
                                                boolean soloMias, int pagina, int tamanio) {
        List<String> estatusBuscados = new java.util.ArrayList<>();
        if (estatus == null || estatus.isBlank() || "TURNADO".equalsIgnoreCase(estatus)) {
            estatusBuscados.addAll(TURNADA_O_POSTERIOR);
        }
        if (estatus == null || estatus.isBlank() || "RECHAZADO".equalsIgnoreCase(estatus)) {
            estatusBuscados.add(RECHAZADA);
        }
        LocalDateTime desde = fechaDesde != null ? fechaDesde.atStartOfDay() : LocalDateTime.of(1900, 1, 1, 0, 0);
        LocalDateTime hasta = fechaHasta != null ? fechaHasta.plusDays(1).atStartOfDay() : LocalDateTime.of(9999, 1, 1, 0, 0);
        String busqueda = texto == null ? "" : texto.trim().toLowerCase();
        String patron = "%" + busqueda.replace("%", "").replace("_", "") + "%";
        String yo = correo == null ? "" : correo;
        int tam = Math.max(1, Math.min(tamanio <= 0 ? TAMANIO_PAGINA_DEFAULT : tamanio, TAMANIO_PAGINA_MAXIMO));

        Page<Queja> resultado = quejaRepository.buscarHistorial(estatusBuscados, soloMias, yo, desde, hasta,
                busqueda, patron, PageRequest.of(Math.max(0, pagina), tam));

        Map<String, String> nombresPersonal = new HashMap<>();
        personalRepository.findAll().forEach(p -> {
            if (p.getCorreoInstitucional() != null) {
                nombresPersonal.put(p.getCorreoInstitucional().toLowerCase(), p.getNombreCompleto());
            }
        });

        List<HistorialFilaModel> filas = resultado.getContent().stream().map(q -> {
            LocalDateTime atencion = q.getFechaValidacion() != null ? q.getFechaValidacion() : q.getFechaTurnado();
            String atendio = esVacio(q.getValidadoPor()) ? null
                    : nombresPersonal.getOrDefault(q.getValidadoPor().toLowerCase(), q.getValidadoPor());
            return new HistorialFilaModel(
                    q.getNumeroFolio(), q.getFechaCreacion(), atencion, horasEntre(q.getFechaCreacion(), atencion),
                    nombreMostrar(q), q.getUnidadAcademicaClave(),
                    ORIGEN_MANUAL.equals(q.getOrigenRegistro()) ? "Ventanilla" : "Web",
                    etiquetaEstatusFinal(q.getEstatus()),
                    ESTATUS_ACTUAL.getOrDefault(q.getEstatus(), q.getEstatus()),
                    atendio,
                    q.getValidadoPor() != null && q.getValidadoPor().equalsIgnoreCase(yo),
                    q.getFolioPrimerContacto(), q.getDefensorAsignado());
        }).toList();

        // Totales del filtro completo (no solo de la página), con columnas ligeras.
        long total = 0, rechazadas = 0, mias = 0, conTiempo = 0;
        double horasSuma = 0;
        for (Object[] fila : quejaRepository.resumenHistorial(estatusBuscados, soloMias, yo, desde, hasta, busqueda, patron)) {
            total++;
            if (RECHAZADA.equals(fila[0])) {
                rechazadas++;
            }
            if (fila[1] != null && ((String) fila[1]).equalsIgnoreCase(yo)) {
                mias++;
            }
            Double horas = horasEntre((LocalDateTime) fila[2], (LocalDateTime) fila[3]);
            if (horas != null) {
                horasSuma += horas;
                conTiempo++;
            }
        }
        HistorialResumenModel resumen = new HistorialResumenModel(total, total - rechazadas, rechazadas, mias,
                conTiempo == 0 ? null : horasSuma / conTiempo);

        return new HistorialPaginaModel(resumen, filas, resultado.getNumber(), resultado.getSize(),
                resultado.getTotalElements(), resultado.getTotalPages());
    }

    private static Double horasEntre(LocalDateTime desde, LocalDateTime hasta) {
        if (desde == null || hasta == null || hasta.isBefore(desde)) {
            return null;
        }
        return java.time.Duration.between(desde, hasta).toMinutes() / 60.0;
    }

    // ---------------- Registro en ventanilla ----------------

    /**
     * La queja se capturó en ventanilla con el formulario público (queja-service) y aquí se
     * marca como MANUAL con los datos del documento físico. Solo aplica a quejas recién
     * creadas (2 horas) que siguen sin atenderse, para que no se pueda reclasificar después
     * una queja que llegó por el portal.
     */
    public void marcarVentanilla(String folio, DatosVentanillaRequest datos, String correoRecepcionista) {
        Queja queja = obtenerPorFolio(folio);
        boolean reciente = queja.getFechaCreacion() != null
                && queja.getFechaCreacion().isAfter(LocalDateTime.now().minusHours(2));
        if (!RECIBIDA.equals(queja.getEstatus()) || !reciente) {
            throw new RuntimeException("Solo se pueden marcar como de ventanilla las quejas recién capturadas.");
        }
        queja.setOrigenRegistro(ORIGEN_MANUAL);
        queja.setTipoDocumentoFisico(esVacio(datos.tipoDocumento()) ? "FORMATO_QUEJA" : datos.tipoDocumento().trim());
        queja.setNumeroOficio(esVacio(datos.numeroOficio()) ? null : datos.numeroOficio().trim());
        queja.setUbicacionFisicaExpediente(esVacio(datos.ubicacionFisica()) ? null : datos.ubicacionFisica().trim());
        if (!esVacio(datos.fechaRecepcionFisica())) {
            try {
                queja.setFechaRecepcionFisica(LocalDate.parse(datos.fechaRecepcionFisica().trim()));
            } catch (java.time.format.DateTimeParseException ex) {
                throw new RuntimeException("La fecha de recepción física no es válida (AAAA-MM-DD).");
            }
        }
        quejaRepository.save(queja);
    }

    public QuejaEvidencia obtenerEvidencia(Long id) {
        return evidenciaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("No se encontró el documento."));
    }

    // ---------------- helpers ----------------

    private Queja obtenerPorFolio(String folio) {
        return quejaRepository.findByNumeroFolio(folio)
                .orElseThrow(() -> new RuntimeException("No se encontró ninguna queja con el folio " + folio));
    }

    private QuejaDetalleModel aDetalle(Queja q) {
        List<EvidenciaResumen> evidencias = evidenciaRepository.findByQuejaId(q.getId()).stream()
                .map(ev -> new EvidenciaResumen(ev.getId(), ev.getNombreArchivo(), ev.getTipoMime(),
                        ev.getTamanioBytes(), ev.getFechaSubida()))
                .toList();

        return new QuejaDetalleModel(
                q.getNumeroFolio(), q.getFechaCreacion(), nombreMostrar(q), q.getCorreoInstitucional(),
                q.getTelefonoContacto(),
                q.getTipoIdentificacionQuejoso(), q.getNumeroIdentificacionQuejoso(),
                q.getMotivo(), q.getDescripcion(), q.getUnidadAcademicaClave(), q.getFechaHechos(),
                nombreDenunciadoCompleto(q), q.getOrigenRegistro(), q.getEstatus(), evidencias,
                q.getMotivoRechazo(), q.getAreaTurnada(), q.getDefensorAsignado(), q.getComentariosRecepcion());
    }

    private String nombreMostrar(Queja q) {
        String nombre = concatenarNoVacios(q.getNombreQuejoso(), q.getApellido1Quejoso(), q.getApellido2Quejoso());
        return nombre.isBlank() ? q.getCorreoInstitucional() : nombre;
    }

    private String nombreDenunciadoCompleto(Queja q) {
        String nombre = concatenarNoVacios(q.getNombreDenunciado(), q.getApellido1Denunciado());
        return nombre.isBlank() ? "No especificado" : nombre;
    }

    private String concatenarNoVacios(String... partes) {
        StringBuilder sb = new StringBuilder();
        for (String parte : partes) {
            if (parte != null && !parte.isBlank()) {
                if (sb.length() > 0) sb.append(' ');
                sb.append(parte.trim());
            }
        }
        return sb.toString();
    }

    private boolean documentacionAparenteCompleta(Queja q) {
        boolean tieneEvidencias = evidenciaRepository.countByQuejaId(q.getId()) > 0;
        boolean tieneDatosBasicos = !esVacio(q.getMotivo()) && !esVacio(q.getDescripcion());
        return tieneEvidencias && tieneDatosBasicos;
    }

    private String etiquetaEstatus(String estatus) {
        if (TURNADA_O_POSTERIOR.contains(estatus)) {
            return "TURNADA";
        }
        return RECHAZADA.equals(estatus) ? "RECHAZADA" : "EN PROCESO";
    }

    private String etiquetaEstatusFinal(String estatus) {
        return RECHAZADA.equals(estatus) ? "RECHAZADO" : "TURNADO";
    }

    private boolean coincideTexto(Queja q, String texto) {
        String t = texto.toLowerCase();
        return (q.getNumeroFolio() != null && q.getNumeroFolio().toLowerCase().contains(t))
                || nombreMostrar(q).toLowerCase().contains(t)
                || (q.getUnidadAcademicaClave() != null && q.getUnidadAcademicaClave().toLowerCase().contains(t));
    }

    private boolean esVacio(String valor) {
        return valor == null || valor.isBlank();
    }

    private String generarFolio() {
        return PREFIJO_FOLIO + UUID.randomUUID().toString().substring(0, LONGITUD_UUID_FOLIO).toUpperCase();
    }

    private QuejaEvidencia convertirAEvidencia(MultipartFile archivo, Queja queja) {
        try {
            QuejaEvidencia evidencia = new QuejaEvidencia();
            evidencia.setQueja(queja);
            evidencia.setNombreArchivo(archivo.getOriginalFilename());
            evidencia.setTipoMime(archivo.getContentType());
            evidencia.setTamanioBytes(archivo.getSize());
            evidencia.setContenido(archivo.getBytes());
            return evidencia;
        } catch (IOException e) {
            throw new RuntimeException("Error al leer el archivo adjunto: " + e.getMessage());
        }
    }
}
