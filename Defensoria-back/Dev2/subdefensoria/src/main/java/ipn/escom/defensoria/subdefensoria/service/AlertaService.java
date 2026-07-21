package ipn.escom.defensoria.subdefensoria.service;

import ipn.escom.defensoria.subdefensoria.dto.AlertaVencimientoDTO;
import ipn.escom.defensoria.subdefensoria.entity.EstatusOficio;
import ipn.escom.defensoria.subdefensoria.entity.OficioInformacion;
import ipn.escom.defensoria.subdefensoria.repository.OficioInformacionRepository;
import ipn.escom.defensoria.subdefensoria.util.DiasHabilesCalculator;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

/**
 * Alimenta la pantalla P17 "Panel de Control de Plazos Vencidos".
 * Depende de que ControlPlazoService (u otra lectura del semaforo)
 * ya haya madurado los oficios vencidos; aqui simplemente se listan
 * los que ya quedaron en estatus VENCIDO.
 */
@Service
public class AlertaService {

    private final OficioInformacionRepository oficioRepository;

    public AlertaService(OficioInformacionRepository oficioRepository) {
        this.oficioRepository = oficioRepository;
    }

    public List<AlertaVencimientoDTO> obtenerVencidos() {
        madurarPendientes();
        return oficioRepository.findByEstatusOrderByFechaLimiteAsc(EstatusOficio.VENCIDO).stream()
                .map(this::convertirADTO)
                .toList();
    }

    /**
     * No depende de que alguien haya abierto antes la pantalla del
     * semaforo (ControlPlazoService): aqui tambien se revisan los
     * oficios EN_ESPERA cuya fecha limite ya paso y se marcan
     * VENCIDO, para que este panel sea confiable por si solo.
     */
    private void madurarPendientes() {
        LocalDate hoy = LocalDate.now();
        List<OficioInformacion> enEspera = oficioRepository.findByEstatusOrderByFechaLimiteAsc(EstatusOficio.EN_ESPERA);
        for (OficioInformacion o : enEspera) {
            if (o.getFechaLimite().isBefore(hoy)) {
                o.setEstatus(EstatusOficio.VENCIDO);
                oficioRepository.save(o);
            }
        }
    }

    private AlertaVencimientoDTO convertirADTO(OficioInformacion o) {
        long diasRetraso = DiasHabilesCalculator.diasHabilesTranscurridos(o.getFechaLimite(), LocalDate.now());
        return AlertaVencimientoDTO.builder()
                .oficioId(o.getId())
                .numeroOficio(o.getNumeroOficio())
                .folio(o.getFolio())
                .unidadAcademica(o.getUnidadAcademica())
                .fase(o.getFase())
                .fechaLimite(o.getFechaLimite().toString())
                .diasRetraso(diasRetraso)
                .build();
    }
}
