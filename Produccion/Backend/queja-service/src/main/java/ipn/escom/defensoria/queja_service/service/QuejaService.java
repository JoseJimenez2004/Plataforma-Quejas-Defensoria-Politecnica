package ipn.escom.defensoria.queja_service.service;


import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import ipn.escom.defensoria.queja_service.dto.EditarQuejaRequest;
import ipn.escom.defensoria.queja_service.dto.EvidenciaResumen;
import ipn.escom.defensoria.queja_service.dto.RegistroQuejaPublicaRequest;
import ipn.escom.defensoria.queja_service.entity.Queja;
import ipn.escom.defensoria.queja_service.entity.QuejaEvidencia;
import ipn.escom.defensoria.queja_service.entity.QuejaTutor;
import ipn.escom.defensoria.queja_service.repository.QuejaEvidenciaRepository;
import ipn.escom.defensoria.queja_service.repository.QuejaRepository;
import ipn.escom.defensoria.queja_service.validacion.ReglasQueja;
import ipn.escom.defensoria.queja_service.validacion.ValidacionException;
import ipn.escom.defensoria.queja_service.validacion.ValidadorArchivos;
import ipn.escom.defensoria.queja_service.validacion.ValidadorQuejaPublica;

@Service
public class QuejaService {

    private static final String ORIGEN_AUTENTICADO = "AUTENTICADO";
    private static final String ORIGEN_PUBLICO = "PUBLICO";
    private static final String ESTATUS_RECIBIDA = "RECIBIDA";
    private static final String PREFIJO_FOLIO = "FOL-";
    private static final int LONGITUD_UUID_FOLIO = 8;
    /** Estatus al que pasa una queja que el propio quejoso retira. NO se borra de la base:
     * en un sistema de quejas institucional, destruir el registro elimina la constancia de
     * que la queja existió — y si alguien la presentó y luego la retiró bajo presión, no
     * quedaría ningún rastro. Ver docs/CAMBIOS.md. */
    private static final String ESTATUS_CANCELADA = "CANCELADA";
    /** Estatus de una queja que Recepción rechazó por faltarle algo del checklist. */
    private static final String ESTATUS_RECHAZADA = "RECHAZADA";
    /**
     * El quejoso ya atendió las observaciones del rechazo y la queja vuelve a la bandeja.
     * Antes de esto, una queja rechazada era un callejón sin salida: no se podía editar
     * (el estatus ya no era RECIBIDA) ni turnar ("Una queja rechazada no puede ser
     * turnada"), así que quedaba muerta en la base.
     */
    private static final String ESTATUS_CORREGIDA = "CORREGIDA";
    private static final String TIPO_IDENTIFICACION = "IDENTIFICACION";
    private static final String TIPO_EVIDENCIA = "EVIDENCIA";

    private final QuejaRepository quejaRepository;
    private final QuejaEvidenciaRepository quejaEvidenciaRepository;
    private final NotificacionClienteService notificacionClienteService;
    private final ValidadorQuejaPublica validadorQuejaPublica;

    public QuejaService(QuejaRepository quejaRepository,
                        QuejaEvidenciaRepository quejaEvidenciaRepository,
                        NotificacionClienteService notificacionClienteService,
                        ValidadorQuejaPublica validadorQuejaPublica) {
        this.quejaRepository = quejaRepository;
        this.quejaEvidenciaRepository = quejaEvidenciaRepository;
        this.notificacionClienteService = notificacionClienteService;
        this.validadorQuejaPublica = validadorQuejaPublica;
    }

    public boolean validarFolioYCorreo(String folio, String correo) {
        return quejaRepository.findByNumeroFolioAndCorreoInstitucional(folio, correo).isPresent();
    }

    /**
     * Devuelve el detalle de una queja dado folio + correo (misma llave de acceso que
     * {@link #validarFolioYCorreo}, pero regresando los datos en vez de solo true/false).
     * La usa el propio frontend (pantalla "Consultar queja" / activación de cuenta) y
     * auth-service (para poblar nombre/boleta reales al activar una cuenta en vez de dejar
     * placeholders como "Ciudadano Defensoría").
     */
    public Queja obtenerPorFolioYCorreo(String folio, String correo) {
        return quejaRepository.findByNumeroFolioAndCorreoInstitucional(folio, correo)
                .orElseThrow(() -> new RuntimeException("El folio no existe o el correo no coincide con el registro."));
    }

    /** "Mis Quejas" del panel autenticado — todas las quejas del correo que viene en el JWT. */
    public List<Queja> listarMisQuejas(String correo) {
        return quejaRepository.findByCorreoInstitucionalOrderByFechaCreacionDesc(correo);
    }

    /** Detalle de una queja propia (panel autenticado) — a diferencia de
     * {@link #obtenerPorFolioYCorreo}, aquí el correo viene del JWT verificado, no de un
     * parámetro que cualquiera podría manipular. */
    public Queja obtenerMiQueja(String folio, String correo) {
        Queja queja = quejaRepository.findByNumeroFolioAndCorreoInstitucional(folio, correo)
                .orElseThrow(() -> new RuntimeException("No se encontró esa queja asociada a tu cuenta."));
        return queja;
    }

    /** Edita una queja propia MIENTRAS siga en estatus "RECIBIDA" -- una vez que pasa a
     * revisión (recepcionista la valida/rechaza/turna), la información queda definitiva, tal
     * como ya se le advertía al quejoso en el formulario ("Nota importante"). Solo se tocan
     * los campos que vienen no-nulos en la petición. */
    /**
     * El quejoso atiende las observaciones de un rechazo y reenvía su queja a Recepción.
     * Solo aplica sobre una queja RECHAZADA; la deja en CORREGIDA, que es como vuelve a
     * aparecer en la bandeja del recepcionista para una segunda validación.
     */
    public Queja corregirMiQueja(String folio, String correo, EditarQuejaRequest datos) {
        Queja queja = obtenerMiQueja(folio, correo);

        if (!ESTATUS_RECHAZADA.equalsIgnoreCase(queja.getEstatus())) {
            throw new RuntimeException(
                    "Solo se puede corregir una queja que fue rechazada. Esta queja está en estatus "
                            + queja.getEstatus() + ".");
        }

        aplicarCambios(queja, datos);
        queja.setEstatus(ESTATUS_CORREGIDA);

        Queja guardada = quejaRepository.save(queja);
        notificacionClienteService.notificarCambioEstatus(
                guardada.getCorreoInstitucional(),
                guardada.getNumeroFolio(),
                "Queja corregida",
                "Reenviaste tu queja con folio " + guardada.getNumeroFolio()
                        + ". Recepción la revisará de nuevo.");
        return guardada;
    }

    public Queja editarMiQueja(String folio, String correo, EditarQuejaRequest datos) {
        Queja queja = obtenerMiQueja(folio, correo);

        String estatusActual = queja.getEstatus() == null ? ESTATUS_RECIBIDA : queja.getEstatus();
        if (!ESTATUS_RECIBIDA.equalsIgnoreCase(estatusActual)) {
            throw new RuntimeException(
                    "Esta queja ya está en revisión y no se puede editar. Su información es definitiva.");
        }

        aplicarCambios(queja, datos);
        return quejaRepository.save(queja);
    }

    /**
     * Aplica sobre la queja solo los campos que vienen no-nulos. Compartido por
     * editarMiQueja (queja aún RECIBIDA) y corregirMiQueja (queja RECHAZADA que el quejoso
     * reenvía): son la misma edición, lo que cambia es desde qué estatus se permite y a
     * cuál se va.
     */
    private void aplicarCambios(Queja queja, EditarQuejaRequest datos) {
        if (datos.getDescripcion() != null) {
            if (esVacio(datos.getDescripcion())) {
                throw new RuntimeException("La descripción no puede quedar vacía.");
            }
            queja.setDescripcion(datos.getDescripcion());
        }
        if (datos.getUnidadAcademicaClave() != null) {
            queja.setUnidadAcademicaClave(datos.getUnidadAcademicaClave());
        }
        if (datos.getFechaHechos() != null) {
            queja.setFechaHechos(datos.getFechaHechos());
        }
        if (datos.getNombreDenunciado() != null) {
            queja.setNombreDenunciado(datos.getNombreDenunciado().isBlank() ? null : datos.getNombreDenunciado());
        }
        if (datos.getApellido1Denunciado() != null) {
            queja.setApellido1Denunciado(datos.getApellido1Denunciado().isBlank() ? null : datos.getApellido1Denunciado());
        }
        if (datos.getApellido2Denunciado() != null) {
            queja.setApellido2Denunciado(
                    datos.getApellido2Denunciado().isBlank() ? null : datos.getApellido2Denunciado());
        }
    }

    /** Evidencias de una queja propia, SIN el contenido binario (solo para listarlas en el
     * detalle) — ver {@link EvidenciaResumen}. */
    public List<EvidenciaResumen> listarEvidencias(String folio, String correo) {
        Queja queja = obtenerMiQueja(folio, correo);
        return queja.getEvidencias().stream()
                .map(this::aResumen)
                .toList();
    }

    // =========================================================================================
    // CU-Q07 — Gestión de la propia queja desde el panel
    // =========================================================================================

    /**
     * Retira una queja del historial del quejoso. NO la borra: la marca como CANCELADA.
     *
     * Solo se permite mientras siga en "RECIBIDA", igual que la edición: una vez que el
     * recepcionista empezó a validarla, ya hay trabajo institucional invertido y el quejoso
     * no puede hacerla desaparecer por su cuenta.
     */
    @Transactional
    public Queja cancelarMiQueja(String folio, String correo) {
        Queja queja = obtenerMiQueja(folio, correo);
        exigirEstatusRecibida(queja,
                "Esta queja ya está en revisión y no se puede eliminar. "
                        + "Si necesitas retirarla, comunícate con la Defensoría.");

        queja.setEstatus(ESTATUS_CANCELADA);
        return quejaRepository.save(queja);
    }

    /**
     * Agrega evidencias a una queja propia que siga en "RECIBIDA".
     *
     * Las reglas de tipo y tamaño son las mismas del registro inicial, verificadas por la
     * firma binaria del archivo y no por su extensión.
     */
    @Transactional
    public List<EvidenciaResumen> agregarEvidencias(String folio, String correo,
                                                    List<MultipartFile> archivos) {
        Queja queja = obtenerMiQueja(folio, correo);
        exigirEstatusRecibida(queja,
                "Esta queja ya está en revisión: no se le pueden agregar más evidencias.");

        ValidadorArchivos.validarEvidenciasAdicionales(archivos);
        agregarArchivos(queja, archivos, TIPO_EVIDENCIA);

        Queja guardada = quejaRepository.save(queja);
        return guardada.getEvidencias().stream().map(this::aResumen).toList();
    }

    /**
     * Quita una evidencia de una queja propia que siga en "RECIBIDA".
     *
     * La credencial oficial no se puede quitar si es la única que queda: la queja dejaría de
     * tener con qué acreditar la identidad de quien la presentó, y el recepcionista tendría
     * que rechazarla.
     */
    @Transactional
    public void eliminarEvidencia(String folio, String correo, Long evidenciaId) {
        Queja queja = obtenerMiQueja(folio, correo);
        exigirEstatusRecibida(queja,
                "Esta queja ya está en revisión: sus evidencias son definitivas.");

        QuejaEvidencia evidencia = buscarEvidencia(queja, evidenciaId);

        if (esUltimaIdentificacion(queja, evidencia)) {
            throw new ValidacionException(
                    "No puedes quitar tu identificación oficial: la queja debe conservar al menos una. "
                            + "Sube la nueva antes de quitar esta.");
        }

        queja.getEvidencias().remove(evidencia);
        quejaEvidenciaRepository.delete(evidencia);
    }

    /** Contenido binario de una evidencia propia — lo usa el panel para previsualizarla. */
    public QuejaEvidencia obtenerEvidencia(String folio, String correo, Long evidenciaId) {
        Queja queja = obtenerMiQueja(folio, correo);
        return buscarEvidencia(queja, evidenciaId);
    }

    // -----------------------------------------------------------------------------------------

    private void exigirEstatusRecibida(Queja queja, String mensajeSiNoAplica) {
        String estatusActual = queja.getEstatus() == null ? ESTATUS_RECIBIDA : queja.getEstatus();
        if (!ESTATUS_RECIBIDA.equalsIgnoreCase(estatusActual)) {
            throw new ValidacionException(mensajeSiNoAplica);
        }
    }

    private QuejaEvidencia buscarEvidencia(Queja queja, Long evidenciaId) {
        return queja.getEvidencias().stream()
                .filter(ev -> ev.getId().equals(evidenciaId))
                .findFirst()
                // Se busca DENTRO de la queja del usuario, no por id suelto en la tabla: así
                // nadie puede borrar la evidencia de otra persona mandando un id ajeno.
                .orElseThrow(() -> new ValidacionException(
                        "Esa evidencia no existe o no pertenece a esta queja."));
    }

    private boolean esUltimaIdentificacion(Queja queja, QuejaEvidencia evidencia) {
        if (!TIPO_IDENTIFICACION.equals(evidencia.getTipo())) {
            return false;
        }
        long identificaciones = queja.getEvidencias().stream()
                .filter(ev -> TIPO_IDENTIFICACION.equals(ev.getTipo()))
                .count();
        return identificaciones <= 1;
    }

    private EvidenciaResumen aResumen(QuejaEvidencia ev) {
        return new EvidenciaResumen(ev.getId(), ev.getNombreArchivo(), ev.getTipoMime(),
                ev.getTamanioBytes(), ev.getFechaSubida(), ev.getTipo());
    }

    /**
     * Registra una queja de un usuario ya autenticado (con JWT). Los datos estructurados
     * (unidad académica, fecha de hechos, denunciado) se guardan como columnas propias en
     * vez de concatenarse como texto libre dentro de "descripcion", como se hacía antes.
     */
    public Queja registrarQueja(
            String motivo,
            String descripcion,
            String correo,
            String unidadAcademicaClave,
            LocalDate fechaHechos,
            String nombreDenunciado,
            String apellido1Denunciado,
            List<MultipartFile> archivos) {

        if (esVacio(motivo) || esVacio(descripcion)) {
            throw new RuntimeException("Faltan datos obligatorios de la queja.");
        }

        Queja queja = new Queja();
        queja.setNumeroFolio(generarFolio());
        queja.setCorreoInstitucional(correo);
        queja.setMotivo(motivo);
        queja.setDescripcion(descripcion);
        queja.setUnidadAcademicaClave(unidadAcademicaClave);
        queja.setFechaHechos(fechaHechos);
        queja.setNombreDenunciado(nombreDenunciado);
        queja.setApellido1Denunciado(apellido1Denunciado);
        queja.setOrigenRegistro(ORIGEN_AUTENTICADO);

        agregarEvidencias(queja, archivos);

        Queja guardada = quejaRepository.save(queja);
        notificacionClienteService.notificarQuejaCreada(guardada.getCorreoInstitucional(), guardada.getNumeroFolio());
        return guardada;
    }

    /**
     * Registra una queja del formulario público, sin necesidad de sesión iniciada (decisión
     * explícita del usuario: "quiero que las quejas estén tanto para nuevos usuarios como
     * para los que ya están registrados"). Guarda la identidad completa del quejoso (no solo
     * el correo, como en el flujo autenticado) y, si aplica, los datos del tutor.
     */
    public Queja registrarQuejaPublica(RegistroQuejaPublicaRequest datos) {
        // El validador normaliza los textos del request (trim, espacios colapsados, correo en
        // minúsculas) y clasifica los archivos en credencial oficial vs. evidencias, cada
        // grupo con sus propias reglas de tipo y tamaño.
        ValidadorArchivos.ArchivosClasificados archivos = validadorQuejaPublica.validar(datos);

        Queja queja = new Queja();
        queja.setNumeroFolio(generarFolio());
        queja.setCorreoInstitucional(datos.getCorreo());
        queja.setMotivo("Queja en " + datos.getUnidadAcademicaClave());
        queja.setDescripcion(datos.getDescripcion());
        queja.setNombreQuejoso(datos.getNombre());
        queja.setApellido1Quejoso(datos.getApellido1());
        queja.setApellido2Quejoso(datos.getApellido2());
        queja.setFechaNacimientoQuejoso(datos.getFechaNacimiento());
        queja.setTipoIdentificacionQuejoso(datos.getTipoIdentificacion());
        queja.setNumeroIdentificacionQuejoso(datos.getNumeroIdentificacion());
        queja.setUnidadAcademicaClave(datos.getUnidadAcademicaClave());
        queja.setFechaHechos(datos.getFechaHechos());
        queja.setNombreDenunciado(datos.getNombreDenunciado());
        queja.setApellido1Denunciado(datos.getApellido1Denunciado());
        queja.setApellido2Denunciado(datos.getApellido2Denunciado());
        queja.setOrigenRegistro(ORIGEN_PUBLICO);

        // Constancia del consentimiento: la fecha la pone el servidor, no el cliente.
        queja.setAvisoPrivacidadAceptado(Boolean.TRUE);
        queja.setAvisoPrivacidadFecha(LocalDateTime.now());
        queja.setAvisoPrivacidadVersion(
                datos.getAvisoPrivacidadVersion() == null || datos.getAvisoPrivacidadVersion().isBlank()
                        ? ReglasQueja.AVISO_PRIVACIDAD_VERSION
                        : datos.getAvisoPrivacidadVersion());

        if (datos.tieneTutor()) {
            QuejaTutor tutor = new QuejaTutor();
            tutor.setQueja(queja);
            tutor.setNombre(datos.getTutorNombre());
            tutor.setApellido1(datos.getTutorApellido1());
            tutor.setApellido2(datos.getTutorApellido2());
            tutor.setParentesco(datos.getTutorParentesco());
            tutor.setCorreo(datos.getTutorCorreo());
            tutor.setTelefono(datos.getTutorTelefono());
            queja.setTutor(tutor);
        }

        agregarArchivos(queja, archivos.identificaciones(), TIPO_IDENTIFICACION);
        agregarArchivos(queja, archivos.evidencias(), TIPO_EVIDENCIA);

        Queja guardada = quejaRepository.save(queja);
        notificacionClienteService.notificarQuejaCreada(guardada.getCorreoInstitucional(), guardada.getNumeroFolio());

        if (datos.tieneTutor() && datos.getTutorCorreo() != null && !datos.getTutorCorreo().isBlank()) {
            String nombreMenor = (datos.getNombre() + " " + datos.getApellido1()).trim();
            notificacionClienteService.notificarTutorQuejaCreada(
                    datos.getTutorCorreo(), datos.getTutorNombre(), nombreMenor, guardada.getNumeroFolio());
        }

        return guardada;
    }

    private boolean esVacio(String valor) {
        return valor == null || valor.isBlank();
    }

    private String generarFolio() {
        return PREFIJO_FOLIO + UUID.randomUUID().toString().substring(0, LONGITUD_UUID_FOLIO).toUpperCase();
    }

    /** Registro autenticado (/registrar): todos los archivos entran como evidencia simple. */
    private void agregarEvidencias(Queja queja, List<MultipartFile> archivos) {
        agregarArchivos(queja, archivos, TIPO_EVIDENCIA);
    }

    private void agregarArchivos(Queja queja, List<MultipartFile> archivos, String tipo) {
        if (archivos == null) {
            return;
        }
        for (MultipartFile archivo : archivos) {
            if (archivo == null || archivo.isEmpty()) {
                continue;
            }
            queja.getEvidencias().add(convertirAEvidencia(archivo, queja, tipo));
        }
    }

    private QuejaEvidencia convertirAEvidencia(MultipartFile archivo, Queja queja, String tipo) {
        try {
            QuejaEvidencia evidencia = new QuejaEvidencia();
            evidencia.setQueja(queja);
            evidencia.setNombreArchivo(archivo.getOriginalFilename());
            evidencia.setTipoMime(archivo.getContentType());
            evidencia.setTamanioBytes(archivo.getSize());
            evidencia.setContenido(archivo.getBytes());
            evidencia.setTipo(tipo);
            return evidencia;
        } catch (IOException e) {
            throw new ValidacionException(
                    "No se pudo leer el archivo \"" + archivo.getOriginalFilename() + "\".");
        }
    }
}