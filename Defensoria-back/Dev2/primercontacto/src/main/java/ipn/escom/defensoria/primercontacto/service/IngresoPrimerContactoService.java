package ipn.escom.defensoria.primercontacto.service;

import ipn.escom.defensoria.primercontacto.dto.ExpedienteTurnadoRequest;
import ipn.escom.defensoria.primercontacto.entity.ExpedientePrimerContacto;
import ipn.escom.defensoria.primercontacto.repository.ExpedientePrimerContactoRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class IngresoPrimerContactoService {

    private final ExpedientePrimerContactoRepository expedienteRepository;

    public IngresoPrimerContactoService(
            ExpedientePrimerContactoRepository expedienteRepository
    ) {
        this.expedienteRepository = expedienteRepository;
    }

    public ExpedientePrimerContacto recibir(
            ExpedienteTurnadoRequest request
    ) {

        /*
         * Evita que la misma queja sea ingresada dos veces
         * a Primer Contacto.
         */
        return expedienteRepository
                .findByFolioOrigen(request.getFolioOrigen())
                .orElseGet(() -> crearNuevoExpediente(request));
    }

    private ExpedientePrimerContacto crearNuevoExpediente(
            ExpedienteTurnadoRequest request
    ) {

        LocalDateTime ahora = LocalDateTime.now();

        ExpedientePrimerContacto expediente =
                ExpedientePrimerContacto.builder()
                        .folio(generarFolio())
                        .folioOrigen(request.getFolioOrigen())
                        .estatus("PENDIENTE_ANALISIS")
                        .fechaCreacion(ahora)
                        .fechaActualizacion(ahora)
                        .build();

        return expedienteRepository.save(expediente);
    }

    private String generarFolio() {

        return "PC-"
                + UUID.randomUUID()
                .toString()
                .substring(0, 8)
                .toUpperCase();
    }
}