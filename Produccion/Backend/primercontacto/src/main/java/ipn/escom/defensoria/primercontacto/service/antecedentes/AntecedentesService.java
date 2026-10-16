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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
    private static final int MAX_POR_HECHOS = 20;
    private static final int SIMILITUD_COINCIDENCIA_FUERTE = 50;

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
        Personas personas = Personas.de(expediente, queja);

        if (motorModelo.habilitado()) {
            try {
                MotorAntecedentesModelo.ResultadoV5 v5 = motorModelo.buscarV5(expediente, queja);
                if (v5 != null) {
                    return tresListas(expediente, motorModelo, v5.porHechos().size(),
                            v5.porHechos(),
                            completarInvolucrados(v5.porInvolucrados(), v5.porHechos(), personas),
                            v5.coincidenciasFuertes(),
                            avisos);
                }
                // El modelo desplegado todavía es el anterior (sin ruta V5).
                List<AntecedenteDTO> resultados = motorModelo.buscar(expediente, queja, List.of());
                return tresListasDesdeTexto(expediente, motorModelo, resultados.size(), resultados,
                        personas, avisos);
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

        return tresListasDesdeTexto(expediente, motorReglas, candidatas.size(),
                motorReglas.buscar(expediente, queja, candidatas), personas, avisos);
    }

    /**
     * Las tres listas que ve el analista, igual que las del modelo V5:
     *   - por hechos: narrativas parecidas, aunque las personas sean otras (las primeras 20);
     *   - por involucrados: el mismo quejoso y/o denunciado;
     *   - coincidencias fuertes: misma persona Y hechos parecidos (prioritarias).
     */
    private BusquedaAntecedentesDTO tresListas(
            ExpedientePrimerContacto expediente,
            MotorAntecedentes motor,
            int analizadas,
            List<AntecedenteDTO> porHechos,
            List<AntecedenteDTO> porInvolucrados,
            List<AntecedenteDTO> coincidenciasFuertes,
            List<String> avisos
    ) {
        BusquedaAntecedentesDTO busqueda = resultado(expediente, motor, analizadas, porInvolucrados, avisos);
        busqueda.setPorHechos(porHechos.stream().limit(MAX_POR_HECHOS).toList());
        busqueda.setPorInvolucrados(porInvolucrados);
        busqueda.setCoincidenciasFuertes(coincidenciasFuertes);
        return busqueda;
    }

    /**
     * Para el modelo anterior y el motor por reglas, que solo comparan texto: las personas se
     * reconocen aquí, y es coincidencia fuerte la misma persona por identificación o correo
     * con similitud de hechos de 50% o más (el mismo criterio del modelo V5).
     */
    private BusquedaAntecedentesDTO tresListasDesdeTexto(
            ExpedientePrimerContacto expediente,
            MotorAntecedentes motor,
            int analizadas,
            List<AntecedenteDTO> resultados,
            Personas personas,
            List<String> avisos
    ) {
        List<AntecedenteDTO> porHechos = resultados.stream()
                .sorted(Comparator.comparingInt(AntecedenteDTO::getSimilitud).reversed())
                .toList();
        List<AntecedenteDTO> porInvolucrados = filtrarPorPersonas(copias(porHechos), personas);
        List<AntecedenteDTO> fuertes = new ArrayList<>();
        for (AntecedenteDTO a : porInvolucrados) {
            if (a.isIdentidadVerificada() && a.getSimilitud() >= SIMILITUD_COINCIDENCIA_FUERTE) {
                AntecedenteDTO fuerte = copia(a);
                fuerte.setRazonCoincidencia(a.getTipoCoincidencia() + " | similitud de hechos "
                        + a.getSimilitud() + "%");
                fuertes.add(fuerte);
            }
        }
        return tresListas(expediente, motor, analizadas, porHechos, porInvolucrados, fuertes, avisos);
    }

    /**
     * El modelo V5 reconoce a las personas por número de identificación o por nombre completo
     * idéntico. Se agregan además las que aquí se reconocen por correo o por nombre sin
     * segundo apellido ("Ana López" = "Ana López Ruiz"), marcadas como por verificar.
     */
    private List<AntecedenteDTO> completarInvolucrados(
            List<AntecedenteDTO> delModelo,
            List<AntecedenteDTO> porHechos,
            Personas personas
    ) {
        List<AntecedenteDTO> involucrados = new ArrayList<>(delModelo);
        Set<String> yaIncluidos = new HashSet<>();
        for (AntecedenteDTO a : delModelo) {
            yaIncluidos.add(MotorAntecedentesModelo.clave(a));
            // El modelo pudo reconocer solo a una de las dos personas: se completa la otra.
            filtrarPorPersonas(List.of(copia(a)), personas).stream().findFirst().ifPresent(nuestro -> {
                if (nuestro.isMismoQuejoso() && !a.isMismoQuejoso()
                        || nuestro.isMismoDenunciado() && !a.isMismoDenunciado()) {
                    List<String> nuevos = new ArrayList<>();
                    if (nuestro.isMismoQuejoso() && !a.isMismoQuejoso()) {
                        nuevos.add("Nombre del quejoso (por verificar)");
                    }
                    if (nuestro.isMismoDenunciado() && !a.isMismoDenunciado()) {
                        nuevos.add("Nombre del denunciado (por verificar)");
                    }
                    List<String> tipo = new ArrayList<>();
                    if (a.getTipoCoincidencia() != null && !a.getTipoCoincidencia().isBlank()) {
                        tipo.add(a.getTipoCoincidencia());
                    }
                    tipo.addAll(nuevos);
                    a.setTipoCoincidencia(String.join(" + ", tipo));
                    a.getCoincidencias().addAll(0, nuevos);
                    a.setMismoQuejoso(a.isMismoQuejoso() || nuestro.isMismoQuejoso());
                    a.setMismoDenunciado(a.isMismoDenunciado() || nuestro.isMismoDenunciado());
                    a.setIdentidadVerificada(a.isIdentidadVerificada() || nuestro.isIdentidadVerificada());
                }
            });
        }

        List<AntecedenteDTO> faltantes = porHechos.stream()
                .filter(a -> !yaIncluidos.contains(MotorAntecedentesModelo.clave(a)))
                .toList();
        involucrados.addAll(filtrarPorPersonas(copias(faltantes), personas));
        return involucrados;
    }

    private static List<AntecedenteDTO> copias(List<AntecedenteDTO> lista) {
        return lista.stream().map(AntecedentesService::copia).toList();
    }

    private static AntecedenteDTO copia(AntecedenteDTO a) {
        return a.toBuilder()
                .coincidencias(a.getCoincidencias() != null ? new ArrayList<>(a.getCoincidencias()) : new ArrayList<>())
                .build();
    }

    /** Quejoso y denunciado de la queja que se analiza, para reconocerlos en otras quejas. */
    private record Personas(String correo, String identificacion, String quejoso, String denunciado) {
        static Personas de(ExpedientePrimerContacto expediente, QuejaReferencia queja) {
            return new Personas(
                    queja != null && queja.getCorreoInstitucional() != null
                            ? queja.getCorreoInstitucional() : expediente.getQuejosoCorreo(),
                    queja != null ? queja.getNumeroIdentificacionQuejoso() : null,
                    queja != null
                            ? NombresPersona.unir(queja.getNombreQuejoso(), queja.getApellido1Quejoso(),
                                    queja.getApellido2Quejoso())
                            : expediente.getQuejosoNombre(),
                    queja != null
                            ? NombresPersona.unir(queja.getNombreDenunciado(), queja.getApellido1Denunciado(),
                                    queja.getApellido2Denunciado())
                            : null
            );
        }
    }

    /**
     * Los antecedentes siempre tienen que ver con las personas: el modelo (y el motor por
     * reglas) solo comparan el texto, así que aquí se dejan únicamente las quejas del MISMO
     * QUEJOSO (misma boleta/número de empleado, mismo correo o mismo nombre) y/o del MISMO
     * DENUNCIADO (mismo nombre). Primero las que coinciden en ambos, luego por similitud.
     */
    private List<AntecedenteDTO> filtrarPorPersonas(List<AntecedenteDTO> resultados, Personas personas) {
        List<AntecedenteDTO> filtrados = new ArrayList<>();
        for (AntecedenteDTO a : resultados) {
            boolean mismaIdentificacion = NombresPersona.mismoDato(personas.identificacion(), a.getIdentificacionQuejoso());
            boolean mismoCorreo = NombresPersona.mismoDato(personas.correo(), a.getCorreoQuejoso());
            // Mismo correo pero las dos quejas traen nombres que no coinciden (p. ej. alguien
            // registró quejas a nombre de otras personas con su correo): no se da por verificado.
            boolean correoConOtroNombre = mismoCorreo && !mismaIdentificacion
                    && personas.quejoso() != null && a.getNombreQuejoso() != null
                    && !a.getNombreQuejoso().isBlank()
                    && !NombresPersona.mismoNombre(personas.quejoso(), a.getNombreQuejoso());
            boolean mismoQuejoso = mismaIdentificacion || mismoCorreo
                    || NombresPersona.mismoNombre(personas.quejoso(), a.getNombreQuejoso());
            boolean mismoDenunciado = NombresPersona.mismoNombre(personas.denunciado(), a.getNombreDenunciado());
            if (!mismoQuejoso && !mismoDenunciado) {
                continue;
            }

            List<String> tipo = new ArrayList<>();
            if (mismaIdentificacion) {
                tipo.add("Identificación del quejoso");
            } else if (correoConOtroNombre) {
                tipo.add("Mismo correo, otro nombre (por verificar)");
            } else if (mismoCorreo) {
                tipo.add("Correo del quejoso");
            } else if (mismoQuejoso) {
                tipo.add("Nombre del quejoso (por verificar)");
            }
            if (mismoDenunciado) {
                tipo.add("Nombre del denunciado (por verificar)");
            }
            a.setTipoCoincidencia(String.join(" + ", tipo));
            a.setIdentidadVerificada(mismaIdentificacion || (mismoCorreo && !correoConOtroNombre));
            a.setMismoDenunciado(mismoDenunciado);

            // Mismas etiquetas que el modelo V5: dicen CÓMO coincidió la persona.
            List<String> coincidencias = new ArrayList<>(tipo);
            if (a.getCoincidencias() != null) {
                a.getCoincidencias().stream()
                        .filter(c -> !"Mismo quejoso".equals(c))
                        .forEach(coincidencias::add);
            }
            a.setCoincidencias(coincidencias);
            a.setMismoQuejoso(mismoQuejoso);
            filtrados.add(a);
        }
        // Primero las que coinciden en quejoso Y denunciado, luego por similitud.
        filtrados.sort(Comparator
                .comparingInt((AntecedenteDTO a) -> a.isMismoQuejoso() && a.isMismoDenunciado() ? 0 : 1)
                .thenComparing(Comparator.comparingInt(AntecedenteDTO::getSimilitud).reversed()));
        return filtrados;
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
                    .descripcion(item.getDescripcion())
                    .resultado(recortar(item.getResultado(), 50))
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
                .descripcion(a.getDescripcion())
                .resultado(a.getResultado())
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
