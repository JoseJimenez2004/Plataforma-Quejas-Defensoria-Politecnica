package ipn.escom.defensoria.revision_service.model;

import java.time.LocalDateTime;

/**
 * Números de la parte de arriba de la Bandeja. Están pensados para que la Defensoría sepa
 * cuánto trabajo hay, si se está atendiendo a tiempo y quién lo está atendiendo:
 *
 * @param porAtender           quejas que nadie ha tomado (nuevas + corregidas por el quejoso)
 * @param nuevasHoy            quejas que llegaron hoy (cualquier estatus)
 * @param corregidas           de las porAtender, cuántas son reenvíos tras un rechazo
 * @param enAtencion           quejas abiertas ahora mismo por alguna recepcionista
 * @param enAtencionMias       de esas, las que tiene abiertas quien consulta
 * @param atendidasHoy         validadas hoy por el área (turnadas + rechazadas)
 * @param atendidasHoyMias     de esas, las que validó quien consulta
 * @param turnadasHoy          turnadas a Primer Contacto hoy
 * @param rechazadasHoy        regresadas al quejoso con observaciones hoy
 * @param folioMasAntiguo      la queja que más tiempo lleva esperando a que alguien la tome
 * @param fechaMasAntigua      desde cuándo espera
 * @param horasPromedioAtencion promedio de horas entre que llega una queja y Recepción la valida
 *                              (últimos 30 días); null si no hay datos
 * @param atendidasUltimos30   cuántas quejas entran en ese promedio
 */
public record IndicadoresBandejaModel(
        long porAtender,
        long nuevasHoy,
        long corregidas,
        long enAtencion,
        long enAtencionMias,
        long atendidasHoy,
        long atendidasHoyMias,
        long turnadasHoy,
        long rechazadasHoy,
        String folioMasAntiguo,
        LocalDateTime fechaMasAntigua,
        Double horasPromedioAtencion,
        long atendidasUltimos30) {
}
