package ipn.escom.defensoria.ia.tfidf;

import ipn.escom.defensoria.ia.normalizacion.Normalizador;
import java.util.*;

public class TfidfVectorizer {

    private final List<String> vocabulario;
    private final List<Map<String, Double>> vectores;
    private final Map<String, Double> idf;

    public TfidfVectorizer(List<String> documentos) {
        Set<String> vocab = new LinkedHashSet<>();
        List<List<String>> tokensDocs = new ArrayList<>();

        for (String doc : documentos) {
            List<String> tokens = Normalizador.tokenizar(doc);
            tokensDocs.add(tokens);
            vocab.addAll(tokens);
        }

        this.vocabulario = new ArrayList<>(vocab);
        int n = documentos.size();

        Map<String, Integer> df = new HashMap<>();
        for (List<String> tokens : tokensDocs) {
            Set<String> unicos = new HashSet<>(tokens);
            for (String t : unicos) {
                df.merge(t, 1, Integer::sum);
            }
        }

        this.idf = new HashMap<>();
        for (String termino : vocabulario) {
            double idfValue = Math.log((double) n / (1 + df.getOrDefault(termino, 0)));
            idf.put(termino, idfValue);
        }

        this.vectores = new ArrayList<>();
        for (List<String> tokens : tokensDocs) {
            Map<String, Integer> tf = new HashMap<>();
            for (String t : tokens) {
                tf.merge(t, 1, Integer::sum);
            }

            Map<String, Double> vector = new LinkedHashMap<>();
            for (String termino : vocabulario) {
                int frecuencia = tf.getOrDefault(termino, 0);
                double tfValue = frecuencia > 0 ? 1 + Math.log(frecuencia) : 0;
                vector.put(termino, tfValue * idf.get(termino));
            }
            vectores.add(vector);
        }
    }

    public List<String> vocabulario() {
        return vocabulario;
    }

    public Map<String, Double> vector(int indice) {
        return vectores.get(indice);
    }

    public Map<String, Double> vectorizarConsulta(String texto) {
        List<String> tokens = Normalizador.tokenizar(texto);
        Map<String, Integer> tf = new HashMap<>();
        for (String t : tokens) {
            tf.merge(t, 1, Integer::sum);
        }

        Map<String, Double> vector = new LinkedHashMap<>();
        for (String termino : vocabulario) {
            int frecuencia = tf.getOrDefault(termino, 0);
            double tfValue = frecuencia > 0 ? 1 + Math.log(frecuencia) : 0;
            vector.put(termino, tfValue * idf.getOrDefault(termino, 0.0));
        }
        return vector;
    }
}
