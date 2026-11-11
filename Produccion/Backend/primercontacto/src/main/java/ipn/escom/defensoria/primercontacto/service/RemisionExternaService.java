package ipn.escom.defensoria.primercontacto.service;

import ipn.escom.defensoria.primercontacto.dto.CrearRemisionDTO;
import ipn.escom.defensoria.primercontacto.dto.InstanciaRemisionDTO;
import ipn.escom.defensoria.primercontacto.dto.RemisionDTO;
import ipn.escom.defensoria.primercontacto.entity.DictamenPrimerContacto;
import ipn.escom.defensoria.primercontacto.entity.EstatusExpediente;
import ipn.escom.defensoria.primercontacto.entity.ExpedientePrimerContacto;
import ipn.escom.defensoria.primercontacto.entity.InstanciaRemision;
import ipn.escom.defensoria.primercontacto.entity.PersonalAdministrativo;
import ipn.escom.defensoria.primercontacto.entity.RemisionExterna;
import ipn.escom.defensoria.primercontacto.exception.OperacionInvalidaException;
import ipn.escom.defensoria.primercontacto.exception.RecursoNoEncontradoException;
import ipn.escom.defensoria.primercontacto.repository.DictamenPrimerContactoRepository;
import ipn.escom.defensoria.primercontacto.repository.EvidenciaPrimerContactoRepository;
import ipn.escom.defensoria.primercontacto.repository.InstanciaRemisionRepository;
import ipn.escom.defensoria.primercontacto.repository.RemisionExternaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Remisión externa (CU-PC-09). Solo aplica a expedientes IMPROCEDENTES.
 *
 * Desde 2026-11-10 se genera y se envía en una sola acción del analista: elige la instancia
 * del catálogo (OAG, OIC, COSECOVI / Género; el correo lo administra la Defensora), se genera
 * el oficio en PDF y se manda adjunto a ese correo. Internamente son tres pasos separados,
 * porque un correo que ya salió no se puede deshacer con un rollback:
 *
 *   1. (transacción) se guarda la remisión GENERADA con su número de oficio;
 *   2. (sin transacción) se manda el correo con el PDF;
 *   3. (transacción) remisión ENVIADA + expediente REMITIDA; luego el aviso al quejoso.
 *
 * Si falla el correo, la remisión queda GENERADA y se reintenta con
 * {@link #enviarRemision(String, String)} (botón "Enviar oficio"). Si fallara el paso 3 con el
 * correo ya enviado, lo peor es que al reintentar la instancia reciba el mismo oficio dos
 * veces, nunca un oficio enviado sin registro.
 */
@Service
public class RemisionExternaService {

    private final RemisionExternaRepository remisionExternaRepository;
    private final DictamenPrimerContactoRepository dictamenRepository;
    private final EvidenciaPrimerContactoRepository evidenciaRepository;
    private final InstanciaRemisionRepository instanciaRepository;
    private final TransicionExpedienteService transicionService;
    private final NotificacionQuejosoService notificacionService;
    private final OficioRemisionPdfService pdfService;
    private final TransactionTemplate tx;

    public RemisionExternaService(
            RemisionExternaRepository remisionExternaRepository,
            DictamenPrimerContactoRepository dictamenRepository,
            EvidenciaPrimerContactoRepository evidenciaRepository,
            InstanciaRemisionRepository instanciaRepository,
            TransicionExpedienteService transicionService,
            NotificacionQuejosoService notificacionService,
            OficioRemisionPdfService pdfService,
            PlatformTransactionManager transactionManager
    ) {
        this.tx = new TransactionTemplate(transactionManager);
        this.remisionExternaRepository = remisionExternaRepository;
        this.dictamenRepository = dictamenRepository;
        this.evidenciaRepository = evidenciaRepository;
        this.instanciaRepository = instanciaRepository;
        this.transicionService = transicionService;
        this.notificacionService = notificacionService;
        this.pdfService = pdfService;
    }

    /** Instancias del catálogo, en orden, con el correo al que saldrá el oficio. */
    public List<InstanciaRemisionDTO> listarInstancias() {
        return instanciaRepository.findAllByOrderByOrdenAsc().stream()
                .map(i -> new InstanciaRemisionDTO(i.getClave(), i.getSigla(), i.getNombre(), i.getCorreo()))
                .toList();
    }

    /** Genera el oficio y lo envía a la instancia elegida (pasos 1, 2 y 3). */
    public RemisionDTO crearRemision(
            CrearRemisionDTO dto,
            PersonalAdministrativo analista
    ) {
        InstanciaRemision instancia = instanciaConCorreo(dto.getInstanciaClave());
        RemisionExterna guardada = tx.execute(estado -> registrarGenerada(dto, analista, instancia));
        return convertirADTO(enviar(guardada, instancia));
    }

    /* Paso 1: la remisión queda GENERADA (con su número de oficio) antes de mandar nada. */
    private RemisionExterna registrarGenerada(
            CrearRemisionDTO dto,
            PersonalAdministrativo analista,
            InstanciaRemision instancia
    ) {

        ExpedientePrimerContacto expediente =
                transicionService.obtenerPorFolio(dto.getFolio());

        if (!EstatusExpediente.esImprocedente(expediente.getEstatus())) {
            throw new OperacionInvalidaException(
                    "Solo se remite un expediente dictaminado como improcedente "
                            + "(estatus actual: " + expediente.getEstatus() + ")."
            );
        }

        /*
         * Un expediente solamente puede tener
         * una remisión externa.
         */
        if (remisionExternaRepository.existsByExpedienteId(expediente.getId())) {
            throw new OperacionInvalidaException(
                    "El expediente ya cuenta con una remisión registrada."
            );
        }

        LocalDateTime ahora = LocalDateTime.now();

        RemisionExterna remision =
                RemisionExterna.builder()
                        .expedienteId(expediente.getId())
                        .folio(expediente.getFolio())
                        .analistaId(analista.getId())
                        .analistaNombre(analista.getNombreCompleto())
                        .autoridadRemision(instancia.getNombre())
                        .instanciaClave(instancia.getClave())
                        .justificacionLegal(dto.getJustificacionLegal())
                        .sugerenciaQuejoso(dto.getSugerenciaQuejoso())
                        .adjuntarExpediente(dto.getAdjuntarExpediente())
                        .fechaRemision(ahora)
                        .estatus(RemisionExterna.ESTATUS_GENERADA)
                        .build();

        RemisionExterna guardada = remisionExternaRepository.save(remision);

        guardada.setNumeroOficio(
                String.format("DDP/PC/REM/%d/%04d", ahora.getYear(), guardada.getId())
        );

        return remisionExternaRepository.save(guardada);
    }

    public RemisionDTO obtenerPorExpediente(
            Long expedienteId
    ) {

        return remisionExternaRepository
                .findByExpedienteId(expedienteId)
                .map(this::convertirADTO)
                .orElseThrow(() ->
                        new RecursoNoEncontradoException("Remisión no encontrada")
                );
    }

    public RemisionDTO obtenerPorFolio(
            String folio
    ) {

        return convertirADTO(buscarPorFolio(folio));
    }

    /**
     * Envía (o reintenta) una remisión GENERADA: la que quedó del flujo anterior o una cuyo
     * correo falló. Si no se indica instancia se usa la que ya tenga guardada.
     */
    public RemisionDTO enviarRemision(
            String folio,
            String instanciaClave
    ) {

        RemisionExterna remision = buscarPorFolio(folio);

        if (RemisionExterna.ESTATUS_ENVIADA.equals(estatusDe(remision))) {
            throw new OperacionInvalidaException(
                    "La remisión ya se había enviado."
            );
        }

        String clave = instanciaClave != null && !instanciaClave.isBlank()
                ? instanciaClave
                : remision.getInstanciaClave();

        if (clave == null || clave.isBlank()) {
            throw new OperacionInvalidaException("Elige la instancia a la que se enviará el oficio.");
        }

        InstanciaRemision instancia = instanciaConCorreo(clave);

        // Si cambió la instancia, se guarda antes de enviar para que el PDF y el registro coincidan.
        if (!instancia.getClave().equals(remision.getInstanciaClave())) {
            remision.setInstanciaClave(instancia.getClave());
            remision.setAutoridadRemision(instancia.getNombre());
            remision = remisionExternaRepository.save(remision);
        }

        return convertirADTO(enviar(remision, instancia));
    }

    /*
     * Paso 2 (correo, fuera de transacción) y paso 3 (transacción). Si el correo no sale se
     * lanza la excepción y la remisión se queda GENERADA para reintentar.
     */
    private RemisionExterna enviar(
            RemisionExterna remision,
            InstanciaRemision instancia
    ) {
        ExpedientePrimerContacto expediente =
                transicionService.obtenerPorFolio(remision.getFolio());

        OficioPdf oficio = generarPdf(remision, expediente);
        String correo = instancia.getCorreo().trim();

        boolean enviado = notificacionService.enviarCorreoConPdf(
                correo,
                "Defensoría de los Derechos Politécnicos — Oficio " + remision.getNumeroOficio()
                        + " (remisión de la queja " + expediente.getFolioOrigen() + ")",
                cuerpoCorreo(remision, expediente, instancia),
                oficio.nombreArchivo(),
                oficio.contenido()
        );

        if (!enviado) {
            throw new OperacionInvalidaException(
                    "No se pudo enviar el oficio " + remision.getNumeroOficio() + " a " + correo
                            + ". Quedó pendiente de enviar; usa «Enviar oficio» para reintentar."
            );
        }

        // Paso 3: se relee todo dentro de la transacción.
        Long remisionId = remision.getId();
        RemisionExterna guardada = tx.execute(estado -> {
            RemisionExterna actual = remisionExternaRepository.findById(remisionId)
                    .orElseThrow(() -> new RecursoNoEncontradoException("Remisión no encontrada"));
            ExpedientePrimerContacto exp = transicionService.obtenerPorFolio(actual.getFolio());

            actual.setCorreoDestino(correo);
            actual.setEstatus(RemisionExterna.ESTATUS_ENVIADA);
            actual.setFechaEnvio(LocalDateTime.now());
            RemisionExterna enviada = remisionExternaRepository.save(actual);

            // El estado viejo PENDIENTE_REMISION también termina en REMITIDA.
            transicionService.cambiarEstatus(exp, EstatusExpediente.REMITIDA);
            return enviada;
        });

        String orientacion =
                remision.getSugerenciaQuejoso() != null
                        && !remision.getSugerenciaQuejoso().isBlank()
                        ? "\n\nOrientación de Primer Contacto: " + remision.getSugerenciaQuejoso().trim()
                        : "";

        // Aviso al quejoso después de guardar (best-effort, no deshace nada si falla).
        notificacionService.notificar(
                expediente,
                NotificacionQuejosoService.TIPO_CAMBIO_ESTATUS,
                "Tu queja fue remitida a otra instancia",
                "Tu queja " + expediente.getFolioOrigen() + " fue remitida a "
                        + remision.getAutoridadRemision()
                        + " mediante el oficio " + remision.getNumeroOficio()
                        + ", por ser la instancia competente para atenderla." + orientacion
        );

        return guardada;
    }

    private String cuerpoCorreo(
            RemisionExterna remision,
            ExpedientePrimerContacto expediente,
            InstanciaRemision instancia
    ) {
        return instancia.getNombre() + "\nP R E S E N T E\n\n"
                + "Por este medio, la Defensoría de los Derechos Politécnicos remite a esa instancia la queja "
                + expediente.getFolioOrigen() + ", por ser la competente para atenderla.\n\n"
                + "Se adjunta el oficio " + remision.getNumeroOficio()
                + (Boolean.TRUE.equals(remision.getAdjuntarExpediente())
                        ? ", que incluye la copia del expediente y la lista de evidencias."
                        : ".")
                + "\n\nAtentamente,\n" + valor(remision.getAnalistaNombre(), "Primer Contacto")
                + "\nPrimer Contacto";
    }

    /**
     * Oficio de remisión en PDF. Se genera al vuelo con los datos guardados, así que siempre
     * coincide con lo enviado y no hay archivos que almacenar.
     */
    public OficioPdf generarPdf(String folio) {
        return generarPdf(buscarPorFolio(folio), transicionService.obtenerPorFolio(folio));
    }

    private OficioPdf generarPdf(RemisionExterna remision, ExpedientePrimerContacto expediente) {

        DictamenPrimerContacto dictamen =
                dictamenRepository.findByExpedienteId(expediente.getId()).orElse(null);

        byte[] contenido = pdfService.generar(
                remision,
                expediente,
                dictamen,
                evidenciaRepository.findByExpedienteId(expediente.getId())
        );

        String nombre = "oficio-remision-" + expediente.getFolioOrigen() + ".pdf";

        return new OficioPdf(nombre, contenido);
    }

    public record OficioPdf(String nombreArchivo, byte[] contenido) {
    }

    private InstanciaRemision instanciaConCorreo(String clave) {
        InstanciaRemision instancia = instanciaRepository.findById(clave == null ? "" : clave.trim())
                .orElseThrow(() -> new OperacionInvalidaException(
                        "La instancia elegida no existe en el catálogo."));
        if (instancia.getCorreo() == null || instancia.getCorreo().isBlank()) {
            throw new OperacionInvalidaException(
                    instancia.getSigla() + " no tiene correo registrado. Pide a la Defensora que lo agregue.");
        }
        return instancia;
    }

    private RemisionExterna buscarPorFolio(String folio) {
        return remisionExternaRepository
                .findByFolio(folio)
                .orElseThrow(() ->
                        new RecursoNoEncontradoException(
                                "El expediente " + folio + " no tiene remisión registrada."
                        )
                );
    }

    /*
     * Las remisiones creadas antes de que existiera la columna estatus
     * (null) salieron con el flujo anterior, que creaba y enviaba en el
     * mismo paso: se leen como ENVIADA.
     */
    private String estatusDe(RemisionExterna remision) {
        return remision.getEstatus() != null
                ? remision.getEstatus()
                : RemisionExterna.ESTATUS_ENVIADA;
    }

    private static String valor(String texto, String porDefecto) {
        return texto == null || texto.isBlank() ? porDefecto : texto;
    }

    private RemisionDTO convertirADTO(
            RemisionExterna remision
    ) {

        return RemisionDTO.builder()
                .id(remision.getId())
                .expedienteId(remision.getExpedienteId())
                .folio(remision.getFolio())
                .analistaId(remision.getAnalistaId())
                .analistaNombre(remision.getAnalistaNombre())
                .autoridadRemision(remision.getAutoridadRemision())
                .justificacionLegal(remision.getJustificacionLegal())
                .sugerenciaQuejoso(remision.getSugerenciaQuejoso())
                .adjuntarExpediente(remision.getAdjuntarExpediente())
                .fechaRemision(
                        remision.getFechaRemision() != null
                                ? remision.getFechaRemision().toString()
                                : null
                )
                .estatus(estatusDe(remision))
                .numeroOficio(remision.getNumeroOficio())
                .fechaEnvio(
                        remision.getFechaEnvio() != null
                                ? remision.getFechaEnvio().toString()
                                : null
                )
                .instanciaClave(remision.getInstanciaClave())
                .correoDestino(remision.getCorreoDestino())
                .build();
    }
}
