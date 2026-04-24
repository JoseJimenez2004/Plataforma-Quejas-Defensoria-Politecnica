package ipn.escom.defensoria.quejoso.entity;

public enum EstatusQuejaEntity {
    RECIBIDA,            // Recepción valida requerimientos
    EN_ANALISIS,         // Primer contacto valida competencia
    COMPETENTE,
    IMPROCEDENTE,
    ASIGNADO_A_ABOGADO,
    EN_INVESTIGACION,
    PROCESO_CONCLUSION,
    FINALIZADA,
    REMISION,
    CANCELADA// Fin de ciclo por improcedencia
}