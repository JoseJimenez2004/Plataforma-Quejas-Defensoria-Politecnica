package ipn.escom.defensoria.primercontacto.service;

import ipn.escom.defensoria.primercontacto.dto.BandejaAnalisisDTO;
import ipn.escom.defensoria.primercontacto.dto.FiltroExpedienteDTO;
import ipn.escom.defensoria.primercontacto.entity.ExpedientePrimerContacto;
import ipn.escom.defensoria.primercontacto.repository.CitaPrimerContactoRepository;
import ipn.escom.defensoria.primercontacto.repository.ExpedientePrimerContactoRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;

@Service
public class BandejaAnalisisService {

    private final ExpedientePrimerContactoRepository expedienteRepository;
    private final CitaPrimerContactoRepository citaRepository;

    public BandejaAnalisisService(
            ExpedientePrimerContactoRepository expedienteRepository,
            CitaPrimerContactoRepository citaRepository
    ) {
        this.expedienteRepository = expedienteRepository;
        this.citaRepository = citaRepository;
    }

    public List<BandejaAnalisisDTO> obtenerBandeja(
            String token
    ) {

        return expedienteRepository.findAll()
                .stream()
                .sorted(masRecientesPrimero())
                .map(convertidor())
                .toList();
    }

    public BandejaAnalisisDTO buscarPorFolio(
            String folio,
            String token
    ) {

        return expedienteRepository
                .findByFolio(folio)
                .map(this::convertirADTO)
                .orElse(null);
    }

    public List<BandejaAnalisisDTO> filtrar(
            FiltroExpedienteDTO filtro,
            String token
    ) {

        String texto = normalizar(
                filtro.getTexto() != null ? filtro.getTexto() : filtro.getFolio()
        );

        LocalDate desde = parsearFecha(filtro.getFechaInicio());
        LocalDate hasta = parsearFecha(filtro.getFechaFin());

        Comparator<ExpedientePrimerContacto> orden =
                "antiguos".equalsIgnoreCase(filtro.getOrden())
                        ? masRecientesPrimero().reversed()
                        : masRecientesPrimero();

        return expedienteRepository.findAll()
                .stream()

                .filter(e ->
                        texto == null
                                || contiene(e.getFolio(), texto)
                                || contiene(e.getFolioOrigen(), texto)
                                || contiene(e.getQuejosoNombre(), texto)
                )

                .filter(e ->
                        filtro.getNombreQuejoso() == null
                                || filtro.getNombreQuejoso().isBlank()
                                || contiene(e.getQuejosoNombre(), normalizar(filtro.getNombreQuejoso()))
                )

                .filter(e -> coincidePrioridad(e.getPrioridad(), filtro.getPrioridad(), filtro.getPrioridades()))
                .filter(e -> coincide(e.getEstatus(), filtro.getEstatus(), filtro.getEstatusLista()))
                .filter(e -> coincide(e.getUnidadAcademica(), filtro.getUnidadAcademica(), filtro.getUnidadesAcademicas()))
                .filter(e -> coincide(e.getTema(), null, filtro.getTemas()))

                .filter(e -> desde == null || fechaDe(e) == null || !fechaDe(e).isBefore(desde))
                .filter(e -> hasta == null || fechaDe(e) == null || !fechaDe(e).isAfter(hasta))

                .sorted(orden)
                .map(convertidor())
                .toList();
    }

    public List<BandejaAnalisisDTO> obtenerPorPrioridad(
            String prioridad,
            String token
    ) {

        return expedienteRepository.findAll()
                .stream()
                .filter(e -> coincide(e.getPrioridad(), prioridad, null))
                .map(convertidor())
                .toList();
    }

    public List<BandejaAnalisisDTO> obtenerPorEstatus(
            String estatus,
            String token
    ) {

        return expedienteRepository.findAll()
                .stream()
                .filter(e -> coincide(e.getEstatus(), estatus, null))
                .map(convertidor())
                .toList();
    }

    /*
     * PC-5: antes cada fila hacía su propia consulta de "¿tiene cita activa?" (≈1 200 consultas
     * por petición con un pool de 3 conexiones). Ahora se traen todos los ids con cita activa en
     * una sola consulta y cada fila solo revisa el conjunto.
     */
    private Function<ExpedientePrimerContacto, BandejaAnalisisDTO> convertidor() {
        Set<Long> conCita = citaRepository.idsExpedientesConCitaActiva(CitaPrimerContactoService.CERRADAS);
        return e -> convertirADTO(e, conCita.contains(e.getId()));
    }

    private BandejaAnalisisDTO convertirADTO(
            ExpedientePrimerContacto expediente
    ) {
        return convertirADTO(expediente, citaRepository.existsByExpedienteIdAndEstatusNotIn(
                expediente.getId(), CitaPrimerContactoService.CERRADAS));
    }

    private BandejaAnalisisDTO convertirADTO(
            ExpedientePrimerContacto expediente,
            boolean tieneCitaActiva
    ) {

        return BandejaAnalisisDTO.builder()
                .expedienteId(expediente.getId())
                .folio(expediente.getFolio())
                .folioOrigen(expediente.getFolioOrigen())
                .nombreQuejoso(expediente.getQuejosoNombre())
                .unidadAcademica(expediente.getUnidadAcademica())
                .tema(expediente.getTema())
                .prioridad(expediente.getPrioridad())
                .estatus(expediente.getEstatus())
                .fechaRecepcion(expediente.getFechaRecepcionOrigen())
                .tieneCitaActiva(tieneCitaActiva)
                .build();
    }

    /*
     * Un valor simple y/o una lista: el campo coincide si es igual
     * (sin distinguir mayúsculas) a cualquiera de ellos.
     */
    /**
     * PC-1: igual que coincide(), pero "SIN_ASIGNAR" encuentra los expedientes sin prioridad.
     */
    private boolean coincidePrioridad(String valor, String unico, List<String> varios) {
        boolean sinValor = valor == null || valor.isBlank();
        if (sinValor) {
            boolean pideSinAsignar = "SIN_ASIGNAR".equalsIgnoreCase(unico)
                    || (varios != null && varios.stream().anyMatch("SIN_ASIGNAR"::equalsIgnoreCase));
            boolean sinFiltro = (unico == null || unico.isBlank()) && (varios == null || varios.isEmpty());
            return sinFiltro || pideSinAsignar;
        }
        return coincide(valor, unico, varios);
    }

    private boolean coincide(
            String valor,
            String unico,
            List<String> varios
    ) {

        boolean sinUnico = unico == null || unico.isBlank();
        boolean sinVarios = varios == null || varios.isEmpty();

        if (sinUnico && sinVarios) {
            return true;
        }

        if (valor == null) {
            return false;
        }

        if (!sinUnico && valor.equalsIgnoreCase(unico)) {
            return true;
        }

        return !sinVarios && varios.stream()
                .filter(Objects::nonNull)
                .anyMatch(valor::equalsIgnoreCase);
    }

    private boolean contiene(String valor, String textoNormalizado) {
        return valor != null && valor.toLowerCase().contains(textoNormalizado);
    }

    private String normalizar(String texto) {
        return texto == null || texto.isBlank() ? null : texto.trim().toLowerCase();
    }

    private Comparator<ExpedientePrimerContacto> masRecientesPrimero() {
        return Comparator.comparing(
                ExpedientePrimerContacto::getFechaCreacion,
                Comparator.nullsLast(Comparator.reverseOrder())
        );
    }

    /*
     * fechaRecepcionOrigen llega como texto ISO (fecha o fecha-hora).
     */
    private LocalDate fechaDe(ExpedientePrimerContacto expediente) {
        String fecha = expediente.getFechaRecepcionOrigen();
        return fecha == null || fecha.length() < 10 ? null : parsearFecha(fecha.substring(0, 10));
    }

    private LocalDate parsearFecha(String fecha) {
        try {
            return fecha == null || fecha.isBlank() ? null : LocalDate.parse(fecha.trim());
        } catch (RuntimeException ex) {
            return null;
        }
    }
}
