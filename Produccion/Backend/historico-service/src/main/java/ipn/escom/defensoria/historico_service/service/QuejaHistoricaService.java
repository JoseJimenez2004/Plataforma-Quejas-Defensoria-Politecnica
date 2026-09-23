package ipn.escom.defensoria.historico_service.service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import ipn.escom.defensoria.historico_service.dto.CapturaQuejaHistoricaRequest;
import ipn.escom.defensoria.historico_service.dto.EvidenciaResumenDTO;
import ipn.escom.defensoria.historico_service.dto.PersonaDTO;
import ipn.escom.defensoria.historico_service.dto.QuejaHistoricaDTO;
import ipn.escom.defensoria.historico_service.entity.EvidenciaHistorica;
import ipn.escom.defensoria.historico_service.entity.QuejaHistorica;
import ipn.escom.defensoria.historico_service.repository.EvidenciaHistoricaRepository;
import ipn.escom.defensoria.historico_service.repository.QuejaHistoricaRepository;

@Service
public class QuejaHistoricaService {

    private static final String ORIGEN = "HISTORICO";
    private static final String PREFIJO_SIN_FOLIO = "SF-";

    private static final Set<String> FUENTES = Set.of(
            "EXCEL", "LLAMADA", "OFICIO_FISICO", "SISTEMA_ANTERIOR", "OTRO");
    private static final Set<String> ESTATUS = Set.of(
            "RECHAZADA", "IMPROCEDENTE", "REMITIDA", "CONCLUIDA");
    private static final Set<String> RESULTADOS = Set.of(
            "RECHAZADA_EN_RECEPCION", "IMPROCEDENTE", "REMITIDA_A_OTRA_AUTORIDAD",
            "CONCLUIDA_CON_ACUERDO", "CONCLUIDA_SIN_ACUERDO", "SIN_DATO");
    private static final Set<String> TIPOS_IDENTIFICACION = Set.of(
            "BOLETA", "EMPLEADO", "CURP", "INE", "OTRO");
    // Los 3 que ofrece el selector de registro-manual. DOCENTE y ADMINISTRATIVO no
    // existen por separado en el sistema: ambos caen en EMPLEADO.
    private static final Set<String> TIPOS_USUARIO = Set.of(
            "ALUMNO", "EMPLEADO", "EXTERNO");

    private static final DateTimeFormatter ISO_FECHA_HORA = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private final QuejaHistoricaRepository quejaRepository;
    private final EvidenciaHistoricaRepository evidenciaRepository;

    public QuejaHistoricaService(QuejaHistoricaRepository quejaRepository,
                                 EvidenciaHistoricaRepository evidenciaRepository) {
        this.quejaRepository = quejaRepository;
        this.evidenciaRepository = evidenciaRepository;
    }

    // ------------------------------------------------------------------ captura

    @Transactional
    public QuejaHistorica capturar(CapturaQuejaHistoricaRequest datos, String capturadoPor) {

        QuejaHistorica queja = new QuejaHistorica();

        String folio = limpiar(datos.getFolio());
        if (folio == null) {
            queja.setFolio(generarFolioSinOriginal());
            queja.setFolioGenerado(true);
        } else {
            if (quejaRepository.existsByFolio(folio)) {
                throw new IllegalArgumentException(
                        "Ya existe una queja histórica con el folio " + folio
                                + ". Si son expedientes distintos que comparten número, "
                                + "agrega un sufijo para distinguirlos (por ejemplo "
                                + folio + "-B).");
            }
            queja.setFolio(folio);
            queja.setFolioGenerado(false);
        }

        queja.setFuente(validar("fuente", datos.getFuente(), FUENTES, false));
        queja.setFechaPresentacionOriginal(datos.getFechaPresentacionOriginal());
        queja.setFechaHechos(datos.getFechaHechos());
        queja.setUnidadAcademicaClave(mayusculas(datos.getUnidadAcademicaClave()));
        queja.setMotivo(limpiar(datos.getMotivo()));
        queja.setDescripcion(limpiar(datos.getDescripcion()));

        String estatus = validar("estatus", datos.getEstatus(), ESTATUS, true);
        queja.setEstatus(estatus != null ? estatus : "CONCLUIDA");

        String resultado = validar("resultado", datos.getResultado(), RESULTADOS, true);
        queja.setResultado(resultado != null ? resultado : "SIN_DATO");

        queja.setQuejosoTipoIdentificacion(
                validar("tipo de identificación del quejoso",
                        datos.getQuejosoTipoIdentificacion(), TIPOS_IDENTIFICACION, true));
        queja.setQuejosoNumeroIdentificacion(limpiar(datos.getQuejosoNumeroIdentificacion()));
        queja.setQuejosoNombre(limpiar(datos.getQuejosoNombre()));
        queja.setQuejosoApellido1(limpiar(datos.getQuejosoApellido1()));
        queja.setQuejosoApellido2(limpiar(datos.getQuejosoApellido2()));
        queja.setQuejosoTipoUsuario(
                validar("tipo de usuario del quejoso",
                        datos.getQuejosoTipoUsuario(), TIPOS_USUARIO, true));

        queja.setDenunciadoTipoIdentificacion(
                validar("tipo de identificación del denunciado",
                        datos.getDenunciadoTipoIdentificacion(), TIPOS_IDENTIFICACION, true));
        queja.setDenunciadoNumeroIdentificacion(limpiar(datos.getDenunciadoNumeroIdentificacion()));
        queja.setDenunciadoNombre(limpiar(datos.getDenunciadoNombre()));
        queja.setDenunciadoApellido1(limpiar(datos.getDenunciadoApellido1()));
        queja.setDenunciadoApellido2(limpiar(datos.getDenunciadoApellido2()));
        queja.setDenunciadoTipoUsuario(
                validar("tipo de usuario del denunciado",
                        datos.getDenunciadoTipoUsuario(), TIPOS_USUARIO, true));

        queja.setCapturadoPor(capturadoPor);
        queja.setFechaCaptura(LocalDateTime.now());
        queja.setNotasCaptura(limpiar(datos.getNotasCaptura()));

        return quejaRepository.save(queja);
    }

    @Transactional
    public EvidenciaHistorica agregarEvidencia(String folio, MultipartFile archivo, String tipo) {

        QuejaHistorica queja = obtenerPorFolio(folio);

        if (archivo == null || archivo.isEmpty()) {
            throw new IllegalArgumentException("El archivo viene vacío.");
        }

        EvidenciaHistorica evidencia = new EvidenciaHistorica();
        evidencia.setNombreArchivo(archivo.getOriginalFilename());
        evidencia.setTipoMime(archivo.getContentType());
        evidencia.setTipo(mayusculas(tipo));
        evidencia.setTamanioBytes(archivo.getSize());
        try {
            evidencia.setContenido(archivo.getBytes());
        } catch (Exception ex) {
            throw new IllegalArgumentException("No se pudo leer el archivo: " + ex.getMessage());
        }

        queja.agregarEvidencia(evidencia);
        quejaRepository.save(queja);
        return evidencia;
    }

    // ----------------------------------------------------------------- consulta

    @Transactional(readOnly = true)
    public QuejaHistorica obtenerPorFolio(String folio) {
        return quejaRepository.findByFolio(folio)
                .orElseThrow(() -> new IllegalArgumentException(
                        "No existe una queja histórica con folio " + folio));
    }

    @Transactional(readOnly = true)
    public Page<QuejaHistorica> listar(String texto, String unidad, Pageable pageable) {
        // Cadena vacía = sin filtro (ver comentario en el repositorio).
        return quejaRepository.buscar(opcional(texto), opcional(mayusculas(unidad)), pageable);
    }

    /**
     * Antecedentes por identificación. La usa revision-service al abrir una queja nueva,
     * para unir estos resultados con los de defensoria_db y mostrarle al recepcionista un
     * solo historial.
     */
    @Transactional(readOnly = true)
    public List<QuejaHistoricaDTO> antecedentesPorIdentificacion(String tipo, String numero) {
        if (limpiar(numero) == null) {
            return List.of();
        }
        return quejaRepository
                .buscarAntecedentesPorIdentificacion(opcional(mayusculas(tipo)), limpiar(numero))
                .stream()
                .map(this::aDTO)
                .toList();
    }

    /** Respaldo cuando el quejoso no trae boleta ni número de empleado. */
    @Transactional(readOnly = true)
    public List<QuejaHistoricaDTO> antecedentesPorNombre(String nombre, String apellido1) {
        if (limpiar(nombre) == null || limpiar(apellido1) == null) {
            return List.of();
        }
        return quejaRepository.buscarAntecedentesPorNombre(limpiar(nombre), limpiar(apellido1))
                .stream()
                .map(this::aDTO)
                .toList();
    }

    /** Dataset completo en el formato del contrato, para entrenar/alimentar el modelo. */
    @Transactional(readOnly = true)
    public List<QuejaHistoricaDTO> exportarTodas() {
        return quejaRepository.findAll().stream().map(this::aDTO).toList();
    }

    @Transactional(readOnly = true)
    public List<EvidenciaResumenDTO> listarEvidencias(String folio) {
        QuejaHistorica queja = obtenerPorFolio(folio);
        return evidenciaRepository.findByQuejaHistoricaId(queja.getId()).stream()
                .map(ev -> EvidenciaResumenDTO.builder()
                        .id(ev.getId())
                        .nombreArchivo(ev.getNombreArchivo())
                        .tipoMime(ev.getTipoMime())
                        .tipo(ev.getTipo())
                        .tamanioBytes(ev.getTamanioBytes())
                        .fechaSubida(ev.getFechaSubida() != null
                                ? ev.getFechaSubida().format(ISO_FECHA_HORA) : null)
                        .build())
                .toList();
    }

    @Transactional(readOnly = true)
    public EvidenciaHistorica obtenerEvidencia(Long id) {
        return evidenciaRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("No existe la evidencia " + id));
    }

    // ------------------------------------------------------------------ mapeo

    /** Convierte al contrato de antecedentes — la misma forma que devuelve queja-service. */
    public QuejaHistoricaDTO aDTO(QuejaHistorica q) {
        return QuejaHistoricaDTO.builder()
                .folio(q.getFolio())
                .origen(ORIGEN)
                .fuente(q.getFuente())
                // fechaRegistro = cuándo se presentó en su momento, NO cuándo se capturó.
                .fechaRegistro(q.getFechaPresentacionOriginal() != null
                        ? q.getFechaPresentacionOriginal().atStartOfDay().format(ISO_FECHA_HORA)
                        : null)
                .fechaHechos(q.getFechaHechos() != null ? q.getFechaHechos().toString() : null)
                .unidadAcademicaClave(q.getUnidadAcademicaClave())
                .motivo(q.getMotivo())
                .descripcion(q.getDescripcion())
                .estatus(q.getEstatus())
                .resultado(q.getResultado())
                .quejoso(PersonaDTO.builder()
                        .tipoIdentificacion(q.getQuejosoTipoIdentificacion())
                        .numeroIdentificacion(q.getQuejosoNumeroIdentificacion())
                        .nombre(q.getQuejosoNombre())
                        .apellido1(q.getQuejosoApellido1())
                        .apellido2(q.getQuejosoApellido2())
                        .tipoUsuario(q.getQuejosoTipoUsuario())
                        .build())
                .denunciado(PersonaDTO.builder()
                        .tipoIdentificacion(q.getDenunciadoTipoIdentificacion())
                        .numeroIdentificacion(q.getDenunciadoNumeroIdentificacion())
                        .nombre(q.getDenunciadoNombre())
                        .apellido1(q.getDenunciadoApellido1())
                        .apellido2(q.getDenunciadoApellido2())
                        .tipoUsuario(q.getDenunciadoTipoUsuario())
                        .build())
                .build();
    }

    // ------------------------------------------------------------------ apoyo

    private String generarFolioSinOriginal() {
        long consecutivo = quejaRepository.countByFolioGeneradoTrue() + 1;
        String candidato = PREFIJO_SIN_FOLIO + String.format("%06d", consecutivo);
        // Por si alguien borró un registro y el conteo ya no alcanza para ser único.
        while (quejaRepository.existsByFolio(candidato)) {
            consecutivo++;
            candidato = PREFIJO_SIN_FOLIO + String.format("%06d", consecutivo);
        }
        return candidato;
    }

    private String limpiar(String valor) {
        if (valor == null) {
            return null;
        }
        String limpio = valor.trim();
        return limpio.isEmpty() ? null : limpio;
    }

    /** null -> "" para los parámetros de consulta que significan "sin filtro". */
    private String opcional(String valor) {
        String limpio = limpiar(valor);
        return limpio == null ? "" : limpio;
    }

    private String mayusculas(String valor) {
        String limpio = limpiar(valor);
        return limpio == null ? null : limpio.toUpperCase();
    }

    /**
     * Cierra los catálogos. Sin esto llegarían "Boleta", "boleta" y "BOLETA ESCOLAR" como
     * tres categorías distintas al modelo de antecedentes.
     */
    private String validar(String campo, String valor, Set<String> permitidos, boolean opcional) {
        String normalizado = mayusculas(valor);
        if (normalizado == null) {
            if (opcional) {
                return null;
            }
            throw new IllegalArgumentException("Falta " + campo + ".");
        }
        if (!permitidos.contains(normalizado)) {
            throw new IllegalArgumentException(
                    "Valor no válido para " + campo + ": '" + valor + "'. Permitidos: "
                            + String.join(", ", permitidos.stream().sorted().toList()) + ".");
        }
        return normalizado;
    }
}
