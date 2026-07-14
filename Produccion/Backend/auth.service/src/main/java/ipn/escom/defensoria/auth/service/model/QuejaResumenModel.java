package ipn.escom.defensoria.auth.service.model;

import lombok.Data;

/**
 * Subconjunto de campos de `Queja` (queja-service) que auth-service necesita al activar una
 * cuenta — nombre/apellidos reales del quejoso y su número de identificación, para no dejar
 * los placeholders "Ciudadano Defensoría"/"PENDIENTE" en el `Usuario` creado. Jackson solo
 * mapea los campos que reconoce por nombre; el resto de la respuesta de queja-service
 * (descripción, fechas, etc.) se ignora sin problema.
 */
@Data
public class QuejaResumenModel {
    private String numeroFolio;
    private String correoInstitucional;
    private String nombreQuejoso;
    private String apellidoPaternoQuejoso;
    private String apellidoMaternoQuejoso;
    private String numeroIdentificacionQuejoso;
}
