package ipn.escom.defensoria.primercontacto.service;

import ipn.escom.defensoria.primercontacto.dto.ExpedienteTurnadoRequest;
import ipn.escom.defensoria.primercontacto.entity.ExpedientePrimerContacto;
import ipn.escom.defensoria.primercontacto.repository.ExpedientePrimerContactoRepository;
import org.springframework.stereotype.Service;
import ipn.escom.defensoria.primercontacto.entity.EvidenciaPrimerContacto;
import ipn.escom.defensoria.primercontacto.repository.EvidenciaPrimerContactoRepository;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class IngresoPrimerContactoService {

    private final ExpedientePrimerContactoRepository expedienteRepository;
    private final EvidenciaPrimerContactoRepository evidenciaRepository;

    public IngresoPrimerContactoService(
            ExpedientePrimerContactoRepository expedienteRepository,
            EvidenciaPrimerContactoRepository evidenciaRepository
    ) {
        this.expedienteRepository = expedienteRepository;
        this.evidenciaRepository = evidenciaRepository;
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

        ExpedientePrimerContacto.ExpedientePrimerContactoBuilder builder =
                ExpedientePrimerContacto.builder()
                        .folio(generarFolio())
                        .folioOrigen(request.getFolioOrigen())
                        .tema(request.getTema())
                        .descripcionHechos(request.getDescripcionHechos())
                        .fechaRecepcionOrigen(request.getFechaRecepcion())
                        .prioridad(request.getPrioridad())
                        .estatus("PENDIENTE_ANALISIS")
                        .fechaCreacion(ahora)
                        .fechaActualizacion(ahora);

        if (request.getQuejoso() != null) {

            builder
                    .quejosoId(
                            request.getQuejoso().getId()
                    )
                    .quejosoNombre(
                            request.getQuejoso().getNombreCompleto()
                    )
                    .quejosoCorreo(
                            request.getQuejoso().getCorreo()
                    )
                    .quejosoTelefono(
                            request.getQuejoso().getTelefono()
                    )
                    .unidadAcademica(
                            request.getQuejoso().getUnidadAcademica()
                    )
                    .quejosoTipoUsuario(
                            request.getQuejoso().getTipoUsuario()
                    );
        }

        ExpedientePrimerContacto expediente =
                builder.build();

        ExpedientePrimerContacto guardado =
                expedienteRepository.save(expediente);

        /*
         * Guardamos una copia de los metadatos de las evidencias
         * que llegaron desde Revisión.
         */
        if (request.getEvidencias() != null) {

            request.getEvidencias().forEach(evidencia -> {

                EvidenciaPrimerContacto nueva =
                        EvidenciaPrimerContacto.builder()
                                .expedienteId(guardado.getId())
                                .evidenciaOrigenId(evidencia.getId())
                                .nombreArchivo(evidencia.getNombreArchivo())
                                .tipoArchivo(evidencia.getTipoArchivo())
                                .urlArchivo(evidencia.getUrlArchivo())
                                .fechaCarga(evidencia.getFechaCarga())
                                .build();

                evidenciaRepository.save(nueva);
            });
        }

        return guardado;    }

    private String generarFolio() {

        return "PC-"
                + UUID.randomUUID()
                .toString()
                .substring(0, 8)
                .toUpperCase();
    }
}