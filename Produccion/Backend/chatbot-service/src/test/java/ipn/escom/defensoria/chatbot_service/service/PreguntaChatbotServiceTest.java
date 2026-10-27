package ipn.escom.defensoria.chatbot_service.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import ipn.escom.defensoria.chatbot_service.dto.PreguntaChatbotRequest;
import ipn.escom.defensoria.chatbot_service.entity.PreguntaChatbot;
import ipn.escom.defensoria.chatbot_service.model.CategoriaChatbotModel;
import ipn.escom.defensoria.chatbot_service.repository.PreguntaChatbotRepository;

/** Pruebas unitarias de {@link PreguntaChatbotService}: RF-A08 / CU-A10 (mini-chat). */
@ExtendWith(MockitoExtension.class)
@DisplayName("PreguntaChatbotService — preguntas frecuentes")
class PreguntaChatbotServiceTest {

    private static final Long ID = 3L;

    @Mock
    private PreguntaChatbotRepository repositorio;

    @InjectMocks
    private PreguntaChatbotService servicio;

    private static PreguntaChatbot pregunta(long id, String categoria, String texto) {
        PreguntaChatbot p = new PreguntaChatbot();
        p.setId(id);
        p.setCategoria(categoria);
        p.setPregunta(texto);
        p.setRespuesta("Respuesta " + id);
        return p;
    }

    private static PreguntaChatbotRequest datos(String categoria, String pregunta, String respuesta) {
        PreguntaChatbotRequest d = new PreguntaChatbotRequest();
        d.setCategoria(categoria);
        d.setPregunta(pregunta);
        d.setRespuesta(respuesta);
        return d;
    }

    @Test
    void obtenerMenuPublico_agrupaPorCategoriaRespetandoElOrden() {
        when(repositorio.findByActivoTrueOrderByOrdenAsc()).thenReturn(List.of(
                pregunta(1, "Quejas", "¿Cómo presento una queja?"),
                pregunta(2, "Cuenta", "¿Cómo activo mi cuenta?"),
                pregunta(3, "Quejas", "¿Cuánto tarda?")));

        List<CategoriaChatbotModel> menu = servicio.obtenerMenuPublico();

        assertEquals(2, menu.size(), "Dos categorías");
        assertEquals("Quejas", menu.get(0).getCategoria(), "Primero la categoría de la primera pregunta");
        assertEquals(2, menu.get(0).getPreguntas().size(), "Quejas agrupa dos preguntas");
        assertEquals("¿Cuánto tarda?", menu.get(0).getPreguntas().get(1).getPregunta(), "Se conserva el orden");
    }

    @Test
    void obtenerMenuPublico_sinPreguntas_menuVacio() {
        when(repositorio.findByActivoTrueOrderByOrdenAsc()).thenReturn(List.of());
        assertTrue(servicio.obtenerMenuPublico().isEmpty(), "Sin preguntas activas no hay menú");
    }

    @Test
    void crear_datosCompletos_registraFechas() {
        when(repositorio.save(any())).thenAnswer(inv -> inv.getArgument(0));
        PreguntaChatbot p = servicio.crear(datos("Quejas", "¿Pregunta?", "Respuesta"));
        assertNotNull(p.getCreadoEn(), "Fecha de creación");
        assertNotNull(p.getActualizadoEn(), "Fecha de actualización");
    }

    @Test
    void crear_sinRespuesta_lanzaExcepcion() {
        RuntimeException ex = assertThrows(RuntimeException.class, () -> servicio.crear(datos("Quejas", "¿?", "  ")));
        assertEquals("Categoría, pregunta y respuesta son obligatorias.", ex.getMessage(), "Respuesta obligatoria");
        verify(repositorio, never()).save(any());
    }

    @Test
    void editar_desactivarSinTocarTextos() {
        PreguntaChatbot existente = pregunta(ID, "Quejas", "Texto original");
        when(repositorio.findById(ID)).thenReturn(Optional.of(existente));
        when(repositorio.save(any())).thenAnswer(inv -> inv.getArgument(0));
        PreguntaChatbotRequest cambios = datos(null, "", null);
        cambios.setActivo(Boolean.FALSE);

        PreguntaChatbot p = servicio.editar(ID, cambios);

        assertFalse(p.getActivo(), "Se desactiva");
        assertEquals("Texto original", p.getPregunta(), "La pregunta no cambia");
    }

    @Test
    void editar_inexistente_lanzaExcepcion() {
        when(repositorio.findById(ID)).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class, () -> servicio.editar(ID, datos("a", "b", "c")), "Pregunta inexistente");
    }

    @Test
    void eliminar_inexistente_lanzaExcepcionYNoBorra() {
        when(repositorio.existsById(ID)).thenReturn(false);
        assertThrows(RuntimeException.class, () -> servicio.eliminar(ID), "Pregunta inexistente");
        verify(repositorio, never()).deleteById(any());
    }

    @Test
    void eliminar_existente_laBorra() {
        when(repositorio.existsById(ID)).thenReturn(true);
        servicio.eliminar(ID);
        verify(repositorio).deleteById(ID);
    }
}
