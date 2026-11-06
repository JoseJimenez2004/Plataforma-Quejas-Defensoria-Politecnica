package ipn.escom.defensoria.admin_service.entity;

/**
 * Roles del personal de la Defensoría. Solo DEFENSOR (la Defensora) usa el panel de
 * administración (personal, catálogo, plantillas, respaldos y seguimiento); los otros 3 roles
 * entran a su propio panel: Recepción, Primer Contacto y Subdefensoría.
 */
public enum RolStaff {
    RECEPCIONISTA,
    ANALISTA_PRIMER_CONTACTO,
    SUBDEFENSOR,
    DEFENSOR
}
