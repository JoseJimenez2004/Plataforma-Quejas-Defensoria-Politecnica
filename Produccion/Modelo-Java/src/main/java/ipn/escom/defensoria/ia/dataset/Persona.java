package ipn.escom.defensoria.ia.dataset;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class Persona {
    public String nombre;
    public String apellido1;
    public String apellido2;
    public String correo;
    public String tipo_identificacion;
    public String numero_identificacion;
    public String cargo;
    public String relacion;

    public String nombreCompleto() {
        return String.join(" ", nombre, apellido1, apellido2).trim();
    }
}
