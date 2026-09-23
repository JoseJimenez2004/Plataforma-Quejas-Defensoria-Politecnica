package ipn.escom.defensoria.subdefensoria.service;

import ipn.escom.defensoria.subdefensoria.dto.BandejaNuevaDTO;
import ipn.escom.defensoria.subdefensoria.entity.EstatusExpediente;
import ipn.escom.defensoria.subdefensoria.entity.EstatusOficio;
import ipn.escom.defensoria.subdefensoria.entity.ExpedienteInvestigacion;
import ipn.escom.defensoria.subdefensoria.entity.FaseOficio;
import ipn.escom.defensoria.subdefensoria.repository.ExpedienteInvestigacionRepository;
import ipn.escom.defensoria.subdefensoria.repository.OficioInformacionRepository;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Pantalla P14.A "Quejas Nuevas": expedientes a los que hay que
 * redactarles un oficio ahora mismo. Cubre dos casos: RECIBIDO (toca
 * TS-01, primer oficio) y EN_GESTION_DIRECTOR sin oficio de esa fase
 * todavia activo (toca TS-04, oficio al director).
 */
@Service
public class BandejaInvestigacionService {

    private final ExpedienteInvestigacionRepository expedienteRepository;
    private final OficioInformacionRepository oficioRepository;

    public BandejaInvestigacionService(
            ExpedienteInvestigacionRepository expedienteRepository,
            OficioInformacionRepository oficioRepository
    ) {
        this.expedienteRepository = expedienteRepository;
        this.oficioRepository = oficioRepository;
    }

    public List<BandejaNuevaDTO> obtenerQuejasNuevas() {
        List<BandejaNuevaDTO> resultado = new java.util.ArrayList<>();

        expedienteRepository.findByEstatusOrderByFechaAdmisionAsc(EstatusExpediente.RECIBIDO)
                .forEach(e -> resultado.add(convertirADTO(e, FaseOficio.SOLICITUD_INFORMACION)));

        expedienteRepository.findByEstatusOrderByFechaAdmisionAsc(EstatusExpediente.EN_GESTION_DIRECTOR).stream()
                .filter(e -> !oficioRepository.existsByExpedienteIdAndEstatus(e.getId(), EstatusOficio.EN_ESPERA)
                        && !oficioRepository.existsByExpedienteIdAndEstatus(e.getId(), EstatusOficio.VENCIDO))
                .forEach(e -> resultado.add(convertirADTO(e, FaseOficio.GESTION_DIRECTOR)));

        return resultado;
    }

    private BandejaNuevaDTO convertirADTO(ExpedienteInvestigacion e, String siguienteFase) {
        return BandejaNuevaDTO.builder()
                .expedienteId(e.getId())
                .folio(e.getFolio())
                .fechaAdmision(e.getFechaAdmision() != null ? e.getFechaAdmision().toString() : null)
                .quejosoNombre(e.getQuejosoNombre())
                .asunto(e.getAsunto())
                .unidadAcademica(e.getUnidadAcademica())
                .siguienteFase(siguienteFase)
                .build();
    }
}
