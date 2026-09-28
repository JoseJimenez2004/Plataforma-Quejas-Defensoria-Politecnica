package ipn.escom.defensoria.primercontacto.service;

import ipn.escom.defensoria.primercontacto.entity.ExpedientePrimerContacto;
import ipn.escom.defensoria.primercontacto.entity.QuejaReferencia;
import ipn.escom.defensoria.primercontacto.exception.RecursoNoEncontradoException;
import ipn.escom.defensoria.primercontacto.repository.ExpedientePrimerContactoRepository;
import ipn.escom.defensoria.primercontacto.repository.QuejaReferenciaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * Único lugar donde cambia el estatus de un expediente de Primer Contacto (ver
 * EstatusExpediente para el diagrama).
 *
 * Además refleja el cambio en quejas.estatus, que es lo que ven los demás módulos, cuando
 * primer-contacto.sincronizar-estatus-queja=true. Viene APAGADO por defecto: el panel del
 * quejoso (Frontend) todavía solo reconoce RECIBIDA/EN_VALIDACION/TURNADA/RECHAZADA/
 * CANCELADA, así que un EN_ANALISIS le aparecería como "Recibida". Se enciende cuando ese
 * front tenga las etiquetas nuevas. Mientras tanto el quejoso se entera por el centro de
 * notificaciones (NotificacionQuejosoService).
 */
@Service
public class TransicionExpedienteService {

    private static final Logger log = LoggerFactory.getLogger(TransicionExpedienteService.class);

    private final ExpedientePrimerContactoRepository expedienteRepository;
    private final QuejaReferenciaRepository quejaRepository;
    private final boolean sincronizarQueja;

    public TransicionExpedienteService(
            ExpedientePrimerContactoRepository expedienteRepository,
            QuejaReferenciaRepository quejaRepository,
            @Value("${primer-contacto.sincronizar-estatus-queja:false}")
            boolean sincronizarQueja
    ) {
        this.expedienteRepository = expedienteRepository;
        this.quejaRepository = quejaRepository;
        this.sincronizarQueja = sincronizarQueja;
    }

    public ExpedientePrimerContacto obtenerPorFolio(String folio) {
        return expedienteRepository.findByFolio(folio)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No existe un expediente de Primer Contacto con folio " + folio
                ));
    }

    public ExpedientePrimerContacto cambiarEstatus(
            ExpedientePrimerContacto expediente,
            String nuevoEstatus
    ) {
        expediente.setEstatus(nuevoEstatus);
        expediente.setFechaActualizacion(LocalDateTime.now());

        ExpedientePrimerContacto guardado = expedienteRepository.save(expediente);

        sincronizarQueja(guardado);

        return guardado;
    }

    private void sincronizarQueja(ExpedientePrimerContacto expediente) {
        if (!sincronizarQueja) {
            return;
        }

        try {
            QuejaReferencia queja = quejaRepository
                    .findByNumeroFolio(expediente.getFolioOrigen())
                    .orElse(null);

            if (queja == null) {
                log.warn("No existe la queja {} para sincronizar el estatus {}",
                        expediente.getFolioOrigen(), expediente.getEstatus());
                return;
            }

            queja.setEstatus(expediente.getEstatus());
            quejaRepository.save(queja);

        } catch (Exception ex) {
            // El expediente ya avanzó; que la tabla de otro módulo falle no debe revertirlo.
            log.error("No se pudo sincronizar quejas.estatus de {}: {}",
                    expediente.getFolioOrigen(), ex.getMessage());
        }
    }
}
