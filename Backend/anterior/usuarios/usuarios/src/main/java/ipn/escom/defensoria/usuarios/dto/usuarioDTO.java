package ipn.escom.defensoria.usuarios.dto;

import lombok.Data;

@Data
public class usuarioDTO {
    private String correo;
    private String password;
    private String nombre;
    private String primerApellido;
    private String segundoApellido;
    private String boleta;
    private String folioVinculado;

    public String getCorreo() {
        return correo;
    }

    public void setCorreo(String correo) {
        this.correo = correo;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getPrimerApellido() {
        return primerApellido;
    }

    public void setPrimerApellido(String primerApellido) {
        this.primerApellido = primerApellido;
    }

    public String getSegundoApellido() {
        return segundoApellido;
    }

    public void setSegundoApellido(String segundoApellido) {
        this.segundoApellido = segundoApellido;
    }

    public String getBoleta() {
        return boleta;
    }

    public void setBoleta(String boleta) {
        this.boleta = boleta;
    }

    public String getFolioVinculado() {
        return folioVinculado;
    }

    public void setFolioVinculado(String folioVinculado) {
        this.folioVinculado = folioVinculado;
    }
    
    
}