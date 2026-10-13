package ipn.escom.defensoria.subdefensoria.service;

import ipn.escom.defensoria.subdefensoria.entity.EstatusExpediente;
import ipn.escom.defensoria.subdefensoria.entity.ExpedienteInvestigacion;
import ipn.escom.defensoria.subdefensoria.entity.QuejaReferencia;
import ipn.escom.defensoria.subdefensoria.repository.ExpedienteInvestigacionRepository;
import ipn.escom.defensoria.subdefensoria.repository.QuejaReferenciaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * Único lugar donde cambia el estatus de un expediente de Subdefensoría (ver
 * EstatusExpediente para el diagrama).
 *
 * Además refleja el cambio en quejas.estatus (lo que ve el quejoso en su panel) cuando
 * subdefensoria.sincronizar-estatus-queja=true, igual que Primer Contacto. Si la queja no
 * existe o la tabla falla, el expediente avanza de todos modos y solo queda en el log.
 */
@Service
public class TransicionExpedienteService {

    private static final Logger log = LoggerFactory.getLogger(TransicionExpedienteService.class);

    private final ExpedienteInvestigacionRepository expedienteRepository;
    private final QuejaReferenciaRepository quejaRepository;
    private final boolean sincronizarQueja;

    public TransicionExpedienteService(
            ExpedienteInvestigacionRepository expedienteRepository,
            QuejaReferenciaRepository quejaRepository,
            @Value("${subdefensoria.sincronizar-estatus-queja:true}") boolean sincronizarQueja
    ) {
        this.expedienteRepository = expedienteRepository;
        this.quejaRepository = quejaRepository;
        this.sincronizarQueja = sincronizarQueja;
    }

    public ExpedienteInvestigacion cambiar(ExpedienteInvestigacion expediente, String nuevoEstatus) {
        expediente.setEstatus(nuevoEstatus);
        expediente.setFechaActualizacion(LocalDateTime.now());
        ExpedienteInvestigacion guardado = expedienteRepository.save(expediente);
        sincronizarQueja(guardado);
        return guardado;
    }

    /** Para el expediente recién creado (ingesta), que ya nace con su estatus. */
    public void sincronizarQueja(ExpedienteInvestigacion expediente) {
        if (!sincronizarQueja || expediente.getFolioQueja() == null) {
            return;
        }
        try {
            QuejaReferencia queja = quejaRepository.findByNumeroFolio(expediente.getFolioQueja()).orElse(null);
            if (queja == null) {
                log.warn("No existe la queja {} para sincronizar el estatus {}",
                        expediente.getFolioQueja(), expediente.getEstatus());
                return;
            }
            queja.setEstatus(EstatusExpediente.estatusQueja(expediente.getEstatus()));
            quejaRepository.save(queja);
        } catch (Exception ex) {
            log.error("No se pudo sincronizar quejas.estatus de {}: {}",
                    expediente.getFolioQueja(), ex.getMessage());
        }
    }
}
