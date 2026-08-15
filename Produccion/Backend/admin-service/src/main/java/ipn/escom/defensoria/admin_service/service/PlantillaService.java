package ipn.escom.defensoria.admin_service.service;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import ipn.escom.defensoria.admin_service.entity.PlantillaDocumento;
import ipn.escom.defensoria.admin_service.repository.PlantillaDocumentoRepository;
import jakarta.annotation.PostConstruct;

@Service
public class PlantillaService {

    @Autowired
    private PlantillaDocumentoRepository repository;

    /** Datos de ejemplo usados solo para la previsualización -- nunca se guardan. */
    private static final Map<String, String> DATOS_EJEMPLO = Map.of(
            "folio_queja", "DDP-2026-0421",
            "nombre_quejoso", "Juan Pérez García",
            "nombre_subdefensor", "Mtra. María López",
            "nombre_denunciado", "N/A",
            "unidad_academica", "ESCOM",
            "fecha_actual", java.time.LocalDate.now().toString());

    @PostConstruct
    public void sembrarPlantillasBase() {
        if (repository.count() > 0) {
            return;
        }
        crearSiNoExiste("OFICIO_SOLICITUD_INFORMACION", "Oficio de Solicitud de Información",
                "Por medio de la presente, se solicita información sobre el expediente "
                        + "{folio_queja} del quejoso {nombre_quejoso}, relacionado con hechos "
                        + "ocurridos en {unidad_academica}.\n\n"
                        + "Se solicita dar respuesta a la brevedad posible.\n\n"
                        + "Atentamente,\n{nombre_subdefensor}");
        crearSiNoExiste("OFICIO_NOTIFICACION_RESOLUCION", "Oficio de Notificación de Resolución",
                "Por medio del presente oficio se notifica al quejoso {nombre_quejoso} la "
                        + "resolución correspondiente al expediente {folio_queja}, con fecha "
                        + "{fecha_actual}.\n\nAtentamente,\n{nombre_subdefensor}");
        crearSiNoExiste("ACUERDO_CONCILIACION", "Acuerdo de Conciliación",
                "En la Ciudad de México, siendo {fecha_actual}, comparecen las partes "
                        + "involucradas en el expediente {folio_queja} para efectos de "
                        + "conciliación entre {nombre_quejoso} y {nombre_denunciado}.\n\n"
                        + "Atentamente,\n{nombre_subdefensor}");
    }

    private void crearSiNoExiste(String tipo, String nombre, String contenido) {
        if (repository.findByTipo(tipo).isPresent()) {
            return;
        }
        PlantillaDocumento p = new PlantillaDocumento();
        p.setTipo(tipo);
        p.setNombre(nombre);
        p.setContenido(contenido);
        p.setActiva(true);
        repository.save(p);
    }

    public List<PlantillaDocumento> listar() {
        return repository.findAllByOrderByNombreAsc();
    }

    public PlantillaDocumento obtener(String tipo) {
        return repository.findByTipo(tipo)
                .orElseThrow(() -> new RuntimeException("No existe una plantilla de tipo " + tipo));
    }

    public PlantillaDocumento actualizar(String tipo, String contenido, String actualizadoPor) {
        PlantillaDocumento plantilla = obtener(tipo);
        plantilla.setContenido(contenido);
        plantilla.setActualizadoEn(LocalDateTime.now());
        plantilla.setActualizadoPor(actualizadoPor);
        return repository.save(plantilla);
    }

    /** Rellena los placeholders {clave} con datos de ejemplo para la vista previa -- ver
     * mockup "Ver Previsualización PDF" (aquí se regresa como texto/HTML, no PDF real). */
    public String previsualizar(String tipo) {
        String contenido = obtener(tipo).getContenido();
        String resultado = contenido;
        for (Map.Entry<String, String> dato : DATOS_EJEMPLO.entrySet()) {
            resultado = resultado.replace("{" + dato.getKey() + "}", dato.getValue());
        }
        return resultado;
    }

    public long contarActivas() {
        return repository.countByActivaTrue();
    }

    public Map<String, String> placeholdersDisponibles() {
        Map<String, String> mapa = new LinkedHashMap<>();
        mapa.put("folio_queja", "Folio de la queja");
        mapa.put("nombre_quejoso", "Nombre completo del quejoso");
        mapa.put("nombre_denunciado", "Nombre de la persona denunciada");
        mapa.put("nombre_subdefensor", "Nombre de quien firma el oficio");
        mapa.put("unidad_academica", "Unidad académica involucrada");
        mapa.put("fecha_actual", "Fecha en que se genera el documento");
        return mapa;
    }
}
