package ipn.escom.defensoria.quejoso.service;

import ipn.escom.defensoria.quejoso.entity.EstatusQuejaEntity;
import ipn.escom.defensoria.quejoso.entity.EvidenciaEntity;
import ipn.escom.defensoria.quejoso.entity.Queja;
import ipn.escom.defensoria.quejoso.entity.Usuario;
import ipn.escom.defensoria.quejoso.dto.QuejaEditarDTO;
import ipn.escom.defensoria.quejoso.dto.QuejaFiltroDTO;
import ipn.escom.defensoria.quejoso.repository.EvidenciaRepository;
import ipn.escom.defensoria.quejoso.repository.QuejaRepository;
import ipn.escom.defensoria.quejoso.repository.UsuarioRepository;
import ipn.escom.defensoria.quejoso.dto.QuejaSeguimientoDTO;
import ipn.escom.defensoria.quejoso.dto.TramitesResumenDTO;
import ipn.escom.defensoria.quejoso.dto.EvidenciaDTO;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.data.domain.PageRequest; // Importar esto


import java.time.Year;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.List;

@Service
public class QuejaService {

    @Autowired
    private QuejaRepository quejaRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private EvidenciaRepository evidenciaRepository;

    // En QuejaService.java
    public Queja registrarQueja(Queja nuevaQueja, String identificacion, String correo) {
        // 1. Generar Folio Automático
        nuevaQueja.setFolio(generarNuevoFolio());

        // 2. VINCULACIÓN AUTOMÁTICA (Regla: Boleta O Correo)
        // Buscamos primero por boleta
        Optional<Usuario> usuarioExistente = usuarioRepository.findByBoleta(identificacion);

        // Si no se encuentra por boleta, intentamos buscar por correo institucional
        if (usuarioExistente.isEmpty()) {
            usuarioExistente = usuarioRepository.findByCorreoInstitucional(correo);
        }

        // Si se encontró por cualquiera de los dos medios, vinculamos la queja al usuario
        if (usuarioExistente.isPresent()) {
            nuevaQueja.setQuejoso(usuarioExistente.get());
        }
        // Si no existe, los datos ya están en 'nuevaQueja' por el mapeo previo del controlador

        // 3. Guardar en BD
        return quejaRepository.save(nuevaQueja);
    }

    public Optional<QuejaSeguimientoDTO> obtenerSeguimiento(String folio) {
        return quejaRepository.findByFolio(folio)
                .map(queja -> QuejaSeguimientoDTO.builder()
                        .folio(queja.getFolio())
                        .fechaRegistro(queja.getFechaRegistro())
                        .asunto(queja.getAsunto())
                        .descripcionHechos(queja.getDescripcion())
                        .estatusActual(queja.getEstatus())
                        .evidencias(queja.getEvidencias().stream()
                                .map(e -> new EvidenciaDTO(e.getNombreArchivo(), e.getUrlAlmacenamiento()))
                                .collect(Collectors.toList()))
                        .build());
    }

    public Optional<QuejaSeguimientoDTO> obtenerSeguimiento(String folio, String correo) {
        return quejaRepository.findByFolio(folio)
                .map(queja -> QuejaSeguimientoDTO.builder()
                        .folio(queja.getFolio())
                        .fechaRegistro(queja.getFechaRegistro())
                        .asunto(queja.getAsunto())
                        .descripcionHechos(queja.getDescripcion())
                        .estatusActual(queja.getEstatus())
                        .evidencias(queja.getEvidencias().stream()
                                .map(e -> new EvidenciaDTO(e.getNombreArchivo(), e.getUrlAlmacenamiento()))
                                .collect(Collectors.toList()))
                        .build());
    }

    private String generarNuevoFolio() {
        int year = Year.now().getValue();
        String prefijo = "DDP-" + year + "-";

        // CAMBIO AQUÍ: Usamos PageRequest.of(0, 1) para pedir solo el primer resultado (el más alto)
        List<String> resultados = quejaRepository.findLastFolioByYear(prefijo + "%", PageRequest.of(0, 1));

        // Si la lista está vacía, es el primer folio del año
        String lastFolio = resultados.isEmpty() ? null : resultados.get(0);

        if (lastFolio == null) {
            return prefijo + "0001";
        }

        // Extraer el número y sumar 1
        int consecutivo = Integer.parseInt(lastFolio.substring(prefijo.length())) + 1;
        return prefijo + String.format("%04d", consecutivo);
    }

    // Aquí iría la lógica de validación de los 30MB de archivos
    public void validarYGuardarEvidencias(Queja queja, List<MultipartFile> archivos) {
        if (archivos == null || archivos.isEmpty()) return;

        // 1. Validar el tamaño total (Regla de 30MB)
        long tamanoTotalBytes = archivos.stream()
                .mapToLong(MultipartFile::getSize)
                .sum();

        long limiteMaximo = 30 * 1024 * 1024; // 30MB en bytes

        if (tamanoTotalBytes > limiteMaximo) {
            throw new RuntimeException("El tamaño total de los archivos excede el límite de 30MB.");
        }

        // 2. Procesar cada archivo
        for (MultipartFile archivo : archivos) {
            // En un entorno real, aquí llamarías a un S3 o Google Cloud Storage
            // Por ahora simulamos la ruta como String en BD como acordamos
            String urlSimulada = "/storage/evidencias/" + queja.getFolio() + "_" + archivo.getOriginalFilename();

            EvidenciaEntity evidencia = new EvidenciaEntity();
            evidencia.setNombreArchivo(archivo.getOriginalFilename());
            evidencia.setUrlAlmacenamiento(urlSimulada);
            evidencia.setTamano(archivo.getSize());
            evidencia.setQueja(queja);

            // Guardar relación (se puede hacer via Cascade en Queja o directo)
            queja.getEvidencias().add(evidencia);
        }
    }


    public TramitesResumenDTO obtenerResumenDashboard(Long usuarioId) {
        return TramitesResumenDTO.builder()
                .totales(quejaRepository.countByQuejosoId(usuarioId))
                .enProceso(quejaRepository.countEnProcesoByUsuarioId(usuarioId))
                .finalizadas(quejaRepository.countFinalizadasByUsuarioId(usuarioId))
                .build();
    }

    public List<QuejaSeguimientoDTO> obtenerHistorial(Long usuarioId) {
        return quejaRepository.findByQuejosoIdOrderByFechaRegistroDesc(usuarioId)
                .stream()
                .map(q -> QuejaSeguimientoDTO.builder()
                        .folio(q.getFolio())
                        .fechaRegistro(q.getFechaRegistro())
                        .asunto(q.getAsunto())
                        .estatusActual(q.getEstatus())
                        .build())
                .collect(Collectors.toList());
    }

    public void eliminarQueja(String folio, Long usuarioId) {
        Queja queja = quejaRepository.findByFolio(folio)
                .orElseThrow(() -> new RuntimeException("Queja no encontrada"));

        // 1. Validar que la queja tenga un usuario vinculado
        // Si es nulo, significa que la cuenta no ha sido activada y no puede ser borrada desde el historial
        if (queja.getQuejoso() == null) {
            throw new RuntimeException("Esta queja aún no está vinculada a una cuenta activa.");
        }

        // 2. Regla de seguridad: solo el dueño puede borrar
        if (!queja.getQuejoso().getId().equals(usuarioId)) {
            throw new RuntimeException("No tiene permisos sobre esta queja");
        }

        // 3. Regla de negocio: solo si está en estatus RECIBIDA
        if (queja.getEstatus() != EstatusQuejaEntity.RECIBIDA) {
            throw new RuntimeException("No se puede eliminar una queja que ya está en proceso");
        }

        quejaRepository.delete(queja);
    }
    // En QuejaService.java

    public void eliminarEvidencia(Long evidenciaId, String folio) {
        Queja queja = quejaRepository.findByFolio(folio)
                .orElseThrow(() -> new RuntimeException("Queja no encontrada"));

        if (queja.getEstatus() != EstatusQuejaEntity.RECIBIDA) {
            throw new RuntimeException("Solo se pueden eliminar evidencias en estatus 'RECIBIDA'");
        }

        // Lógica para borrar el archivo del storage y de la base de datos
        evidenciaRepository.deleteById(evidenciaId);
    }

    @Transactional
    public void editarQueja(String folio, Long usuarioId, QuejaEditarDTO dto, List<MultipartFile> nuevasEvidencias) {
        // 1. Buscar la queja y validar propiedad
        Queja queja = quejaRepository.findByFolio(folio)
                .orElseThrow(() -> new RuntimeException("Queja no encontrada"));

        if (queja.getQuejoso() == null || !queja.getQuejoso().getId().equals(usuarioId)) {
            throw new RuntimeException("No tiene permisos para editar esta queja");
        }

        // 2. VALIDACIÓN CRÍTICA: Solo si es RECIBIDA
        if (queja.getEstatus() != EstatusQuejaEntity.RECIBIDA) {
            throw new RuntimeException("La queja ya está en proceso y no puede ser editada");
        }

        // 3. Actualizar campos básicos
        queja.setAsunto(dto.getAsunto());
        queja.setDescripcion(dto.getDescripcionHechos());

        // 4. Gestionar eliminación de evidencias viejas
        if (dto.getEvidenciasBorrarIds() != null) {
            for (Long idEvidencia : dto.getEvidenciasBorrarIds()) {
                eliminarEvidencia(idEvidencia, folio);
            }
        }

        // 5. Agregar nuevas evidencias (si hay) validando los 30MB totales
        if (nuevasEvidencias != null && !nuevasEvidencias.isEmpty()) {
            validarYGuardarEvidencias(queja, nuevasEvidencias);
        }

        quejaRepository.save(queja);
    }

    // En QuejaService.java

    // En QuejaService.java

    public List<QuejaSeguimientoDTO> obtenerHistorialFiltrado(Long usuarioId, QuejaFiltroDTO filtro) {
        // 1. VALIDACIÓN DE SEGURIDAD:
        // Si el usuario no existe en la base de datos, retornamos lista vacía de inmediato.
        if (!usuarioRepository.existsById(usuarioId)) {
            return java.util.Collections.emptyList();
        }

        // 2. Obtener las quejas (Solo traerá las que pertenecen al ID si el repositorio está bien)
        List<Queja> quejas = quejaRepository.findByQuejosoIdOrderByFechaRegistroDesc(usuarioId);

        // 3. Aplicamos los filtros dinámicamente sobre los resultados del usuario
        return quejas.stream()
                .filter(q -> filtro.getFolio() == null || q.getFolio().contains(filtro.getFolio()))
                .filter(q -> filtro.getEstatus() == null || q.getEstatus().equals(filtro.getEstatus()))
                .filter(q -> filtro.getFechaInicio() == null || !q.getFechaRegistro().toLocalDate().isBefore(filtro.getFechaInicio()))
                .filter(q -> filtro.getFechaFin() == null || !q.getFechaRegistro().toLocalDate().isAfter(filtro.getFechaFin()))
                .map(q -> QuejaSeguimientoDTO.builder()
                        .folio(q.getFolio())
                        .fechaRegistro(q.getFechaRegistro())
                        .asunto(q.getAsunto())
                        .estatusActual(q.getEstatus())
                        .build())
                .collect(Collectors.toList());
    }
}