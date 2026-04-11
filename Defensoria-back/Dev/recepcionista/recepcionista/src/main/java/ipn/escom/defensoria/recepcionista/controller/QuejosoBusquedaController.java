package ipn.escom.defensoria.recepcionista.controller;

import ipn.escom.defensoria.recepcionista.dto.QuejosoBusquedaResultDTO;
import ipn.escom.defensoria.recepcionista.service.IQuejaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controlador REST para la pantalla de Búsqueda de Antecedentes y Canalización.
 *
 * Permite buscar el historial de quejas previas de un quejoso por nombre,
 * antes de decidir cómo canalizar una queja nueva.
 *
 * Ruta base: /api/recepcionista/quejosos
 */
@RestController
@RequestMapping("/api/recepcionista/quejosos")
@CrossOrigin(origins = "*")
public class QuejosoBusquedaController {

    @Autowired
    private IQuejaService quejaService;

    /**
     * Búsqueda de antecedentes por nombre del quejoso.
     * Busca en perfiles_quejoso por nombre o apellido (búsqueda parcial,
     * sin importar mayúsculas/minúsculas) y devuelve todas las quejas
     * asociadas a los perfiles encontrados.
     *
     * Útil para que la recepcionista identifique si el quejoso tiene
     * casos previos antes de proceder con la canalización.
     *
     * GET /api/recepcionista/quejosos/buscar?nombre=Juan
     * Parámetro: nombre (String) — puede ser nombre, primer o segundo apellido
     * Respuesta: 200 OK + List<QuejosoBusquedaResultDTO>
     *            Lista vacía [] si no se encuentran coincidencias
     */
    @GetMapping("/buscar")
    public ResponseEntity<List<QuejosoBusquedaResultDTO>> buscarAntecedentes(
            @RequestParam String nombre) {
        return ResponseEntity.ok(quejaService.buscarAntecedentes(nombre));
    }
}
