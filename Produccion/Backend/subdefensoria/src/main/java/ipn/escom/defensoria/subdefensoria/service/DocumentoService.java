package ipn.escom.defensoria.subdefensoria.service;

import ipn.escom.defensoria.subdefensoria.entity.AcuerdoConclusion;
import ipn.escom.defensoria.subdefensoria.entity.ExpedienteInvestigacion;
import ipn.escom.defensoria.subdefensoria.entity.OficioInformacion;
import ipn.escom.defensoria.subdefensoria.entity.RecordatorioUrgencia;
import ipn.escom.defensoria.subdefensoria.exception.OperacionInvalidaException;
import ipn.escom.defensoria.subdefensoria.exception.RecursoNoEncontradoException;
import ipn.escom.defensoria.subdefensoria.repository.AcuerdoConclusionRepository;
import ipn.escom.defensoria.subdefensoria.repository.ExpedienteInvestigacionRepository;
import ipn.escom.defensoria.subdefensoria.repository.OficioInformacionRepository;
import ipn.escom.defensoria.subdefensoria.repository.RecordatorioUrgenciaRepository;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;

/**
 * Arma los PDF de Subdefensoría a partir de lo guardado (no se almacenan: se generan al
 * descargarlos, así siempre reflejan los datos vigentes). Ver DocumentoPdfService.
 */
@Service
public class DocumentoService {

    public record Pdf(String nombreArchivo, byte[] contenido) {
    }

    private final OficioInformacionRepository oficioRepository;
    private final RecordatorioUrgenciaRepository recordatorioRepository;
    private final AcuerdoConclusionRepository acuerdoRepository;
    private final ExpedienteInvestigacionRepository expedienteRepository;
    private final DocumentoPdfService pdfService;

    public DocumentoService(
            OficioInformacionRepository oficioRepository,
            RecordatorioUrgenciaRepository recordatorioRepository,
            AcuerdoConclusionRepository acuerdoRepository,
            ExpedienteInvestigacionRepository expedienteRepository,
            DocumentoPdfService pdfService
    ) {
        this.oficioRepository = oficioRepository;
        this.recordatorioRepository = recordatorioRepository;
        this.acuerdoRepository = acuerdoRepository;
        this.expedienteRepository = expedienteRepository;
        this.pdfService = pdfService;
    }

    public Pdf oficio(Long oficioId) {
        OficioInformacion oficio = oficioRepository.findById(oficioId)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe el oficio " + oficioId));
        return new Pdf(archivo("oficio", oficio.getNumeroOficio()),
                pdfService.oficio(oficio, expediente(oficio.getExpedienteId())));
    }

    public Pdf recordatorio(Long recordatorioId) {
        RecordatorioUrgencia recordatorio = recordatorioRepository.findById(recordatorioId)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe el recordatorio " + recordatorioId));
        OficioInformacion oficio = oficioRepository.findById(recordatorio.getOficioId())
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe el oficio " + recordatorio.getOficioId()));

        // Número consecutivo del recordatorio dentro de su oficio (1, 2, ...).
        List<RecordatorioUrgencia> delOficio = recordatorioRepository.findByOficioIdOrderByFechaEnvioDesc(oficio.getId())
                .stream().sorted(Comparator.comparing(RecordatorioUrgencia::getId)).toList();
        int numero = (int) delOficio.stream().takeWhile(r -> !r.getId().equals(recordatorioId)).count() + 1;

        return new Pdf(archivo("recordatorio", oficio.getNumeroOficio() + "-R" + numero),
                pdfService.recordatorio(recordatorio, numero, oficio, expediente(oficio.getExpedienteId())));
    }

    public Pdf acuerdoConciliacion(Long expedienteId) {
        AcuerdoConclusion acuerdo = acuerdo(expedienteId);
        if (acuerdo.getFechaEnvioSecretarial() == null) {
            throw new OperacionInvalidaException("El acuerdo de conciliación todavía es un borrador.");
        }
        ExpedienteInvestigacion expediente = expediente(expedienteId);
        return new Pdf(archivo("acuerdo-conciliacion", expediente.getFolio()),
                pdfService.acuerdoConciliacion(acuerdo, expediente));
    }

    public Pdf acuerdoConclusion(Long expedienteId) {
        AcuerdoConclusion acuerdo = acuerdo(expedienteId);
        if (!Boolean.TRUE.equals(acuerdo.getConcluido())) {
            throw new OperacionInvalidaException("El expediente todavía no tiene acuerdo de conclusión.");
        }
        ExpedienteInvestigacion expediente = expediente(expedienteId);
        return new Pdf(archivo("acuerdo-conclusion", expediente.getFolio()),
                pdfService.acuerdoConclusion(acuerdo, expediente));
    }

    /** Para el enlace del quejoso: el acuerdo del token (conciliación o conclusión). */
    public Pdf acuerdoPorToken(String token, boolean conclusion) {
        AcuerdoConclusion acuerdo = acuerdoRepository.findByTokenRespuesta(token == null ? "" : token.strip())
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "El enlace del acuerdo no es válido o ya fue sustituido por uno más reciente."));
        return conclusion ? acuerdoConclusion(acuerdo.getExpedienteId()) : acuerdoConciliacion(acuerdo.getExpedienteId());
    }

    private AcuerdoConclusion acuerdo(Long expedienteId) {
        return acuerdoRepository.findByExpedienteId(expedienteId)
                .orElseThrow(() -> new RecursoNoEncontradoException("El expediente no tiene acuerdo."));
    }

    private ExpedienteInvestigacion expediente(Long expedienteId) {
        return expedienteRepository.findById(expedienteId)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe el expediente " + expedienteId));
    }

    private static String archivo(String tipo, String identificador) {
        return tipo + "-" + identificador.replaceAll("[^A-Za-z0-9-]", "_") + ".pdf";
    }
}
