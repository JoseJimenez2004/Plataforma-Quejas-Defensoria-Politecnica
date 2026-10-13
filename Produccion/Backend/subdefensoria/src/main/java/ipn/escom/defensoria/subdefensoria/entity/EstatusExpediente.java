package ipn.escom.defensoria.subdefensoria.entity;

/**
 * Estatus del expediente, alineado a la tabla ECA del diagrama de estados de la queja
 * (versión 2026-10-12, filas 12 a 20):
 *
 *   RECIBIDO                (llega de Primer Contacto, CU-SD-01)
 *     -> EN_INVESTIGACION   (el subdefensor abre el expediente, CU-SD-05)
 *     -> EN_ESPERA_OFICIO   (se envía un oficio, CU-SD-06)
 *     -> ELABORO_ACUERDO    (el director responde, CU-SD-08)
 *     -> PENDIENTE_CONCILIACION (se le manda el acuerdo al quejoso, CU-SD-09)
 *     -> PENDIENTE_CONCLUSION   (el quejoso acepta el acuerdo de conciliación, CU-QJ-14)
 *     -> CONCLUIDO          (se manda el acuerdo de conclusión, CU-SD-09)
 *
 * Oficios en dos rondas fijas: el de SOLICITUD_INFORMACION, al responderse, regresa el
 * expediente a EN_INVESTIGACION para emitir el de GESTION_DIRECTOR; la respuesta a este
 * último lleva a ELABORO_ACUERDO.
 *
 * Desde PENDIENTE_CONCILIACION, si el quejoso RECHAZA el acuerdo, el expediente regresa a
 * EN_INVESTIGACION y se emite otro oficio de gestión para buscar un nuevo acuerdo.
 */
public final class EstatusExpediente {

    public static final String RECIBIDO = "RECIBIDO";
    public static final String EN_INVESTIGACION = "EN_INVESTIGACION";
    public static final String EN_ESPERA_OFICIO = "EN_ESPERA_OFICIO";
    public static final String ELABORO_ACUERDO = "ELABORO_ACUERDO";

    /** El acuerdo de conciliación ya se le envió al quejoso y se espera su respuesta. */
    public static final String PENDIENTE_CONCILIACION = "PENDIENTE_CONCILIACION";

    /** El quejoso aceptó el acuerdo de conciliación; falta el acuerdo de conclusión. */
    public static final String PENDIENTE_CONCLUSION = "PENDIENTE_CONCLUSION";

    public static final String CONCLUIDO = "CONCLUIDO";

    private EstatusExpediente() {
    }

    /**
     * Estatus que ve el quejoso en quejas.estatus. Son los mismos nombres, salvo RECIBIDO
     * (que en la queja se distingue de la recepción inicial) y CONCLUIDO (la queja es
     * femenina, como sus demás estatus).
     */
    public static String estatusQueja(String estatusExpediente) {
        return switch (estatusExpediente) {
            case RECIBIDO -> "RECIBIDA_SUBDEFENSORIA";
            case CONCLUIDO -> "CONCLUIDA";
            default -> estatusExpediente;
        };
    }
}
