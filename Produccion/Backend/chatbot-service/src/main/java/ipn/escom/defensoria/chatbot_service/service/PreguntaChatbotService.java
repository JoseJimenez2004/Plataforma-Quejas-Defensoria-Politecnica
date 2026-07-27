package ipn.escom.defensoria.chatbot_service.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import ipn.escom.defensoria.chatbot_service.dto.PreguntaChatbotRequest;
import ipn.escom.defensoria.chatbot_service.entity.PreguntaChatbot;
import ipn.escom.defensoria.chatbot_service.model.CategoriaChatbotModel;
import ipn.escom.defensoria.chatbot_service.model.PreguntaChatbotModel;
import ipn.escom.defensoria.chatbot_service.repository.PreguntaChatbotRepository;

@Service
public class PreguntaChatbotService {

    @Autowired
    private PreguntaChatbotRepository preguntaChatbotRepository;

    /**
     * Menú completo del mini-chat para el portal del quejoso: categorías en el orden en que
     * aparece su primera pregunta, y dentro de cada una, las preguntas ordenadas por
     * "orden". Se manda todo en una sola llamada (el catálogo es pequeño) para que el
     * frontend no tenga que pedir cada respuesta por separado al hacer clic.
     */
    public List<CategoriaChatbotModel> obtenerMenuPublico() {
        List<PreguntaChatbot> activas = preguntaChatbotRepository.findByActivoTrueOrderByOrdenAsc();

        Map<String, List<PreguntaChatbotModel>> agrupadas = new LinkedHashMap<>();
        for (PreguntaChatbot pregunta : activas) {
            agrupadas
                .computeIfAbsent(pregunta.getCategoria(), k -> new ArrayList<>())
                .add(new PreguntaChatbotModel(pregunta.getId(), pregunta.getPregunta(), pregunta.getRespuesta()));
        }

        List<CategoriaChatbotModel> menu = new ArrayList<>();
        agrupadas.forEach((categoria, preguntas) -> menu.add(new CategoriaChatbotModel(categoria, preguntas)));
        return menu;
    }

    /** Lista TODAS (activas e inactivas) -- usado por el CRUD de administración. */
    public List<PreguntaChatbot> listarTodas() {
        return preguntaChatbotRepository.findAllByOrderByOrdenAsc();
    }

    public PreguntaChatbot crear(PreguntaChatbotRequest datos) {
        if (esVacio(datos.getCategoria()) || esVacio(datos.getPregunta()) || esVacio(datos.getRespuesta())) {
            throw new RuntimeException("Categoría, pregunta y respuesta son obligatorias.");
        }
        PreguntaChatbot pregunta = new PreguntaChatbot();
        aplicarDatos(pregunta, datos);
        pregunta.setCreadoEn(LocalDateTime.now());
        pregunta.setActualizadoEn(LocalDateTime.now());
        return preguntaChatbotRepository.save(pregunta);
    }

    public PreguntaChatbot editar(Long id, PreguntaChatbotRequest datos) {
        PreguntaChatbot pregunta = preguntaChatbotRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("No se encontró la pregunta indicada."));
        aplicarDatos(pregunta, datos);
        pregunta.setActualizadoEn(LocalDateTime.now());
        return preguntaChatbotRepository.save(pregunta);
    }

    public void eliminar(Long id) {
        if (!preguntaChatbotRepository.existsById(id)) {
            throw new RuntimeException("No se encontró la pregunta indicada.");
        }
        preguntaChatbotRepository.deleteById(id);
    }

    private void aplicarDatos(PreguntaChatbot pregunta, PreguntaChatbotRequest datos) {
        if (!esVacio(datos.getCategoria())) {
            pregunta.setCategoria(datos.getCategoria());
        }
        if (!esVacio(datos.getPregunta())) {
            pregunta.setPregunta(datos.getPregunta());
        }
        if (!esVacio(datos.getRespuesta())) {
            pregunta.setRespuesta(datos.getRespuesta());
        }
        if (datos.getOrden() != null) {
            pregunta.setOrden(datos.getOrden());
        }
        if (datos.getActivo() != null) {
            pregunta.setActivo(datos.getActivo());
        }
    }

    private boolean esVacio(String valor) {
        return valor == null || valor.isBlank();
    }
}
