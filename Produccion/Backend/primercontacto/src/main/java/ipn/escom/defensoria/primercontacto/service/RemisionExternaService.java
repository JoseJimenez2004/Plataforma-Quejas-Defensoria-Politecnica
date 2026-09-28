package ipn.escom.defensoria.primercontacto.service;

import ipn.escom.defensoria.primercontacto.dto.CrearRemisionDTO;
import ipn.escom.defensoria.primercontacto.dto.RemisionDTO;
import ipn.escom.defensoria.primercontacto.entity.DictamenPrimerContacto;
import ipn.escom.defensoria.primercontacto.entity.EstatusExpediente;
import ipn.escom.defensoria.primercontacto.entity.ExpedientePrimerContacto;
import ipn.escom.defensoria.primercontacto.entity.PersonalAdministrativo;
import ipn.escom.defensoria.primercontacto.entity.RemisionExterna;
import ipn.escom.defensoria.primercontacto.exception.OperacionInvalidaException;
import ipn.escom.defensoria.primercontacto.exception.RecursoNoEncontradoException;
import ipn.escom.defensoria.primercontacto.repository.DictamenPrimerContactoRepository;
import ipn.escom.defensoria.primercontacto.repository.EvidenciaPrimerContactoRepository;
import ipn.escom.defensoria.primercontacto.repository.RemisionExternaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Remisión externa (CU-PC-09). Solo aplica a expedientes IMPROCEDENTES:
 *
 *   crear  -> la remisión queda GENERADA y ya se puede descargar su oficio en PDF;
 *             el expediente sigue IMPROCEDENTE.
 *   enviar -> el analista registra que el oficio se envió: remisión ENVIADA y expediente
 *             REMITIDA (cierre por remisión). Se avisa al quejoso.
 *
 * No hay envío electrónico a la instancia externa: el oficio se descarga y se entrega por
 * los medios oficiales; aquí solo se registra que se envió.
 */
@Service
public class RemisionExternaService {

    private final RemisionExternaRepository remisionExternaRepository;
    private final DictamenPrimerContactoRepository dictamenRepository;
    private final EvidenciaPrimerContactoRepository evidenciaRepository;
    private final TransicionExpedienteService transicionService;
    private final NotificacionQuejosoService notificacionService;
    private final OficioRemisionPdfService pdfService;

    public RemisionExternaService(
            RemisionExternaRepository remisionExternaRepository,
            DictamenPrimerContactoRepository dictamenRepository,
            EvidenciaPrimerContactoRepository evidenciaRepository,
            TransicionExpedienteService transicionService,
            NotificacionQuejosoService notificacionService,
            OficioRemisionPdfService pdfService
    ) {
        this.remisionExternaRepository = remisionExternaRepository;
        this.dictamenRepository = dictamenRepository;
        this.evidenciaRepository = evidenciaRepository;
        this.transicionService = transicionService;
        this.notificacionService = notificacionService;
        this.pdfService = pdfService;
    }

    @Transactional
    public RemisionDTO crearRemision(
            CrearRemisionDTO dto,
            PersonalAdministrativo analista
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
                        .autoridadRemision(dto.getAutoridadRemision())
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

        /*
         * Un expediente con el estado viejo PENDIENTE_REMISION se
         * normaliza a IMPROCEDENTE: la diferencia "redactada vs.
         * enviada" ahora vive en la remisión, no en el expediente.
         */
        if (EstatusExpediente.PENDIENTE_REMISION_LEGADO.equals(expediente.getEstatus())) {
            transicionService.cambiarEstatus(expediente, EstatusExpediente.IMPROCEDENTE);
        }

        return convertirADTO(remisionExternaRepository.save(guardada));
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

    @Transactional
    public RemisionDTO enviarRemision(
            String folio
    ) {

        RemisionExterna remision = buscarPorFolio(folio);

        if (RemisionExterna.ESTATUS_ENVIADA.equals(estatusDe(remision))) {
            throw new OperacionInvalidaException(
                    "La remisión ya se había registrado como enviada."
            );
        }

        ExpedientePrimerContacto expediente =
                transicionService.obtenerPorFolio(folio);

        remision.setEstatus(RemisionExterna.ESTATUS_ENVIADA);
        remision.setFechaEnvio(LocalDateTime.now());
        remisionExternaRepository.save(remision);

        transicionService.cambiarEstatus(
                expediente,
                EstatusExpediente.REMITIDA
        );

        String orientacion =
                remision.getSugerenciaQuejoso() != null
                        && !remision.getSugerenciaQuejoso().isBlank()
                        ? "\n\nOrientación de Primer Contacto: " + remision.getSugerenciaQuejoso().trim()
                        : "";

        notificacionService.notificar(
                expediente,
                NotificacionQuejosoService.TIPO_CAMBIO_ESTATUS,
                "Tu queja fue remitida a otra instancia",
                "Tu queja " + expediente.getFolioOrigen() + " fue remitida a "
                        + remision.getAutoridadRemision()
                        + " mediante el oficio " + remision.getNumeroOficio()
                        + ", por ser la instancia competente para atenderla." + orientacion
        );

        return convertirADTO(remision);
    }

    /**
     * Oficio de remisión en PDF. Se genera al vuelo con los datos guardados, así que siempre
     * coincide con lo registrado y no hay archivos que almacenar.
     */
    public OficioPdf generarPdf(String folio) {

        RemisionExterna remision = buscarPorFolio(folio);

        ExpedientePrimerContacto expediente =
                transicionService.obtenerPorFolio(folio);

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
                .build();
    }
}
