package ipn.escom.defensoria.revision_service.service;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import ipn.escom.defensoria.revision_service.dto.EvidenciaResumen;
import ipn.escom.defensoria.revision_service.entity.PersonalAdministrativo;
import ipn.escom.defensoria.revision_service.entity.Queja;
import ipn.escom.defensoria.revision_service.entity.QuejaEvidencia;
import ipn.escom.defensoria.revision_service.model.AntecedenteModel;
import ipn.escom.defensoria.revision_service.model.BandejaResumenModel;
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

    public RevisionQuejaService(
            QuejaRepository quejaRepository,
            QuejaEvidenciaRepository evidenciaRepository,
            NotificacionQuejaService notificacionService,
            PrimerContactoClientService primerContactoClientService,
            PersonalAdministrativoRepository personalRepository) {
        this.quejaRepository = quejaRepository;
        this.evidenciaRepository = evidenciaRepository;
        this.notificacionService = notificacionService;
        this.primerContactoClientService = primerContactoClientService;
        this.personalRepository = personalRepository;
    }

    // ---------------- Bandeja de Entrada ----------------

    public BandejaResumenModel bandeja() {
        // Antes de contar/listar, se liberan las que quedaron abandonadas EN_VALIDACION --
        // así la bandeja nunca muestra "en revisión" a alguien que ya no está ahí.
        liberarRevisionesVencidas();

        // Las corregidas cuentan como pendientes: son trabajo por atender, igual que una
        // queja nueva, solo que ya trae una vuelta encima.
        long pendientes = quejaRepository.countByEstatus(RECIBIDA)
                + quejaRepository.countByEstatus(CORREGIDA);
        long enProceso = quejaRepository.countByEstatus(EN_VALIDACION);

        LocalDateTime inicioHoy = LocalDate.now().atStartOfDay();
        LocalDateTime finHoy = LocalDate.now().atTime(LocalTime.MAX);
        long turnadasHoy = quejaRepository.countByEstatusAndFechaTurnadoBetween(TURNADA, inicioHoy, finHoy);

        List<QuejaResumenBandejaModel> lista = quejaRepository
                .findByEstatusInOrderByFechaCreacionAsc(List.of(RECIBIDA, CORREGIDA, EN_VALIDACION))
                .stream()
                .map(q -> new QuejaResumenBandejaModel(
                        q.getNumeroFolio(),
                        q.getFechaCreacion(),
                        nombreMostrar(q),
                        documentacionAparenteCompleta(q),
                        q.getEstatus(),
                        q.getRevisandoPorNombre()))
                .toList();

        return new BandejaResumenModel(pendientes, enProceso, turnadasHoy, lista);
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

        boolean otraPersonaLaTiene = EN_VALIDACION.equals(queja.getEstatus())
                && queja.getRevisandoPor() != null
                && !queja.getRevisandoPor().equalsIgnoreCase(correoRecepcionista);
        if (otraPersonaLaTiene) {
            String quien = queja.getRevisandoPorNombre() != null
                    ? queja.getRevisandoPorNombre()
                    : queja.getRevisandoPor();
            throw new RuntimeException(
                    "Esta queja la está revisando en este momento " + quien + ". "
                            + "Elige otra de la bandeja; esta se libera sola si " + quien
                            + " la deja inactiva más de " + MINUTOS_LIMITE_REVISION + " minutos.");
        }

        if (RECIBIDA.equals(queja.getEstatus()) || CORREGIDA.equals(queja.getEstatus())) {
            queja.setEstatusPrevioRevision(queja.getEstatus());
            queja.setEstatus(EN_VALIDACION);
            queja.setRevisandoPor(correoRecepcionista);
            queja.setRevisandoPorNombre(resolverNombre(correoRecepcionista));
            queja.setFechaInicioRevision(LocalDateTime.now());
            quejaRepository.save(queja);
        }
        return aDetalle(queja);
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

    public List<AntecedenteModel> antecedentes(String folio) {
        Queja queja = obtenerPorFolio(folio);

        List<Queja> previas;
        if (queja.getNumeroIdentificacionQuejoso() != null && !queja.getNumeroIdentificacionQuejoso().isBlank()) {
            previas = quejaRepository.findByNumeroIdentificacionQuejosoAndIdNotOrderByFechaCreacionDesc(
                    queja.getNumeroIdentificacionQuejoso(), queja.getId());
        } else {
            previas = quejaRepository.findByCorreoInstitucionalAndIdNotOrderByFechaCreacionDesc(
                    queja.getCorreoInstitucional(), queja.getId());
        }

        return previas.stream()
                .map(q -> new AntecedenteModel(q.getNumeroFolio(), q.getFechaCreacion(), q.getMotivo(), etiquetaEstatus(q.getEstatus())))
                .toList();
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
        if (TURNADA.equals(queja.getEstatus())
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
        List<Queja> procesadas = quejaRepository.findByEstatusInOrderByFechaCreacionDesc(List.of(RECHAZADA, TURNADA));

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
        boolean tieneEvidencias = !evidenciaRepository.findByQuejaId(q.getId()).isEmpty();
        boolean tieneDatosBasicos = !esVacio(q.getMotivo()) && !esVacio(q.getDescripcion());
        return tieneEvidencias && tieneDatosBasicos;
    }

    private String etiquetaEstatus(String estatus) {
        return switch (estatus == null ? RECIBIDA : estatus) {
            case TURNADA -> "TURNADA";
            case RECHAZADA -> "RECHAZADA";
            default -> "EN PROCESO";
        };
    }

    private String etiquetaEstatusFinal(String estatus) {
        return TURNADA.equals(estatus) ? "TURNADO" : "RECHAZADO";
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
