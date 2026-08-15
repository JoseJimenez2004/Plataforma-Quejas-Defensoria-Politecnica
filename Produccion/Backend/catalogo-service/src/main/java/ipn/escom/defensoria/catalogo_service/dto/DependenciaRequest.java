package ipn.escom.defensoria.catalogo_service.dto;

import lombok.Data;

@Data
public class DependenciaRequest {
    private String clave;
    private String clavePadre;
    private String nombre;
    private String abreviatura;
    private String tipo;
    private String categoria;
    private Integer nivel;
    private String correoContacto;
    private String nombreTitular;
}
