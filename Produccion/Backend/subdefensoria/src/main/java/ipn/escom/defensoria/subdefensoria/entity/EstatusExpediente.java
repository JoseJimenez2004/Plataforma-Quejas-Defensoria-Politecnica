package ipn.escom.defensoria.subdefensoria.entity;

/**
 * Estatus del expediente, alineado al diagrama de estados de la queja
 * (version 2026-09-18):
 *
 *   RECIBIDO
 *     -> EN_INVESTIGACION   (se elabora el oficio de solicitud de informacion)
 *     -> EN_ESPERA_OFICIO   (oficio enviado, se espera respuesta del director)
 *     -> ELABORO_ACUERDO    (el director respondio y se redacta el acuerdo)
 *     -> PENDIENTE_CONCLUSION (el acuerdo se le mando al quejoso)
 *     -> CONCLUIDO          (el quejoso acepto)
 *
 * Desde PENDIENTE_CONCLUSION, si el quejoso RECHAZA el acuerdo el expediente
 * regresa a EN_INVESTIGACION. Ese retorno es la diferencia de fondo con el
 * modelo anterior, donde el expediente pasaba directo a CONCLUIDO sin que el
 * quejoso interviniera.
 */
public final class EstatusExpediente {

    public static final String RECIBIDO = "RECIBIDO";
    public static final String EN_INVESTIGACION = "EN_INVESTIGACION";
    public static final String EN_ESPERA_OFICIO = "EN_ESPERA_OFICIO";
    public static final String ELABORO_ACUERDO = "ELABORO_ACUERDO";

    /** El acuerdo ya se le envio al quejoso y se espera su respuesta. */
    public static final String PENDIENTE_CONCLUSION = "PENDIENTE_CONCLUSION";

    public static final String CONCLUIDO = "CONCLUIDO";

    private EstatusExpediente() {
    }
}
