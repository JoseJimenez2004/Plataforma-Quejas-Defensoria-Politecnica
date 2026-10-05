package ipn.escom.defensoria.ia.antecedentes;

import ipn.escom.defensoria.ia.coseno.SimilitudCoseno;
import ipn.escom.defensoria.ia.dataset.Queja;
import ipn.escom.defensoria.ia.tfidf.TfidfVectorizer;
import java.util.*;

public class BuscadorAntecedentes {

    private final List<Queja> corpus;
    private final TfidfVectorizer vectorizer;

    public BuscadorAntecedentes(List<Queja> historico, List<Queja> nuevo) {
        this.corpus = new ArrayList<>();
        if (historico != null) this.corpus.addAll(historico);
        if (nuevo != null) this.corpus.addAll(nuevo);

        List<String> documentos = corpus.stream()
                .map(Queja::textoParaVectorizar)
                .toList();

        this.vectorizer = new TfidfVectorizer(documentos);
    }

    public List<ResultadoAntecedente> buscar(String consulta, double umbral, int topK) {
        Map<String, Double> vectorConsulta = vectorizer.vectorizarConsulta(consulta);

        List<ResultadoAntecedente> resultados = new ArrayList<>();
        for (int i = 0; i < corpus.size(); i++) {
            Map<String, Double> vectorDoc = vectorizer.vector(i);
            double sim = SimilitudCoseno.calcular(vectorConsulta, vectorDoc);
            if (sim >= umbral) {
                resultados.add(new ResultadoAntecedente(corpus.get(i), sim, corpus.get(i).es_historico));
            }
        }

        resultados.sort((a, b) -> Double.compare(b.similitud(), a.similitud()));

        return resultados.stream()
                .limit(topK)
                .toList();
    }
}
