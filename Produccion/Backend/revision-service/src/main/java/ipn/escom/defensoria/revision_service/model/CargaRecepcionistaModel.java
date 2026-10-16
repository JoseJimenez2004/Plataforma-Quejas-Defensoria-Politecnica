package ipn.escom.defensoria.revision_service.model;

/** Una fila del panel "Equipo de Recepción": cuántas quejas tiene abiertas y cuántas validó hoy
 * cada recepcionista activa. soyYo marca la fila de quien consulta. */
public record CargaRecepcionistaModel(
        String nombre,
        boolean soyYo,
        long enAtencion,
        long atendidasHoy) {
}
