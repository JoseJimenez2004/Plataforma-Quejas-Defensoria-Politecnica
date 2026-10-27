package ipn.escom.defensoria.primercontacto.service;

import ipn.escom.defensoria.primercontacto.dto.DatosQuejaDTO;
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
import ipn.escom.defensoria.primercontacto.repository.QuejaReferenciaRepository;

import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ExpedienteAnalisisService {

    private final ExpedientePrimerContactoRepository expedienteRepository;
    private final NotaAnalisisRepository notaAnalisisRepository;
    private final EvidenciaPrimerContactoRepository evidenciaRepository;
    private final TransicionExpedienteService transicionService;
    private final QuejaReferenciaRepository quejaRepository;

    public ExpedienteAnalisisService(
            ExpedientePrimerContactoRepository expedienteRepository,
            NotaAnalisisRepository notaAnalisisRepository,
            EvidenciaPrimerContactoRepository evidenciaRepository,
            TransicionExpedienteService transicionService,
            QuejaReferenciaRepository quejaRepository
    ) {
        this.expedienteRepository = expedienteRepository;
        this.notaAnalisisRepository = notaAnalisisRepository;
        this.evidenciaRepository = evidenciaRepository;
        this.transicionService = transicionService;
        this.quejaRepository = quejaRepository;
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
                .datosQueja(datosQueja(expediente))
                .evidencias(evidencias)
                .notas(notas)
                .build();
    }

    /*
     * Lo que el quejoso capturó en su queja (denunciado, fecha de los hechos...), leído de la
     * tabla quejas por el folio de origen. Si falla, el detalle se muestra sin estos datos.
     */
    private DatosQuejaDTO datosQueja(ExpedientePrimerContacto expediente) {
        if (expediente.getFolioOrigen() == null) {
            return null;
        }
        try {
            return quejaRepository.findByNumeroFolio(expediente.getFolioOrigen())
                    .map(q -> DatosQuejaDTO.builder()
                            .fechaRegistro(q.getFechaCreacion() != null
                                    ? q.getFechaCreacion().toLocalDate().toString() : null)
                            .asunto(q.getMotivo())
                            .fechaHechos(q.getFechaHechos() != null ? q.getFechaHechos().toString() : null)
                            .unidadAcademicaClave(q.getUnidadAcademicaClave())
                            .tipoIdentificacionQuejoso(q.getTipoIdentificacionQuejoso())
                            .numeroIdentificacionQuejoso(q.getNumeroIdentificacionQuejoso())
                            .fechaNacimientoQuejoso(q.getFechaNacimientoQuejoso() != null
                                    ? q.getFechaNacimientoQuejoso().toString() : null)
                            .denunciado(unir(q.getNombreDenunciado(), q.getApellido1Denunciado(),
                                    q.getApellido2Denunciado()))
                            .build())
                    .orElse(null);
        } catch (Exception ex) {
            return null;
        }
    }

    private static String unir(String... partes) {
        String nombre = java.util.Arrays.stream(partes)
                .filter(p -> p != null && !p.isBlank())
                .map(String::strip)
                .collect(java.util.stream.Collectors.joining(" "));
        return nombre.isEmpty() ? null : nombre;
    }

    private NotaAnalisisDTO convertirNotaDTO(
            NotaAnalisis nota
    ) {
        return NotaAnalisisService.convertirADTO(nota);
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