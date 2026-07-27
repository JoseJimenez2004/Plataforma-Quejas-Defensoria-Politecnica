package ipn.escom.defensoria.revision_service.service;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import ipn.escom.defensoria.revision_service.dto.EvidenciaResumen;
import ipn.escom.defensoria.revision_service.entity.Queja;
import ipn.escom.defensoria.revision_service.entity.QuejaEvidencia;
import ipn.escom.defensoria.revision_service.model.AntecedenteModel;
import ipn.escom.defensoria.revision_service.model.BandejaResumenModel;
import ipn.escom.defensoria.revision_service.model.HistorialItemModel;
import ipn.escom.defensoria.revision_service.model.QuejaDetalleModel;
import ipn.escom.defensoria.revision_service.model.QuejaResumenBandejaModel;
import ipn.escom.defensoria.revision_service.repository.QuejaEvidenciaRepository;
import ipn.escom.defensoria.revision_service.repository.QuejaRepository;

@Service
public class RevisionQuejaService {

    // Estatus del flujo de revisión -- ver comentario en la entidad Queja.
    public static final String RECIBIDA = "RECIBIDA";
    public static final String EN_VALIDACION = "EN_VALIDACION";
    public static final String RECHAZADA = "RECHAZADA";
    public static final String TURNADA = "TURNADA";

    private static final String ORIGEN_MANUAL = "MANUAL";

    @Autowired
    private QuejaRepository quejaRepository;

    @Autowired
    private QuejaEvidenciaRepository evidenciaRepository;

    @Autowired
    private NotificacionQuejaService notificacionService;

    // ---------------- Bandeja de Entrada ----------------

    public BandejaResumenModel bandeja() {
        long pendientes = quejaRepository.countByEstatus(RECIBIDA);
        long enProceso = quejaRepository.countByEstatus(EN_VALIDACION);

        LocalDateTime inicioHoy = LocalDate.now().atStartOfDay();
        LocalDateTime finHoy = LocalDate.now().atTime(LocalTime.MAX);
        long turnadasHoy = quejaRepository.countByEstatusAndFechaTurnadoBetween(TURNADA, inicioHoy, finHoy);

        List<QuejaResumenBandejaModel> lista = quejaRepository
                .findByEstatusInOrderByFechaCreacionAsc(List.of(RECIBIDA, EN_VALIDACION))
                .stream()
                .map(q -> new QuejaResumenBandejaModel(
                        q.getNumeroFolio(),
                        q.getFechaCreacion(),
                        nombreMostrar(q),
                        documentacionAparenteCompleta(q),
                        q.getEstatus()))
                .toList();

        return new BandejaResumenModel(pendientes, enProceso, turnadasHoy, lista);
    }

    // ---------------- Validación de Requisitos ----------------

    /** Al abrir el detalle, si la queja seguía "RECIBIDA" pasa a "EN_VALIDACION" -- así la
     * bandeja refleja que alguien ya la está trabajando (contador "En Proceso"). */
    public QuejaDetalleModel detalle(String folio) {
        Queja queja = obtenerPorFolio(folio);
        if (RECIBIDA.equals(queja.getEstatus())) {
            queja.setEstatus(EN_VALIDACION);
            quejaRepository.save(queja);
        }
        return aDetalle(queja);
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
        Queja guardada = quejaRepository.save(queja);

        notificacionService.enviarCorreoRechazo(
                guardada.getCorreoInstitucional(), nombreMostrar(guardada), guardada.getNumeroFolio(), textoCompleto);

        return guardada;
    }

    // ---------------- Turnado ----------------

    public Queja turnar(String folio, String areaTurnada, String defensorAsignado, String comentarios,
            String correoRecepcionista) {
        if (esVacio(areaTurnada) || esVacio(defensorAsignado)) {
            throw new RuntimeException("Selecciona el área y el defensor responsable antes de turnar.");
        }

        Queja queja = obtenerPorFolio(folio);
        queja.setEstatus(TURNADA);
        queja.setAreaTurnada(areaTurnada);
        queja.setDefensorAsignado(defensorAsignado);
        queja.setComentariosRecepcion(comentarios);
        queja.setValidadoPor(correoRecepcionista);
        queja.setFechaValidacion(LocalDateTime.now());
        queja.setFechaTurnado(LocalDateTime.now());
        return quejaRepository.save(queja);
    }

    // ---------------- Registro Manual ----------------

    public Queja registrarManual(
            String nombre, String apellidoPaterno, String apellidoMaterno,
            String tipoUsuario, String dependenciaClave, String numeroOficio,
            LocalDate fechaRecepcionFisica, String tipoDocumento,
            String descripcion, String ubicacionFisica,
            MultipartFile archivo, String correoRecepcionista) {

        if (esVacio(nombre) || esVacio(apellidoPaterno) || esVacio(descripcion)) {
            throw new RuntimeException("Completa al menos nombre, primer apellido y descripción del asunto.");
        }

        Queja queja = new Queja();
        queja.setNumeroFolio(generarFolio());
        // Un documento físico no siempre trae un correo institucional capturable de inmediato;
        // se usa un valor de referencia interno para no romper el NOT NULL de la columna
        // compartida con queja-service. El recepcionista puede editarlo después si lo obtiene.
        queja.setCorreoInstitucional("registro-manual+" + queja.getNumeroFolio().toLowerCase() + "@defensoria.ipn.mx");
        queja.setMotivo("Registro manual: " + tipoDocumento);
        queja.setDescripcion(descripcion);
        queja.setNombreQuejoso(nombre);
        queja.setApellidoPaternoQuejoso(apellidoPaterno);
        queja.setApellidoMaternoQuejoso(apellidoMaterno);
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

    public List<HistorialItemModel> historial(String texto, String estatus, LocalDate fecha) {
        List<Queja> procesadas = quejaRepository.findByEstatusInOrderByFechaCreacionDesc(List.of(RECHAZADA, TURNADA));

        return procesadas.stream()
                .filter(q -> estatus == null || estatus.isBlank() || etiquetaEstatusFinal(q.getEstatus()).equalsIgnoreCase(estatus))
                .filter(q -> fecha == null || q.getFechaCreacion().toLocalDate().equals(fecha))
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
                q.getTipoIdentificacionQuejoso(), q.getNumeroIdentificacionQuejoso(),
                q.getMotivo(), q.getDescripcion(), q.getUnidadAcademicaClave(), q.getFechaHechos(),
                nombreDenunciadoCompleto(q), q.getOrigenRegistro(), q.getEstatus(), evidencias,
                q.getMotivoRechazo(), q.getAreaTurnada(), q.getDefensorAsignado(), q.getComentariosRecepcion());
    }

    private String nombreMostrar(Queja q) {
        String nombre = concatenarNoVacios(q.getNombreQuejoso(), q.getApellidoPaternoQuejoso(), q.getApellidoMaternoQuejoso());
        return nombre.isBlank() ? q.getCorreoInstitucional() : nombre;
    }

    private String nombreDenunciadoCompleto(Queja q) {
        String nombre = concatenarNoVacios(q.getNombreDenunciado(), q.getApellidoDenunciado());
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
        return "FOL-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
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
