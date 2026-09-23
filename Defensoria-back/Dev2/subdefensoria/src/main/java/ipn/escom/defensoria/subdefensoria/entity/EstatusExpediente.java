package ipn.escom.defensoria.subdefensoria.entity;

/**
 * Estatus del expediente segun el BPMN validado con el area (fuente
 * de verdad por encima del manual escrito DDP-PO-02):
 *
 * RECIBIDO -> EN_INVESTIGACION (TS-01/02/03, oficio de solicitud de
 * informacion) -> EN_GESTION_DIRECTOR (TS-04/05/06, oficio al
 * director) -> LISTO_A_DICTAMINAR (director ya respondio, falta
 * decidir si concluyo) -> CONCLUIDO (TS-07/08, cierre dentro de
 * Subdefensoria, sin escalar a Defensoria/Titular).
 */
public final class EstatusExpediente {

    public static final String RECIBIDO = "RECIBIDO";
    public static final String EN_INVESTIGACION = "EN_INVESTIGACION";
    public static final String EN_GESTION_DIRECTOR = "EN_GESTION_DIRECTOR";
    public static final String LISTO_A_DICTAMINAR = "LISTO_A_DICTAMINAR";
    public static final String CONCLUIDO = "CONCLUIDO";

    private EstatusExpediente() {
    }
}
