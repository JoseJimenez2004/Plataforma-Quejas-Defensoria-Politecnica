package ipn.escom.defensoria.primercontacto.service;

import ipn.escom.defensoria.primercontacto.dto.EvidenciaDTO;
import ipn.escom.defensoria.primercontacto.dto.ExpedienteAnalisisDTO;
import ipn.escom.defensoria.primercontacto.dto.NotaAnalisisDTO;
import ipn.escom.defensoria.primercontacto.dto.QuejosoDTO;

import ipn.escom.defensoria.primercontacto.entity.EstatusExpediente;
import ipn.escom.defensoria.primercontacto.entity.EvidenciaPrimerContacto;
import ipn.escom.defensoria.primercontacto.entity.ExpedientePrimerContacto;
import ipn.escom.defensoria.primercontacto.entity.NotaAnalisis;
import ipn.escom.defensoria.primercontacto.entity.PersonalAdministrativo;

import ipn.escom.defensoria.primercontacto.exception.RecursoNoEncontradoException;

import ipn.escom.defensoria.primercontacto.repository.EvidenciaPrimerContactoRepository;
import ipn.escom.defensoria.primercontacto.repository.ExpedientePrimerContactoRepository;
import ipn.escom.defensoria.primercontacto.repository.NotaAnalisisRepository;

import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ExpedienteAnalisisService {

    private final ExpedientePrimerContactoRepository expedienteRepository;
    private final NotaAnalisisRepository notaAnalisisRepository;
    private final EvidenciaPrimerContactoRepository evidenciaRepository;
    private final TransicionExpedienteService transicionService;

    public ExpedienteAnalisisService(
            ExpedientePrimerContactoRepository expedienteRepository,
            NotaAnalisisRepository notaAnalisisRepository,
            EvidenciaPrimerContactoRepository evidenciaRepository,
            TransicionExpedienteService transicionService
    ) {
        this.expedienteRepository = expedienteRepository;
        this.notaAnalisisRepository = notaAnalisisRepository;
        this.evidenciaRepository = evidenciaRepository;
        this.transicionService = transicionService;
    }

    /*
     * Busca utilizando el ID INTERNO
     * de Primer Contacto.
     */
    public ExpedienteAnalisisDTO obtenerExpediente(
            Long expedienteId
    ) {

        ExpedientePrimerContacto expediente =
                expedienteRepository
                        .findById(expedienteId)
                        .orElseThrow(() ->
                                new RecursoNoEncontradoException(
                                        "No existe el expediente de Primer Contacto con id "
                                                + expedienteId
                                )
                        );

        return construirDTO(expediente);
    }

    /*
     * Busca utilizando el folio propio
     * de Primer Contacto: PC-XXXXXXXX
     */
    public ExpedienteAnalisisDTO obtenerPorFolio(
            String folio
    ) {

        ExpedientePrimerContacto expediente =
                expedienteRepository
                        .findByFolio(folio)
                        .orElseThrow(() ->
                                new RecursoNoEncontradoException(
                                        "No existe el expediente de Primer Contacto con folio "
                                                + folio
                                )
                        );

        return construirDTO(expediente);
    }

    /*
     * CU-PC-03: al abrir el expediente para trabajarlo, un expediente
     * TURNADA pasa a EN_ANALISIS y queda registrado quién lo abrió y
     * cuándo -- mismo patrón que Revisión al abrir una queja.
     *
     * Es un endpoint explícito (lo llama la pantalla de detalle) y no
     * un efecto de GET /folio/{folio}, porque Agenda, Dictamen y
     * Remisión también consultan el expediente y eso no es "abrirlo".
     * En cualquier otro estado no cambia nada.
     */
    public ExpedienteAnalisisDTO iniciarAnalisis(
            String folio,
            PersonalAdministrativo analista
    ) {

        ExpedientePrimerContacto expediente =
                transicionService.obtenerPorFolio(folio);

        if (EstatusExpediente.TURNADA.equals(expediente.getEstatus())) {

            expediente.setFechaInicioAnalisis(LocalDateTime.now());
            expediente.setAnalistaAnalisisId(analista.getId());
            expediente.setAnalistaAnalisisNombre(analista.getNombreCompleto());

            expediente = transicionService.cambiarEstatus(
                    expediente,
                    EstatusExpediente.EN_ANALISIS
            );
        }

        return construirDTO(expediente);
    }

    private ExpedienteAnalisisDTO construirDTO(
            ExpedientePrimerContacto expediente
    ) {

        /*
         * Notas internas del expediente de Primer Contacto.
         */
        List<NotaAnalisisDTO> notas =
                notaAnalisisRepository
                        .findByExpedienteIdOrderByFechaCreacionDesc(
                                expediente.getId()
                        )
                        .stream()
                        .map(this::convertirNotaDTO)
                        .toList();

        /*
         * Evidencias recibidas cuando el expediente
         * ingresó a Primer Contacto.
         */
        List<EvidenciaDTO> evidencias =
                evidenciaRepository
                        .findByExpedienteId(
                                expediente.getId()
                        )
                        .stream()
                        .map(this::convertirEvidenciaDTO)
                        .toList();

        QuejosoDTO quejoso =
                QuejosoDTO.builder()
                        .id(
                                expediente.getQuejosoId()
                        )
                        .nombreCompleto(
                                expediente.getQuejosoNombre()
                        )
                        .correo(
                                expediente.getQuejosoCorreo()
                        )
                        .telefono(
                                expediente.getQuejosoTelefono()
                        )
                        .unidadAcademica(
                                expediente.getUnidadAcademica()
                        )
                        .tipoUsuario(
                                expediente.getQuejosoTipoUsuario()
                        )
                        .build();

        return ExpedienteAnalisisDTO.builder()
                .expedienteId(
                        expediente.getId()
                )
                .folio(
                        expediente.getFolio()
                )
                .folioOrigen(
                        expediente.getFolioOrigen()
                )
                .folioSubdefensoria(
                        expediente.getFolioSubdefensoria()
                )
                .tema(
                        expediente.getTema()
                )
                .descripcionHechos(
                        expediente.getDescripcionHechos()
                )
                .fechaRecepcion(
                        expediente.getFechaRecepcionOrigen()
                )
                .estatus(
                        expediente.getEstatus()
                )
                .prioridad(
                        expediente.getPrioridad()
                )
                .fechaInicioAnalisis(
                        expediente.getFechaInicioAnalisis() != null
                                ? expediente.getFechaInicioAnalisis().toString()
                                : null
                )
                .analistaAnalisisNombre(
                        expediente.getAnalistaAnalisisNombre()
                )
                .quejoso(quejoso)
                .evidencias(evidencias)
                .notas(notas)
                .build();
    }

    private NotaAnalisisDTO convertirNotaDTO(
            NotaAnalisis nota
    ) {

        return NotaAnalisisDTO.builder()
                .id(nota.getId())
                .expedienteId(
                        nota.getExpedienteId()
                )
                .folio(
                        nota.getFolio()
                )
                .analistaId(
                        nota.getAnalistaId()
                )
                .analistaNombre(
                        nota.getAnalistaNombre()
                )
                .contenido(
                        nota.getContenido()
                )
                .fechaCreacion(
                        nota.getFechaCreacion() != null
                                ? nota.getFechaCreacion().toString()
                                : null
                )
                .fechaActualizacion(
                        nota.getFechaActualizacion() != null
                                ? nota.getFechaActualizacion().toString()
                                : null
                )
                .build();
    }

    private EvidenciaDTO convertirEvidenciaDTO(
            EvidenciaPrimerContacto evidencia
    ) {

        return EvidenciaDTO.builder()

                /*
                 * Conservamos preferentemente el ID que
                 * tenía la evidencia en el área de origen.
                 *
                 * Si no existe, usamos el ID interno
                 * de Primer Contacto.
                 */
                .id(
                        evidencia.getEvidenciaOrigenId() != null
                                ? evidencia.getEvidenciaOrigenId()
                                : evidencia.getId()
                )

                .nombreArchivo(
                        evidencia.getNombreArchivo()
                )
                .tipoArchivo(
                        evidencia.getTipoArchivo()
                )
                .urlArchivo(
                        evidencia.getUrlArchivo()
                )
                .fechaCarga(
                        evidencia.getFechaCarga()
                )
                .build();
    }
}