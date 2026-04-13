package ipn.escom.defensoria.recepcionista.service;

import ipn.escom.defensoria.recepcionista.dto.*;

import java.util.List;

/**
 * Interfaz del servicio de quejas para el módulo Recepcionista.
 *
 * Define el contrato de operaciones disponibles. La implementación concreta
 * está en QuejaServiceImpl. Usar interfaz permite desacoplar el controller
 * de la implementación — si en el futuro se cambia la lógica, el controller
 * no necesita modificarse.
 */
public interface IQuejaService {

    /**
     * Retorna todas las quejas con estatus "RECIBIDA".
     * Alimenta el Dashboard de Recepción con la bandeja de quejas por revisar.
     */
    List<QuejaResumenDTO> listarQuejasPendientes();

    /**
     * Retorna el detalle completo de una queja por su ID.
     * Incluye datos del quejoso, descripción, documentos adjuntos
     * y verificación de campos obligatorios para la pantalla de Validación.
     * Lanza RuntimeException si el ID no existe.
     */
    QuejaDetalleDTO obtenerDetalle(Long id);

    /**
     * Acepta una queja: cambia su estatus de RECIBIDA → EN_REVISION.
     * Registra automáticamente el cambio en historial_estatus_queja.
     * Lanza RuntimeException si el ID no existe o el estatus EN_REVISION no existe en el catálogo.
     */
    QuejaResumenDTO aceptarQueja(Long id);

    /**
     * Rechaza una queja: cambia su estatus a ATENDIDA.
     * Guarda el motivo de rechazo como comentario en historial_estatus_queja.
     * Lanza RuntimeException si el ID no existe.
     */
    QuejaResumenDTO rechazarQueja(Long id, RechazarQuejaRequest request);

    /**
     * Registra que se notificó al quejoso sobre el rechazo de su queja.
     * Por ahora guarda el evento en el historial. En una versión futura,
     * aquí se enviará el correo electrónico al quejoso.
     * Lanza RuntimeException si el ID no existe.
     */
    String notificarRechazo(Long id, RechazarQuejaRequest request);

    /**
     * Canaliza una queja: registra la opción de canalización elegida
     * (ej. "Turno a abogado") y notas adicionales en el historial,
     * y cambia el estatus a EN_REVISION.
     * Lanza RuntimeException si el ID no existe.
     */
    QuejaResumenDTO canalizar(Long id, CanarizarRequest request);

    /**
     * Busca antecedentes de un quejoso por nombre (búsqueda parcial).
     * Busca en perfiles_quejoso por nombre o apellidos y retorna
     * todas las quejas asociadas a los perfiles encontrados.
     * Retorna lista vacía si no hay coincidencias.
     */
    List<QuejosoBusquedaResultDTO> buscarAntecedentes(String nombre);
}
