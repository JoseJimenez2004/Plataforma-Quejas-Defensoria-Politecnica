package ipn.escom.defensoria.primercontacto.service;

import ipn.escom.defensoria.primercontacto.dto.BandejaAnalisisDTO;
import ipn.escom.defensoria.primercontacto.dto.EvidenciaDTO;
import ipn.escom.defensoria.primercontacto.dto.ExpedienteAnalisisDTO;
import ipn.escom.defensoria.primercontacto.dto.QuejosoDTO;
import ipn.escom.defensoria.primercontacto.exception.RecursoNoEncontradoException;
import ipn.escom.defensoria.primercontacto.repository.CitaPrimerContactoRepository;
import ipn.escom.defensoria.primercontacto.store.QuejaEnMemoriaStore;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Puente entre el flujo de análisis de Primer Contacto y las quejas
 * que llegan desde Subdefensoría.
 *
 * Antes este servicio devolvía datos fijos (mock) o llamaba por
 * RestTemplate a una "PlataformaCentral". Ahora las quejas ya no se
 * piden activamente: Subdefensoría las empuja al
 * IngestaSubdefensoriaController y quedan disponibles aquí a través
 * de QuejaEnMemoriaStore. No se persisten en BD porque la fuente de
 * verdad de la queja es Subdefensoría; este servicio solo la
 * "escucha" mientras está corriendo.
 */
@Service
public class PlataformaCentralClientService {

    private final QuejaEnMemoriaStore quejaStore;
    private final CitaPrimerContactoRepository citaRepository;

    public PlataformaCentralClientService(
            QuejaEnMemoriaStore quejaStore,
            CitaPrimerContactoRepository citaRepository
    ) {
        this.quejaStore = quejaStore;
        this.citaRepository = citaRepository;
    }

    public List<BandejaAnalisisDTO> obtenerBandejaAnalisis(String token) {
        return quejaStore.listarTodas().stream()
                .map(this::aBandejaDTO)
                .toList();
    }

    private BandejaAnalisisDTO aBandejaDTO(ExpedienteAnalisisDTO expediente) {
        boolean tieneCita = citaRepository.existsByFolioAndEstatusNot(
                expediente.getFolio(), "CANCELADA");

        QuejosoDTO quejoso = expediente.getQuejoso();

        return BandejaAnalisisDTO.builder()
                .quejaId(expediente.getQuejaId())
                .folio(expediente.getFolio())
                .nombreQuejoso(quejoso != null ? quejoso.getNombreCompleto() : null)
                .unidadAcademica(quejoso != null ? quejoso.getUnidadAcademica() : null)
                .tema(expediente.getTema())
                .prioridad(expediente.getPrioridad())
                .estatus(tieneCita ? "CON_CITA" : expediente.getEstatus())
                .fechaRecepcion(expediente.getFechaRecepcion())
                .build();
    }

    public ExpedienteAnalisisDTO obtenerExpediente(Long quejaId, String token) {
        ExpedienteAnalisisDTO expediente = quejaStore.obtenerPorQuejaId(quejaId);

        if (expediente == null) {
            throw new RecursoNoEncontradoException(
                    "No se ha recibido de Subdefensoría ninguna queja con id " + quejaId);
        }

        return expediente;
    }

    public ExpedienteAnalisisDTO obtenerExpedientePorFolio(String folio, String token) {
        ExpedienteAnalisisDTO expediente = quejaStore.obtenerPorFolio(folio);

        if (expediente == null) {
            throw new RecursoNoEncontradoException(
                    "No se ha recibido de Subdefensoría ninguna queja con folio " + folio);
        }

        return expediente;
    }

    public QuejosoDTO obtenerQuejoso(Long quejosoId, String token) {
        return quejaStore.listarTodas().stream()
                .map(ExpedienteAnalisisDTO::getQuejoso)
                .filter(q -> q != null && quejosoId.equals(q.getId()))
                .findFirst()
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No se encontró un quejoso con id " + quejosoId));
    }

    public List<EvidenciaDTO> obtenerEvidencias(Long quejaId, String token) {
        ExpedienteAnalisisDTO expediente = obtenerExpediente(quejaId, token);
        return expediente.getEvidencias() != null ? expediente.getEvidencias() : List.of();
    }

    /**
     * Refleja hacia el store en memoria el cambio de estatus que
     * decide el analista (competencia, improcedencia, remisión...).
     * Como ya no hay una PlataformaCentral externa a la que avisarle,
     * este es el punto único de verdad para el estatus de la queja
     * mientras el servicio está arriba.
     */
    public void actualizarEstatusQueja(Long quejaId, String nuevoEstatus, String token) {
        quejaStore.actualizarEstatus(quejaId, nuevoEstatus);
    }

    public void enviarNotificacion(Long usuarioId, String asunto, String mensaje, String token) {
        System.out.println("MOCK enviar notificación usuarioId=" + usuarioId);
    }
}
