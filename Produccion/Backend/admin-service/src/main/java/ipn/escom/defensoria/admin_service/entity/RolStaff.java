package ipn.escom.defensoria.admin_service.entity;

/**
 * Roles del personal administrativo. Solo ADMIN_SISTEMAS puede usar esta consola de
 * administración (crear/editar personal, catálogo, plantillas, respaldos) — los otros 4 roles
 * son cuentas que este panel crea/gestiona para que el personal las use en el futuro front de
 * revisión de quejas (todavía no construido), no para entrar aquí.
 */
public enum RolStaff {
    ADMIN_SISTEMAS,
    RECEPCIONISTA,
    ANALISTA_PRIMER_CONTACTO,
    SUBDEFENSOR,
    DEFENSOR
}
