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
import ipn.escom.defensoria.quejoso.storage.StorageService;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.data.domain.PageRequest; // Importar esto


import java.time.LocalDate;
import java.time.LocalDateTime;
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

    @Autowired
    private StorageService storageService;

    public Queja registrarQueja(Queja nuevaQueja, String identificacion, String correo, List<MultipartFile> archivos) {
        // Validar fechaHechos: no futura, no mayor a 90 días
        LocalDateTime fechaHechos = nuevaQueja.getFechaHechos();
        if (fechaHechos != null) {
            LocalDate hoy = LocalDate.now();
            LocalDate fechaHechosFecha = fechaHechos.toLocalDate();
            if (fechaHechosFecha.isAfter(hoy)) {
                throw new RuntimeException("La fecha de los hechos no puede ser una fecha futura.");
            }
            if (fechaHechosFecha.isBefore(hoy.minusDays(90))) {
                throw new RuntimeException("La fecha de los hechos no puede ser mayor a 90 días anteriores.");
            }
        }

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

        // 3. Guardar evidencias en disco usando el folio ya generado
        if (archivos != null && !archivos.isEmpty()) {
            validarYGuardarEvidencias(nuevaQueja, archivos);
        }

        // 4. Guardar en BD (las evidencias se persisten por cascade)
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
                                .map(e -> new EvidenciaDTO(e.getId(), e.getNombreArchivo(), null))
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
                                .map(e -> new EvidenciaDTO(e.getId(), e.getNombreArchivo(), null))
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

    public void validarYGuardarEvidencias(Queja queja, List<MultipartFile> archivos) {
        if (archivos == null || archivos.isEmpty()) return;

        // Validar que el total no supere 30MB
        long total = archivos.stream().mapToLong(MultipartFile::getSize).sum();
        if (total > 30 * 1024 * 1024) {
            throw new RuntimeException("El tamaño total de los archivos excede el límite de 30MB.");
        }

        for (MultipartFile archivo : archivos) {
            // Delegar el guardado físico al StorageService (local ahora, nube después)
            String clave = storageService.guardar(archivo, queja.getFolio());

            EvidenciaEntity evidencia = new EvidenciaEntity();
            evidencia.setNombreArchivo(archivo.getOriginalFilename());
            evidencia.setUrlAlmacenamiento(clave); // clave retornada por StorageService
            evidencia.setTamano(archivo.getSize());
            evidencia.setTipoContenido(archivo.getContentType()); // MIME para servir con Content-Type correcto
            evidencia.setQueja(queja);

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

        // 1. Validar vinculación
        if (queja.getQuejoso() == null) {
            throw new RuntimeException("Esta queja aún no está vinculada a una cuenta activa.");
        }

        // 2. Regla de seguridad: el dueño es el único autorizado
        if (!queja.getQuejoso().getId().equals(usuarioId)) {
            throw new RuntimeException("No tiene permisos sobre esta queja");
        }

        // 3. Regla de negocio: solo se permite "cancelar" si no ha sido atendida
        if (queja.getEstatus() != EstatusQuejaEntity.RECIBIDA) {
            throw new RuntimeException("No se puede eliminar una queja que ya está en proceso o finalizada");
        }

        // --- CAMBIO CLAVE AQUÍ ---
        // En lugar de eliminar el registro de la BD, cambiamos su estatus
        // Esto mantiene el folio en la historia pero lo "apaga" para el usuario
        queja.setEstatus(EstatusQuejaEntity.CANCELADA);

        quejaRepository.save(queja);
    }
    // En QuejaService.java

    public void eliminarEvidencia(Long evidenciaId, String folio) {
        Queja queja = quejaRepository.findByFolio(folio)
                .orElseThrow(() -> new RuntimeException("Queja no encontrada"));

        if (queja.getEstatus() != EstatusQuejaEntity.RECIBIDA) {
            throw new RuntimeException("Solo se pueden eliminar evidencias en estatus 'RECIBIDA'");
        }

        // Borrar el archivo físico antes de eliminar el registro en BD
        evidenciaRepository.findById(evidenciaId).ifPresent(ev -> {
            if (ev.getUrlAlmacenamiento() != null) {
                storageService.eliminar(ev.getUrlAlmacenamiento());
            }
        });

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

    public void procesarQuejaPorRecepcion(String folio, Long adminId) {
        Queja queja = quejaRepository.findByFolio(folio)
                .orElseThrow(() -> new RuntimeException("Queja no encontrada"));

        // --- EL BLOQUEO ---
        if (queja.getEstatus() == EstatusQuejaEntity.CANCELADA) {
            throw new RuntimeException("Operación denegada: La queja fue cancelada por el ciudadano y no puede ser modificada.");
        }

        // Si no está cancelada, continúa la lógica normal...
    }
}