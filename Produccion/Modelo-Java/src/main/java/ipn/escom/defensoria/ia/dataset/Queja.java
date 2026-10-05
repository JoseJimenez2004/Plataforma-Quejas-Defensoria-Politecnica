package ipn.escom.defensoria.ia.dataset;

public class Queja {
    public String folio;
    public String unidad_academica;
    public String fecha_hechos;
    public String tipo_violencia;
    public String lugar_hechos;
    public Persona quejoso;
    public Persona denunciado;
    public String texto_original;
    public String texto_preprocesado;
    public String resumen_extractivo;
    public String estatus;
    public String area_turnada;
    public boolean es_historico;

    public String textoParaVectorizar() {
        return (texto_preprocesado != null && !texto_preprocesado.isBlank())
                ? texto_preprocesado
                : texto_original;
    }

    @Override
    public String toString() {
        return folio + " | " + tipo_violencia + " | " + unidad_academica;
    }
}
