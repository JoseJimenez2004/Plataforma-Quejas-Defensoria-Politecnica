package ipn.escom.defensoria.queja_service.service;


import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import ipn.escom.defensoria.queja_service.dto.EditarQuejaRequest;
import ipn.escom.defensoria.queja_service.dto.EvidenciaResumen;
import ipn.escom.defensoria.queja_service.dto.RegistroQuejaPublicaRequest;
import ipn.escom.defensoria.queja_service.entity.Queja;
import ipn.escom.defensoria.queja_service.entity.QuejaEvidencia;
import ipn.escom.defensoria.queja_service.entity.QuejaTutor;
import ipn.escom.defensoria.queja_service.repository.QuejaRepository;

@Service
public class QuejaService {

    private static final String ORIGEN_AUTENTICADO = "AUTENTICADO";
    private static final String ORIGEN_PUBLICO = "PUBLICO";
    private static final String ESTATUS_RECIBIDA = "RECIBIDA";
    private static final String PREFIJO_FOLIO = "FOL-";
    private static final int LONGITUD_UUID_FOLIO = 8;
    private static final int LONGITUD_MAXIMA_IDENTIFICACION = 12;

    private final QuejaRepository quejaRepository;
    private final NotificacionClienteService notificacionClienteService;

    public QuejaService(QuejaRepository quejaRepository, NotificacionClienteService notificacionClienteService) {
        this.quejaRepository = quejaRepository;
        this.notificacionClienteService = notificacionClienteService;
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
    public Queja editarMiQueja(String folio, String correo, EditarQuejaRequest datos) {
        Queja queja = obtenerMiQueja(folio, correo);

        String estatusActual = queja.getEstatus() == null ? ESTATUS_RECIBIDA : queja.getEstatus();
        if (!ESTATUS_RECIBIDA.equalsIgnoreCase(estatusActual)) {
            throw new RuntimeException(
                    "Esta queja ya está en revisión y no se puede editar. Su información es definitiva.");
        }

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
        if (datos.getApellidoDenunciado() != null) {
            queja.setApellidoDenunciado(datos.getApellidoDenunciado().isBlank() ? null : datos.getApellidoDenunciado());
        }

        return quejaRepository.save(queja);
    }

    /** Evidencias de una queja propia, SIN el contenido binario (solo para listarlas en el
     * detalle) — ver {@link EvidenciaResumen}. */
    public List<EvidenciaResumen> listarEvidencias(String folio, String correo) {
        Queja queja = obtenerMiQueja(folio, correo);
        return queja.getEvidencias().stream()
                .map(ev -> new EvidenciaResumen(
                        ev.getId(), ev.getNombreArchivo(), ev.getTipoMime(),
                        ev.getTamanioBytes(), ev.getFechaSubida()))
                .toList();
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
            String apellidoDenunciado,
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
        queja.setApellidoDenunciado(apellidoDenunciado);
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
        validarDatosPublicos(datos);

        Queja queja = new Queja();
        queja.setNumeroFolio(generarFolio());
        queja.setCorreoInstitucional(datos.getCorreo());
        queja.setMotivo("Queja en " + datos.getUnidadAcademicaClave());
        queja.setDescripcion(datos.getDescripcion());
        queja.setNombreQuejoso(datos.getNombre());
        queja.setApellidoPaternoQuejoso(datos.getApellidoPaterno());
        queja.setApellidoMaternoQuejoso(datos.getApellidoMaterno());
        queja.setFechaNacimientoQuejoso(datos.getFechaNacimiento());
        queja.setTipoIdentificacionQuejoso(datos.getTipoIdentificacion());
        queja.setNumeroIdentificacionQuejoso(datos.getNumeroIdentificacion());
        queja.setUnidadAcademicaClave(datos.getUnidadAcademicaClave());
        queja.setFechaHechos(datos.getFechaHechos());
        queja.setNombreDenunciado(datos.getNombreDenunciado());
        queja.setApellidoDenunciado(datos.getApellidoDenunciado());
        queja.setOrigenRegistro(ORIGEN_PUBLICO);

        if (datos.tieneTutor()) {
            QuejaTutor tutor = new QuejaTutor();
            tutor.setQueja(queja);
            tutor.setNombre(datos.getTutorNombre());
            tutor.setApellidoPaterno(datos.getTutorApellidoPaterno());
            tutor.setApellidoMaterno(datos.getTutorApellidoMaterno());
            tutor.setParentesco(datos.getTutorParentesco());
            tutor.setCorreo(datos.getTutorCorreo());
            tutor.setTelefono(datos.getTutorTelefono());
            queja.setTutor(tutor);
        }

        agregarEvidencias(queja, datos.getArchivos());

        Queja guardada = quejaRepository.save(queja);
        notificacionClienteService.notificarQuejaCreada(guardada.getCorreoInstitucional(), guardada.getNumeroFolio());

        if (datos.tieneTutor() && datos.getTutorCorreo() != null && !datos.getTutorCorreo().isBlank()) {
            String nombreMenor = (datos.getNombre() + " " + datos.getApellidoPaterno()).trim();
            notificacionClienteService.notificarTutorQuejaCreada(
                    datos.getTutorCorreo(), datos.getTutorNombre(), nombreMenor, guardada.getNumeroFolio());
        }

        return guardada;
    }

    private void validarDatosPublicos(RegistroQuejaPublicaRequest datos) {
        if (esVacio(datos.getNombre()) || esVacio(datos.getApellidoPaterno()) || esVacio(datos.getCorreo())) {
            throw new RuntimeException("Faltan datos del quejoso (nombre, apellido o correo).");
        }
        if (datos.getFechaNacimiento() == null) {
            throw new RuntimeException("Falta la fecha de nacimiento.");
        }
        if (esVacio(datos.getTipoIdentificacion()) || esVacio(datos.getNumeroIdentificacion())) {
            throw new RuntimeException("Falta el número de boleta o de empleado.");
        }
        if (!datos.getNumeroIdentificacion().matches("\\d+")) {
            throw new RuntimeException("El número de boleta o de empleado solo puede contener números.");
        }
        if (datos.getNumeroIdentificacion().length() > LONGITUD_MAXIMA_IDENTIFICACION) {
            throw new RuntimeException("El número de boleta o de empleado no puede tener más de 12 caracteres.");
        }
        if (esVacio(datos.getUnidadAcademicaClave())) {
            throw new RuntimeException("Falta la unidad académica donde ocurrieron los hechos.");
        }
        if (datos.getFechaHechos() == null) {
            throw new RuntimeException("Falta la fecha de los hechos.");
        }
        if (esVacio(datos.getDescripcion())) {
            throw new RuntimeException("Falta la descripción de los hechos.");
        }
    }

    private boolean esVacio(String valor) {
        return valor == null || valor.isBlank();
    }

    private String generarFolio() {
        return PREFIJO_FOLIO + UUID.randomUUID().toString().substring(0, LONGITUD_UUID_FOLIO).toUpperCase();
    }

    private void agregarEvidencias(Queja queja, List<MultipartFile> archivos) {
        if (archivos == null) {
            return;
        }
        for (MultipartFile archivo : archivos) {
            if (archivo == null || archivo.isEmpty()) {
                continue;
            }
            queja.getEvidencias().add(convertirAEvidencia(archivo, queja));
        }
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
            throw new RuntimeException("Error al leer el archivo de evidencia: " + e.getMessage());
        }
    }
}