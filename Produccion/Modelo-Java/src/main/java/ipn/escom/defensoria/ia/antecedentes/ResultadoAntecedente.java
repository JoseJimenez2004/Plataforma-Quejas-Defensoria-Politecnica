package ipn.escom.defensoria.ia.antecedentes;

import com.fasterxml.jackson.annotation.JsonInclude;
import ipn.escom.defensoria.ia.dataset.Persona;
import ipn.escom.defensoria.ia.dataset.Queja;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ResultadoAntecedente(
        String folio,
        double similitud,
        boolean historico,
        String tipo_violencia,
        String unidad_academica,
        String fecha_hechos,
        String lugar_hechos,
        String estatus,
        String area_turnada,
        Persona quejoso,
        Persona denunciado,
        String fragmento,
        String texto) {

    private static final int LONGITUD_FRAGMENTO = 300;

    static ResultadoAntecedente de(Queja q, double similitud, boolean incluirTexto) {
        String texto = q.texto_original != null ? q.texto_original : q.textoParaVectorizar();
        String fragmento = texto.length() <= LONGITUD_FRAGMENTO ? texto : texto.substring(0, LONGITUD_FRAGMENTO) + "...";
        return new ResultadoAntecedente(
                q.folio, Math.round(similitud * 10000) / 10000.0, q.es_historico, q.tipo_violencia,
                q.unidad_academica, q.fecha_hechos, q.lugar_hechos, q.estatus, q.area_turnada,
                q.quejoso, q.denunciado,
                incluirTexto ? null : fragmento,
                incluirTexto ? texto : null);
    }
}
