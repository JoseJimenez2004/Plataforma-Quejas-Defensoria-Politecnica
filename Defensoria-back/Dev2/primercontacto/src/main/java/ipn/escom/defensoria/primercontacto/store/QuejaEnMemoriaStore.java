package ipn.escom.defensoria.primercontacto.store;

import ipn.escom.defensoria.primercontacto.dto.ExpedienteAnalisisDTO;
import ipn.escom.defensoria.primercontacto.dto.QuejaEntranteDTO;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Comparator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Almacén en memoria (compartido entre requests, vive mientras el
 * proceso de Java está arriba) para las quejas que Subdefensoría
 * empuja hacia Primer Contacto.
 *
 * No usa base de datos a propósito: la fuente de verdad de la queja
 * es Subdefensoría, este servicio solo la "escucha" y la mantiene
 * disponible para el flujo de análisis. Lo que sí sigue viviendo en
 * la BD local (H2) son las notas, citas, dictámenes y remisiones,
 * porque esos artefactos los genera Primer Contacto mismo.
 */
@Component
public class QuejaEnMemoriaStore {

    private final Map<String, ExpedienteAnalisisDTO> porFolio = new ConcurrentHashMap<>();
    private final Map<Long, String> folioPorQuejaId = new ConcurrentHashMap<>();

    /**
     * Registra o actualiza una queja recibida desde Subdefensoría.
     * Si el folio ya existía, se conserva el estatus de análisis que
     * ya traía (para no perder el avance del analista) y solo se
     * refrescan los datos "de hechos" (narrativa, quejoso, evidencias, etc).
     */
    public ExpedienteAnalisisDTO registrar(QuejaEntranteDTO entrante) {
        String estatusPrevio = null;
        ExpedienteAnalisisDTO existente = porFolio.get(entrante.getFolio());
        if (existente != null) {
            estatusPrevio = existente.getEstatus();
        }

        ExpedienteAnalisisDTO expediente = ExpedienteAnalisisDTO.builder()
                .quejaId(entrante.getQuejaId())
                .folio(entrante.getFolio())
                .tema(entrante.getTema())
                .descripcionHechos(entrante.getDescripcionHechos())
                .fechaRecepcion(entrante.getFechaRecepcion())
                .estatus(estatusPrevio != null ? estatusPrevio : "PENDIENTE_ANALISIS")
                .prioridad(entrante.getPrioridad())
                .quejoso(entrante.getQuejoso())
                .evidencias(entrante.getEvidencias())
                .build();

        porFolio.put(expediente.getFolio(), expediente);
        folioPorQuejaId.put(expediente.getQuejaId(), expediente.getFolio());

        return expediente;
    }

    public Collection<ExpedienteAnalisisDTO> listarTodas() {
        return porFolio.values().stream()
                .sorted(Comparator.comparing(
                        ExpedienteAnalisisDTO::getFolio,
                        Comparator.nullsLast(String::compareTo)))
                .toList();
    }

    public ExpedienteAnalisisDTO obtenerPorFolio(String folio) {
        return porFolio.get(folio);
    }

    public ExpedienteAnalisisDTO obtenerPorQuejaId(Long quejaId) {
        String folio = folioPorQuejaId.get(quejaId);
        return folio != null ? porFolio.get(folio) : null;
    }

    public boolean existePorFolio(String folio) {
        return porFolio.containsKey(folio);
    }

    /**
     * Actualiza únicamente el estatus (lo usan dictamen/remisión
     * cuando el analista resuelve algo sobre la queja).
     */
    public void actualizarEstatus(Long quejaId, String nuevoEstatus) {
        ExpedienteAnalisisDTO actual = obtenerPorQuejaId(quejaId);
        if (actual != null) {
            actual.setEstatus(nuevoEstatus);
        }
    }

    public int total() {
        return porFolio.size();
    }
}
