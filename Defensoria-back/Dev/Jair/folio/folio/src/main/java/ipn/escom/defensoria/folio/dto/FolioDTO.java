package ipn.escom.defensoria.folio.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor

public class FolioDTO {
    private String folio;
    private String mensaje;
    
    private String codigoFolio;
    private String fechaCreacion;
    
    //Datos de la queja 
    private String asunto;
    private String fechaHechos;
    private String descripcion;
    private String unidad;  
    
    //Datos del quejoso
    private String nombre;
    private String primerApellido;
    private String segundoApellido;
    private String correo;
    private String fechaNacimiento;
    
    //Datos que usaremos para identificar el usuario
    private String boleta;
    
    //Datos en caso de ser menor de edad
    private String nombreTutor;
    private String primerApellidoTutor;
    private String segundoApellidoTutor;
    private String parentesco;
    private String correoTutor;
    private String telefonoTutor;
    
    private boolean crearCuenta; 
    private String password;

    public String getFolio() {
        return folio;
    }

    public void setFolio(String folio) {
        this.folio = folio;
    }

    public String getMensaje() {
        return mensaje;
    }

    public void setMensaje(String mensaje) {
        this.mensaje = mensaje;
    }

    public String getCodigoFolio() {
        return codigoFolio;
    }

    public void setCodigoFolio(String codigoFolio) {
        this.codigoFolio = codigoFolio;
    }

    public String getFechaCreacion() {
        return fechaCreacion;
    }

    public void setFechaCreacion(String fechaCreacion) {
        this.fechaCreacion = fechaCreacion;
    }

    public String getAsunto() {
        return asunto;
    }

    public void setAsunto(String asunto) {
        this.asunto = asunto;
    }

    public String getFechaHechos() {
        return fechaHechos;
    }

    public void setFechaHechos(String fechaHechos) {
        this.fechaHechos = fechaHechos;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getUnidad() {
        return unidad;
    }

    public void setUnidad(String unidad) {
        this.unidad = unidad;
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

    public String getCorreo() {
        return correo;
    }

    public void setCorreo(String correo) {
        this.correo = correo;
    }

    public String getFechaNacimiento() {
        return fechaNacimiento;
    }

    public void setFechaNacimiento(String fechaNacimiento) {
        this.fechaNacimiento = fechaNacimiento;
    }

    public String getBoleta() {
        return boleta;
    }

    public void setBoleta(String boleta) {
        this.boleta = boleta;
    }

    public String getNombreTutor() {
        return nombreTutor;
    }

    public void setNombreTutor(String nombreTutor) {
        this.nombreTutor = nombreTutor;
    }

    public String getPrimerApellidoTutor() {
        return primerApellidoTutor;
    }

    public void setPrimerApellidoTutor(String primerApellidoTutor) {
        this.primerApellidoTutor = primerApellidoTutor;
    }

    public String getSegundoApellidoTutor() {
        return segundoApellidoTutor;
    }

    public void setSegundoApellidoTutor(String segundoApellidoTutor) {
        this.segundoApellidoTutor = segundoApellidoTutor;
    }

    public String getParentesco() {
        return parentesco;
    }

    public void setParentesco(String parentesco) {
        this.parentesco = parentesco;
    }

    public String getCorreoTutor() {
        return correoTutor;
    }

    public void setCorreoTutor(String correoTutor) {
        this.correoTutor = correoTutor;
    }

    public String getTelefonoTutor() {
        return telefonoTutor;
    }

    public void setTelefonoTutor(String telefonoTutor) {
        this.telefonoTutor = telefonoTutor;
    }

    public boolean isCrearCuenta() {
        return crearCuenta;
    }

    public void setCrearCuenta(boolean crearCuenta) {
        this.crearCuenta = crearCuenta;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
    
    
}