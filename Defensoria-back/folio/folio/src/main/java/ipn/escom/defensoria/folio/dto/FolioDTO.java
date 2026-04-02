package ipn.escom.defensoria.folio.dto;

public class FolioDTO {
    private String folio;
    private String mensaje;
    
    public FolioDTO (String folio, String mensaje){
        this.folio = folio;
        this.folio = mensaje;
    }

    public String getFolio() {
        return folio;
    }

    public String getMensaje() {
        return mensaje;
    }
}
