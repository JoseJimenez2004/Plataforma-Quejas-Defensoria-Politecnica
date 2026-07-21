package ipn.escom.defensoria.subdefensoria.entity;

/**
 * Distingue a cual de los dos ciclos del BPMN pertenece un oficio:
 * SOLICITUD_INFORMACION = TS-01/02/03 (solicitar a directores si ya
 * conocen el asunto). GESTION_DIRECTOR = TS-04/05/06 (oficio formal
 * al director, con recordatorios que ofrecen medidas).
 */
public final class FaseOficio {
    public static final String SOLICITUD_INFORMACION = "SOLICITUD_INFORMACION";
    public static final String GESTION_DIRECTOR = "GESTION_DIRECTOR";

    private FaseOficio() {
    }
}
