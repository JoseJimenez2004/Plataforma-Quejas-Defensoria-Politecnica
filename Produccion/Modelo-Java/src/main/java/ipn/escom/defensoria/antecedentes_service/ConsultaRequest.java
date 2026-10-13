package ipn.escom.defensoria.antecedentes_service;

public record ConsultaRequest(String texto, Double umbral, Integer topK, Boolean incluirTexto) {
}
