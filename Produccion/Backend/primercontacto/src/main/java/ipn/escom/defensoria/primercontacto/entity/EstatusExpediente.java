package ipn.escom.defensoria.primercontacto.entity;

import java.util.Set;

/**
 * Estados de la queja mientras está en Primer Contacto, con los MISMOS nombres que el
 * diagrama de estados de la queja (y que quejas.estatus):
 *
 *   TURNADA  --(el analista abre el expediente)-->  EN_ANALISIS
 *   EN_ANALISIS --(dictamen de competencia)-->      PROCEDENTE   -> Subdefensoría lo recibe
 *   EN_ANALISIS --(dictamen de improcedencia)-->    IMPROCEDENTE
 *   IMPROCEDENTE --(se envía la remisión)-->        REMITIDA     (cierre por remisión)
 *
 * IMPROCEDENTE ya no es terminal: toda queja improcedente termina en REMITIDA.
 *
 * Tener una cita o notas NO es un estado (antes existía el pseudo-estado CON_CITA): son
 * indicadores aparte que no bloquean ninguna transición.
 */
public final class EstatusExpediente {

    public static final String TURNADA = "TURNADA";
    public static final String EN_ANALISIS = "EN_ANALISIS";
    public static final String PROCEDENTE = "PROCEDENTE";
    public static final String IMPROCEDENTE = "IMPROCEDENTE";
    public static final String REMITIDA = "REMITIDA";

    /**
     * Estado viejo (antes de 2026-09-27): el expediente pasaba aquí al REDACTAR la remisión.
     * Ahora "redactada vs. enviada" es el estatus propio de RemisionExterna, y el expediente
     * sigue IMPROCEDENTE hasta que la remisión se envía. Se sigue reconociendo por si quedan
     * filas viejas en la base (ver docs/migracion-estados-primer-contacto.sql).
     */
    public static final String PENDIENTE_REMISION_LEGADO = "PENDIENTE_REMISION";

    /** Estados en los que todavía se puede trabajar el expediente (citas, notas, dictamen). */
    public static final Set<String> ABIERTOS = Set.of(TURNADA, EN_ANALISIS);

    private EstatusExpediente() {
    }

    public static boolean estaAbierto(String estatus) {
        return estatus != null && ABIERTOS.contains(estatus);
    }

    public static boolean esImprocedente(String estatus) {
        return IMPROCEDENTE.equals(estatus) || PENDIENTE_REMISION_LEGADO.equals(estatus);
    }
}
