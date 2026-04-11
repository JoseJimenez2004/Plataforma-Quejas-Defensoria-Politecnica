package ipn.escom.defensoria.recepcionista.service;

import ipn.escom.defensoria.recepcionista.dto.*;
import ipn.escom.defensoria.recepcionista.entity.HistorialEstatusEntity;
import ipn.escom.defensoria.recepcionista.entity.PerfilQuejosoEntity;
import ipn.escom.defensoria.recepcionista.entity.QuejaEntity;
import ipn.escom.defensoria.recepcionista.entity.cat.CatEstatusQueja;
import ipn.escom.defensoria.recepcionista.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Implementación del servicio de quejas para el módulo Recepcionista.
 *
 * Aquí vive toda la lógica de negocio: consultas a la BD, cambios de estatus,
 * registro en historial y mapeo de entidades a DTOs.
 *
 * @Service indica a Spring que esta clase es un componente de servicio
 * y la registra para inyección de dependencias.
 *
 * NOTA IMPORTANTE: USUARIO_SISTEMA_ID está hardcodeado como 1 porque aún
 * no existe el módulo de autenticación del personal. Cuando se implemente
 * el login de la recepcionista, este valor deberá obtenerse del token de sesión.
 */
@Service
public class QuejaServiceImpl implements IQuejaService {

    // ID del usuario recepcionista esta como 1 hasta que exista el módulo de auth
    private static final int USUARIO_SISTEMA_ID = 1;

    @Autowired
    private QuejaRepository quejaRepository;

    @Autowired
    private PerfilQuejosoRepository perfilQuejosoRepository;

    @Autowired
    private HistorialEstatusRepository historialEstatusRepository;

    @Autowired
    private DocumentoRepository documentoRepository;

    // Necesario para buscar los estatus del catálogo por nombre (ej. "EN_REVISION")
    @Autowired
    private CatEstatusQuejaRepository catEstatusQuejaRepository;

    // =========================================================================
    // DASHBOARD
    // =========================================================================

    /**
     * Consulta las quejas con estatus "RECIBIDA" usando el método derivado
     * findByEstatus_Nombre del repository (Spring genera el SQL automáticamente).
     * Cada queja se convierte a QuejaResumenDTO con el método privado mapearAResumen().
     */
    @Override
    public List<QuejaResumenDTO> listarQuejasPendientes() {
        return quejaRepository.findByEstatus_Nombre("RECIBIDA")
                .stream()
                .map(this::mapearAResumen)
                .collect(Collectors.toList());
    }

    // =========================================================================
    // VALIDACIÓN DE REQUISITOS
    // =========================================================================

    /**
     * Busca la queja por ID, obtiene sus documentos adjuntos de la tabla documentos,
     * verifica si los campos obligatorios están completos y construye el DTO de detalle.
     *
     * Los "campos obligatorios" verificados son:
     *   - tieneDescripcion: descripcion_hechos no está vacío
     *   - tieneTipoQueja: tiene tema asignado
     *   - tieneUbicacion: tiene tipo_documento asignado
     */
    @Override
    public QuejaDetalleDTO obtenerDetalle(Long id) {
        QuejaEntity queja = buscarQuejaPorId(id);

        // Obtiene los nombres de los archivos adjuntos asociados a esta queja
        List<String> documentos = documentoRepository.findByQuejaId(id)
                .stream()
                .map(d -> d.getNombreOriginal())
                .collect(Collectors.toList());

        boolean tieneDescripcion = queja.getDescripcionHechos() != null && !queja.getDescripcionHechos().isBlank();
        boolean tieneTema = queja.getTema() != null;
        boolean tieneTipoDoc = queja.getTipoDocumento() != null;
        boolean documentacionCompleta = tieneDescripcion && tieneTema && tieneTipoDoc;

        return QuejaDetalleDTO.builder()
                .id(queja.getId())
                .noFolio(queja.getNoFolio())
                .fechaRecepcion(queja.getFechaRecepcion())
                .nombreQuejoso(queja.getQuejoso().getNombreCompleto())
                .boletaEmpleado(queja.getQuejoso().getBoletaEmpleado())
                .descripcionHechos(queja.getDescripcionHechos())
                .tema(tieneTema ? queja.getTema().getNombre() : null)
                .tipoDocumento(tieneTipoDoc ? queja.getTipoDocumento().getNombre() : null)
                .tieneDescripcion(tieneDescripcion)
                .tieneTipoQueja(tieneTema)
                .tieneUbicacion(tieneTipoDoc)
                .documentosAdjuntos(documentos)
                .estatusDocumentacion(documentacionCompleta ? "Documentación Completa" : "Documentación Incompleta")
                .build();
    }

    // =========================================================================
    // ACEPTAR QUEJA
    // =========================================================================

    /**
     * @Transactional garantiza que si algo falla (ej. al guardar el historial),
     * se revierte también el cambio de estatus en la queja. Ambas operaciones
     * se ejecutan como una sola transacción atómica.
     */
    @Override
    @Transactional
    public QuejaResumenDTO aceptarQueja(Long id) {
        QuejaEntity queja = buscarQuejaPorId(id);
        CatEstatusQueja nuevoEstatus = buscarEstatus("EN_REVISION");

        queja.setEstatus(nuevoEstatus);
        queja.setFechaActualizacion(LocalDateTime.now());
        quejaRepository.save(queja);

        registrarHistorial(id, nuevoEstatus, "Queja aceptada por recepcionista.", null);

        return mapearAResumen(queja);
    }

    // =========================================================================
    // RECHAZAR QUEJA
    // =========================================================================

    /**
     * Cambia el estatus a "ATENDIDA" (el catálogo no tiene "RECHAZADA" aún)
     * y guarda el motivo como comentario en el historial para trazabilidad.
     */
    @Override
    @Transactional
    public QuejaResumenDTO rechazarQueja(Long id, RechazarQuejaRequest request) {
        QuejaEntity queja = buscarQuejaPorId(id);
        CatEstatusQueja nuevoEstatus = buscarEstatus("ATENDIDA");

        queja.setEstatus(nuevoEstatus);
        queja.setFechaActualizacion(LocalDateTime.now());
        quejaRepository.save(queja);

        registrarHistorial(id, nuevoEstatus, "Queja rechazada: " + request.getMotivoRechazo(), null);

        return mapearAResumen(queja);
    }

    // =========================================================================
    // NOTIFICAR RECHAZO
    // =========================================================================

    /**
     * Por ahora solo registra en el historial que se notificó al quejoso.
     * TODO: Cuando se integre el módulo de correo, aquí se llamará a
     * JavaMailSender para enviar el correo al quejoso con el motivo de rechazo.
     */
    @Override
    public String notificarRechazo(Long id, RechazarQuejaRequest request) {
        QuejaEntity queja = buscarQuejaPorId(id);
        CatEstatusQueja estatusActual = queja.getEstatus();
        registrarHistorial(id, estatusActual,
                "Notificación de rechazo enviada al quejoso: " + request.getMotivoRechazo(), null);

        return "Notificación de rechazo registrada para la queja " + queja.getNoFolio();
    }

    // =========================================================================
    // CANALIZACIÓN
    // =========================================================================

    /**
     * Registra la opción de canalización elegida por la recepcionista en el historial
     * y mueve la queja a EN_REVISION para que un abogado la tome.
     * Las notas adicionales son opcionales.
     */
    @Override
    @Transactional
    public QuejaResumenDTO canalizar(Long id, CanarizarRequest request) {
        QuejaEntity queja = buscarQuejaPorId(id);
        CatEstatusQueja nuevoEstatus = buscarEstatus("EN_REVISION");

        queja.setEstatus(nuevoEstatus);
        queja.setFechaActualizacion(LocalDateTime.now());
        quejaRepository.save(queja);

        String comentario = "Canalización: " + request.getOpcionCanalizacion();
        if (request.getNotasAdicionales() != null && !request.getNotasAdicionales().isBlank()) {
            comentario += " | Notas: " + request.getNotasAdicionales();
        }
        registrarHistorial(id, nuevoEstatus, comentario, null);

        return mapearAResumen(queja);
    }

    // =========================================================================
    // BÚSQUEDA DE ANTECEDENTES
    // =========================================================================

    /**
     * Primero busca todos los perfiles de quejosos cuyo nombre o apellidos
     * contengan el texto buscado (búsqueda parcial, sin importar mayúsculas).
     * Luego, para cada perfil encontrado, obtiene todas sus quejas históricas
     * y las aplana en una sola lista de resultados.
     */
    @Override
    public List<QuejosoBusquedaResultDTO> buscarAntecedentes(String nombre) {
        List<PerfilQuejosoEntity> perfiles = perfilQuejosoRepository.buscarPorNombre(nombre);

        return perfiles.stream()
                .flatMap(perfil -> quejaRepository.findByQuejosoId(perfil.getId()).stream())
                .map(queja -> QuejosoBusquedaResultDTO.builder()
                        .idQueja(queja.getNoFolio())
                        .fecha(queja.getFechaRecepcion())
                        .estatus(queja.getEstatus().getNombre())
                        .build())
                .collect(Collectors.toList());
    }

    // =========================================================================
    // MÉTODOS PRIVADOS DE APOYO
    // =========================================================================

    /**
     * Busca una queja por ID. Si no existe, lanza RuntimeException
     * que el GlobalExceptionHandler convierte en respuesta 400 BAD REQUEST.
     */
    private QuejaEntity buscarQuejaPorId(Long id) {
        return quejaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Queja no encontrada con id: " + id));
    }

    /**
     * Busca un estatus del catálogo por nombre exacto.
     * Los nombres válidos son los insertados en init.sql: RECIBIDA, EN_REVISION, ATENDIDA.
     * Si no existe, lanza RuntimeException — indica que el init.sql no tiene ese valor.
     */
    private CatEstatusQueja buscarEstatus(String nombre) {
        return catEstatusQuejaRepository.findByNombre(nombre)
                .orElseThrow(() -> new RuntimeException("Estatus no encontrado: " + nombre));
    }

    /**
     * Registra un cambio de estatus en historial_estatus_queja.
     * Se llama en cada operación que modifica el estatus de una queja.
     * Si usuarioId es null, usa USUARIO_SISTEMA_ID (1) como valor temporal.
     */
    private void registrarHistorial(Long quejaId, CatEstatusQueja estatus, String comentarios, Integer usuarioId) {
        HistorialEstatusEntity historial = HistorialEstatusEntity.builder()
                .quejaId(quejaId)
                .estatus(estatus)
                .usuarioModificoId(usuarioId != null ? usuarioId : USUARIO_SISTEMA_ID)
                .comentarios(comentarios)
                .fechaCambio(LocalDateTime.now())
                .build();
        historialEstatusRepository.save(historial);
    }

    /**
     * Convierte una QuejaEntity al DTO resumido para el dashboard.
     * documentacionCompleta es true solo si los 3 campos obligatorios están presentes:
     * descripcion_hechos, tema y tipo_documento.
     */
    private QuejaResumenDTO mapearAResumen(QuejaEntity queja) {
        boolean documentacionCompleta = queja.getDescripcionHechos() != null
                && !queja.getDescripcionHechos().isBlank()
                && queja.getTema() != null
                && queja.getTipoDocumento() != null;

        return QuejaResumenDTO.builder()
                .id(queja.getId())
                .noFolio(queja.getNoFolio())
                .fechaRecepcion(queja.getFechaRecepcion())
                .nombreQuejoso(queja.getQuejoso().getNombreCompleto())
                .documentacionCompleta(documentacionCompleta)
                .estatus(queja.getEstatus().getNombre())
                .build();
    }
}
