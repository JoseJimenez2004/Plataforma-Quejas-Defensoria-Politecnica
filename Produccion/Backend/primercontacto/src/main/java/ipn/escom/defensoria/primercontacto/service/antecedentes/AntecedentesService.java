package ipn.escom.defensoria.primercontacto.service.antecedentes;

import ipn.escom.defensoria.primercontacto.dto.AntecedenteDTO;
import ipn.escom.defensoria.primercontacto.dto.AntecedenteGuardadoDTO;
import ipn.escom.defensoria.primercontacto.dto.BusquedaAntecedentesDTO;
import ipn.escom.defensoria.primercontacto.entity.AntecedenteExpediente;
import ipn.escom.defensoria.primercontacto.entity.ExpedientePrimerContacto;
import ipn.escom.defensoria.primercontacto.entity.PersonalAdministrativo;
import ipn.escom.defensoria.primercontacto.entity.QuejaReferencia;
import ipn.escom.defensoria.primercontacto.exception.OperacionInvalidaException;
import ipn.escom.defensoria.primercontacto.exception.RecursoNoEncontradoException;
import ipn.escom.defensoria.primercontacto.repository.AntecedenteExpedienteRepository;
import ipn.escom.defensoria.primercontacto.repository.QuejaReferenciaRepository;
import ipn.escom.defensoria.primercontacto.service.TransicionExpedienteService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Búsqueda de antecedentes de una queja, en dos modalidades que el analista puede combinar:
 *
 *   - CON EL MODELO (buscar): otras quejas parecidas por su narrativa. Se usa el modelo de
 *     antecedentes si está configurado; si no responde, el motor provisional por reglas, y
 *     la pantalla lo avisa.
 *   - MANUAL (buscarManual): el criterio humano del analista, por nombre del quejoso y/o del
 *     denunciado, en las quejas del sistema y en los casos históricos.
 *
 * De cualquiera de las dos, el analista elige cuáles son los antecedentes FINALES del
 * expediente y los guarda (guardar / listarGuardados / quitarGuardado).
 */
@Service
public class AntecedentesService {

    private static final Logger log = LoggerFactory.getLogger(AntecedentesService.class);

    static final String MOTOR_MANUAL = "MANUAL";
    private static final int MAX_RESULTADOS_MANUAL = 100;

    private final TransicionExpedienteService transicionService;
    private final QuejaReferenciaRepository quejaRepository;
    private final MotorAntecedentesModelo motorModelo;
    private final MotorAntecedentesReglas motorReglas;
    private final HistoricoAntecedentesRepository historicoRepository;
    private final AntecedenteExpedienteRepository guardadosRepository;

    public AntecedentesService(
            TransicionExpedienteService transicionService,
            QuejaReferenciaRepository quejaRepository,
            MotorAntecedentesModelo motorModelo,
            MotorAntecedentesReglas motorReglas,
            HistoricoAntecedentesRepository historicoRepository,
            AntecedenteExpedienteRepository guardadosRepository
    ) {
        this.transicionService = transicionService;
        this.quejaRepository = quejaRepository;
        this.motorModelo = motorModelo;
        this.motorReglas = motorReglas;
        this.historicoRepository = historicoRepository;
        this.guardadosRepository = guardadosRepository;
    }

    // ---------------------------------------------------------------------------------
    // Búsqueda con el modelo
    // ---------------------------------------------------------------------------------

    public BusquedaAntecedentesDTO buscar(String folio) {
        ExpedientePrimerContacto expediente = transicionService.obtenerPorFolio(folio);
        QuejaReferencia queja = quejaRepository
                .findByNumeroFolio(expediente.getFolioOrigen())
                .orElse(null);

        List<String> avisos = new ArrayList<>();

        if (motorModelo.habilitado()) {
            try {
                List<AntecedenteDTO> resultados = motorModelo.buscar(expediente, queja, List.of());
                return resultado(expediente, motorModelo, 0, resultados, avisos);
            } catch (Exception ex) {
                log.warn("El modelo de antecedentes no respondió ({}); se usa el motor por reglas.",
                        ex.getMessage());
                avisos.add("El modelo de antecedentes no está disponible en este momento; se "
                        + "muestran los resultados del motor provisional por reglas.");
            }
        }

        List<QuejaReferencia> candidatas = quejaRepository.findAll().stream()
                .filter(q -> !q.getNumeroFolio().equals(expediente.getFolioOrigen()))
                .toList();

        return resultado(expediente, motorReglas, candidatas.size(),
                motorReglas.buscar(expediente, queja, candidatas), avisos);
    }

    // ---------------------------------------------------------------------------------
    // Búsqueda manual por quejoso y/o denunciado
    // ---------------------------------------------------------------------------------

    public BusquedaAntecedentesDTO buscarManual(String folio, String quejoso, String denunciado) {
        ExpedientePrimerContacto expediente = transicionService.obtenerPorFolio(folio);

        List<String> palabrasQuejoso = NombresPersona.palabras(quejoso);
        List<String> palabrasDenunciado = NombresPersona.palabras(denunciado);
        if (palabrasQuejoso.isEmpty() && palabrasDenunciado.isEmpty()) {
            throw new OperacionInvalidaException("Escribe el nombre del quejoso o del denunciado.");
        }

        List<String> avisos = new ArrayList<>();
        List<AntecedenteDTO> resultados = new ArrayList<>();

        // Quejas del sistema (defensoria_db), salvo la propia.
        List<QuejaReferencia> quejas = quejaRepository.findAll();
        for (QuejaReferencia q : quejas) {
            if (q.getNumeroFolio().equals(expediente.getFolioOrigen())) {
                continue;
            }
            AntecedenteDTO dto = AntecedenteDTO.builder()
                    .folioQueja(q.getNumeroFolio())
                    .folioPrimerContacto(q.getFolioPrimerContacto())
                    .fecha(q.getFechaCreacion() != null ? q.getFechaCreacion().toString() : null)
                    .asunto(q.getMotivo())
                    .descripcion(q.getDescripcion())
                    .unidadAcademica(q.getUnidadAcademicaClave())
                    .nombreQuejoso(NombresPersona.unir(q.getNombreQuejoso(), q.getApellido1Quejoso(),
                            q.getApellido2Quejoso()))
                    .nombreDenunciado(NombresPersona.unir(q.getNombreDenunciado(),
                            q.getApellido1Denunciado(), q.getApellido2Denunciado()))
                    .estatus(q.getEstatus())
                    .origen(NombresPersona.ORIGEN_SISTEMA)
                    .build();
            if (marcarCoincidencias(dto, palabrasQuejoso, palabrasDenunciado)) {
                resultados.add(dto);
            }
        }
        int analizadas = quejas.size() - 1;

        // Casos históricos (historico_db).
        if (!historicoRepository.habilitado()) {
            avisos.add("La búsqueda en casos históricos no está configurada en este servidor; "
                    + "solo se buscó en las quejas del sistema.");
        } else {
            try {
                for (AntecedenteDTO dto : historicoRepository.buscar(palabrasQuejoso, palabrasDenunciado)) {
                    if (marcarCoincidencias(dto, palabrasQuejoso, palabrasDenunciado)) {
                        resultados.add(dto);
                    }
                }
            } catch (Exception ex) {
                log.warn("No se pudo consultar el histórico de quejas: {}", ex.getMessage());
                avisos.add("No fue posible consultar los casos históricos en este momento; solo "
                        + "se muestran quejas del sistema.");
            }
        }

        // Primero las que coinciden en quejoso Y denunciado, luego las más recientes.
        resultados.sort(Comparator.comparingInt(AntecedenteDTO::getSimilitud).reversed()
                .thenComparing(AntecedenteDTO::getFecha, Comparator.nullsLast(Comparator.reverseOrder())));

        BusquedaAntecedentesDTO busqueda = resultado(expediente, null, Math.max(analizadas, 0),
                resultados.stream().limit(MAX_RESULTADOS_MANUAL).toList(), avisos);
        busqueda.setMotor(MOTOR_MANUAL);
        busqueda.setDescripcionMotor("Búsqueda manual por nombre del quejoso y/o del denunciado, "
                + "en las quejas del sistema y en los casos históricos.");
        return busqueda;
    }

    /**
     * Anota por qué coincidió y devuelve si coincidió en algo. La "similitud" de la búsqueda
     * manual solo sirve para ordenar: 100 si coinciden quejoso y denunciado, 50 si uno.
     */
    private boolean marcarCoincidencias(
            AntecedenteDTO dto,
            List<String> palabrasQuejoso,
            List<String> palabrasDenunciado
    ) {
        List<String> coincidencias = new ArrayList<>();
        boolean quejoso = NombresPersona.coincide(dto.getNombreQuejoso(), palabrasQuejoso);
        boolean denunciado = NombresPersona.coincide(dto.getNombreDenunciado(), palabrasDenunciado);
        if (quejoso) {
            coincidencias.add("Quejoso: " + dto.getNombreQuejoso());
        }
        if (denunciado) {
            coincidencias.add("Denunciado: " + dto.getNombreDenunciado());
        }
        dto.setCoincidencias(coincidencias);
        dto.setMismoQuejoso(quejoso);
        dto.setSimilitud(quejoso && denunciado ? 100 : 50);
        if (dto.getExtracto() == null) {
            dto.setExtracto(extracto(dto.getDescripcion()));
        }
        return quejoso || denunciado;
    }

    // ---------------------------------------------------------------------------------
    // Antecedentes finales elegidos por el analista
    // ---------------------------------------------------------------------------------

    public List<AntecedenteGuardadoDTO> listarGuardados(String folio) {
        ExpedientePrimerContacto expediente = transicionService.obtenerPorFolio(folio);
        return guardadosRepository.findByExpedienteIdOrderByFechaRegistroDesc(expediente.getId())
                .stream()
                .map(this::convertir)
                .toList();
    }

    /** Guarda la selección; las que ya estaban guardadas se ignoran (no se duplican). */
    @Transactional
    public List<AntecedenteGuardadoDTO> guardar(
            String folio,
            List<AntecedenteGuardadoDTO> seleccion,
            PersonalAdministrativo analista
    ) {
        ExpedientePrimerContacto expediente = transicionService.obtenerPorFolio(folio);
        LocalDateTime ahora = LocalDateTime.now();

        for (AntecedenteGuardadoDTO item : seleccion) {
            String origen = item.getOrigen().strip().toUpperCase();
            String folioAntecedente = item.getFolioQueja().strip();

            if (folioAntecedente.equals(expediente.getFolioOrigen())) {
                throw new OperacionInvalidaException("Una queja no puede ser antecedente de sí misma.");
            }
            if (guardadosRepository.existsByExpedienteIdAndOrigenAndFolioAntecedente(
                    expediente.getId(), origen, folioAntecedente)) {
                continue;
            }

            guardadosRepository.save(AntecedenteExpediente.builder()
                    .expedienteId(expediente.getId())
                    .origen(origen)
                    .folioAntecedente(folioAntecedente)
                    .fuente(item.getFuente().strip().toUpperCase())
                    .similitud(MOTOR_MANUAL.equalsIgnoreCase(item.getFuente()) ? null : item.getSimilitud())
                    .asunto(recortar(item.getAsunto(), 500))
                    .fechaAntecedente(recortar(item.getFecha(), 30))
                    .nombreQuejoso(recortar(item.getNombreQuejoso(), 300))
                    .nombreDenunciado(recortar(item.getNombreDenunciado(), 300))
                    .unidadAcademica(recortar(item.getUnidadAcademica(), 60))
                    .estatusAntecedente(recortar(item.getEstatus(), 50))
                    .folioPrimerContacto(recortar(item.getFolioPrimerContacto(), 50))
                    .extracto(item.getExtracto())
                    .analistaId(analista.getId())
                    .analistaNombre(analista.getNombreCompleto())
                    .fechaRegistro(ahora)
                    .build());
        }

        return listarGuardados(folio);
    }

    @Transactional
    public void quitarGuardado(String folio, Long id) {
        ExpedientePrimerContacto expediente = transicionService.obtenerPorFolio(folio);
        AntecedenteExpediente guardado = guardadosRepository.findById(id)
                .filter(a -> a.getExpedienteId().equals(expediente.getId()))
                .orElseThrow(() -> new RecursoNoEncontradoException("Antecedente no encontrado."));
        guardadosRepository.delete(guardado);
    }

    // ---------------------------------------------------------------------------------

    private BusquedaAntecedentesDTO resultado(
            ExpedientePrimerContacto expediente,
            MotorAntecedentes motor,
            int analizadas,
            List<AntecedenteDTO> resultados,
            List<String> avisos
    ) {
        return BusquedaAntecedentesDTO.builder()
                .folio(expediente.getFolio())
                .folioQueja(expediente.getFolioOrigen())
                .motor(motor != null ? motor.nombre() : null)
                .descripcionMotor(motor != null ? motor.descripcion() : null)
                .generadoEn(LocalDateTime.now().toString())
                .quejasAnalizadas(analizadas)
                .resultados(resultados)
                .avisos(avisos)
                .build();
    }

    private AntecedenteGuardadoDTO convertir(AntecedenteExpediente a) {
        return AntecedenteGuardadoDTO.builder()
                .id(a.getId())
                .origen(a.getOrigen())
                .folioQueja(a.getFolioAntecedente())
                .fuente(a.getFuente())
                .similitud(a.getSimilitud())
                .asunto(a.getAsunto())
                .fecha(a.getFechaAntecedente())
                .nombreQuejoso(a.getNombreQuejoso())
                .nombreDenunciado(a.getNombreDenunciado())
                .unidadAcademica(a.getUnidadAcademica())
                .estatus(a.getEstatusAntecedente())
                .folioPrimerContacto(a.getFolioPrimerContacto())
                .extracto(a.getExtracto())
                .analistaNombre(a.getAnalistaNombre())
                .fechaRegistro(a.getFechaRegistro() != null ? a.getFechaRegistro().toString() : null)
                .build();
    }

    private static String recortar(String texto, int max) {
        if (texto == null) {
            return null;
        }
        String limpio = texto.strip();
        return limpio.length() <= max ? limpio : limpio.substring(0, max);
    }

    private static String extracto(String descripcion) {
        if (descripcion == null) {
            return null;
        }
        String texto = descripcion.strip();
        return texto.length() <= 220 ? texto : texto.substring(0, 217) + "…";
    }
}
