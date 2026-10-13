package ipn.escom.defensoria.denunciado_service.dto;

import java.util.List;

import org.springframework.web.multipart.MultipartFile;

/** multipart/form-data de POST /api/denunciado/respuestas. */
public class RegistroRespuestaRequest {

    private String folioQueja;
    private String nombre;
    private String apellido1;
    private String apellido2;
    private String unidadProcedenciaClave;
    private String tipoIdentificacion;
    private String numeroIdentificacion;
    private String descripcionHechos;
    private Boolean avisoPrivacidadAceptado;
    private String avisoPrivacidadVersion;
    /** 1 o 2 imágenes JPG/PNG (frente y reverso). */
    private List<MultipartFile> credencial;
    /** Opcionales, hasta 10. */
    private List<MultipartFile> evidencias;

    public String getFolioQueja() { return folioQueja; }
    public void setFolioQueja(String folioQueja) { this.folioQueja = folioQueja; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public String getApellido1() { return apellido1; }
    public void setApellido1(String apellido1) { this.apellido1 = apellido1; }
    public String getApellido2() { return apellido2; }
    public void setApellido2(String apellido2) { this.apellido2 = apellido2; }
    public String getUnidadProcedenciaClave() { return unidadProcedenciaClave; }
    public void setUnidadProcedenciaClave(String clave) { this.unidadProcedenciaClave = clave; }
    public String getTipoIdentificacion() { return tipoIdentificacion; }
    public void setTipoIdentificacion(String tipoIdentificacion) { this.tipoIdentificacion = tipoIdentificacion; }
    public String getNumeroIdentificacion() { return numeroIdentificacion; }
    public void setNumeroIdentificacion(String numero) { this.numeroIdentificacion = numero; }
    public String getDescripcionHechos() { return descripcionHechos; }
    public void setDescripcionHechos(String descripcionHechos) { this.descripcionHechos = descripcionHechos; }
    public Boolean getAvisoPrivacidadAceptado() { return avisoPrivacidadAceptado; }
    public void setAvisoPrivacidadAceptado(Boolean aceptado) { this.avisoPrivacidadAceptado = aceptado; }
    public String getAvisoPrivacidadVersion() { return avisoPrivacidadVersion; }
    public void setAvisoPrivacidadVersion(String version) { this.avisoPrivacidadVersion = version; }
    public List<MultipartFile> getCredencial() { return credencial; }
    public void setCredencial(List<MultipartFile> credencial) { this.credencial = credencial; }
    public List<MultipartFile> getEvidencias() { return evidencias; }
    public void setEvidencias(List<MultipartFile> evidencias) { this.evidencias = evidencias; }
}
