package ipn.escom.defensoria.antecedentes_service;

import com.fasterxml.jackson.annotation.JsonProperty;
import ipn.escom.defensoria.ia.dataset.Queja;

/**
 * Acepta tres formas:
 *  1. {"texto": "...", "numero_oraciones": 3}
 *  2. {"queja": { ...queja completa... }, "numero_oraciones": 3}
 *  3. La queja completa tal cual viene en el dataset (usa folio y texto_original).
 */
public record ResumenRequest(
        String texto,
        @JsonProperty("texto_original") String textoOriginal,
        String folio,
        Queja queja,
        @JsonProperty("numero_oraciones") Integer numeroOraciones) {

    public String textoAResumir() {
        if (texto != null && !texto.isBlank()) {
            return texto;
        }
        if (textoOriginal != null && !textoOriginal.isBlank()) {
            return textoOriginal;
        }
        if (queja != null) {
            return queja.texto_original != null && !queja.texto_original.isBlank()
                    ? queja.texto_original
                    : queja.textoParaVectorizar();
        }
        return null;
    }

    public String folioQueja() {
        return queja != null ? queja.folio : folio;
    }
}
