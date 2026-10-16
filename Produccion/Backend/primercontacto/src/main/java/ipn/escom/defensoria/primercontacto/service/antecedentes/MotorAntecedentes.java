package ipn.escom.defensoria.primercontacto.service.antecedentes;

import ipn.escom.defensoria.primercontacto.dto.AntecedenteDTO;
import ipn.escom.defensoria.primercontacto.entity.ExpedientePrimerContacto;
import ipn.escom.defensoria.primercontacto.entity.QuejaReferencia;

import java.util.List;

/**
 * Punto de conexión del MODELO de búsqueda de antecedentes.
 *
 * Hoy lo implementa MotorAntecedentesReglas (provisional, por reglas). Cuando el modelo
 * esté listo, basta con una nueva implementación de esta interfaz marcada con @Primary
 * (o seleccionada por propiedad): el endpoint, el DTO y la pantalla no cambian.
 *
 * Recibe la queja que se analiza y las quejas candidatas; devuelve las que considere
 * antecedentes, ya ordenadas de mayor a menor similitud.
 */
public interface MotorAntecedentes {

    /** Identificador corto que se muestra en la pantalla, ej. REGLAS_PROVISIONAL. */
    String nombre();

    /** Una línea que explica al analista cómo se buscó. */
    String descripcion();

    List<AntecedenteDTO> buscar(
            ExpedientePrimerContacto expediente,
            QuejaReferencia queja,
            List<QuejaReferencia> candidatas
    );
}
