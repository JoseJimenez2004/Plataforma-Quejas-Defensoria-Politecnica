package ipn.escom.defensoria.ia.tfidf;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Indice TF-IDF incremental con vectores dispersos e indice invertido.
 * Agregar un documento cuesta O(longitud del documento); consolidar() recalcula
 * IDF y normas en O(no-ceros). No es thread-safe: la sincronizacion la hace el llamador.
 */
public final class IndiceTfidf {

    private final Map<String, Integer> vocabulario = new HashMap<>();
    private final List<Postings> postings = new ArrayList<>();
    private final List<int[]> terminosPorDoc = new ArrayList<>();
    private final List<float[]> tfPorDoc = new ArrayList<>();
    private int[] df = new int[1024];
    private double[] idf = new double[0];
    private double[] normas = new double[0];

    public void agregar(List<String> tokens) {
        Map<Integer, Integer> conteo = contar(tokens, true);
        int doc = terminosPorDoc.size();
        int[] terminos = new int[conteo.size()];
        float[] tf = new float[conteo.size()];
        int i = 0;
        for (Map.Entry<Integer, Integer> e : conteo.entrySet()) {
            terminos[i] = e.getKey();
            tf[i] = (float) (1 + Math.log(e.getValue()));
            df[terminos[i]]++;
            postings.get(terminos[i]).agregar(doc, tf[i]);
            i++;
        }
        terminosPorDoc.add(terminos);
        tfPorDoc.add(tf);
    }

    public void consolidar() {
        int n = terminosPorDoc.size();
        int v = vocabulario.size();
        double[] nuevoIdf = new double[v];
        for (int t = 0; t < v; t++) {
            nuevoIdf[t] = Math.log((1.0 + n) / (1.0 + df[t])) + 1.0;
        }
        double[] nuevasNormas = new double[n];
        for (int d = 0; d < n; d++) {
            int[] terminos = terminosPorDoc.get(d);
            float[] tf = tfPorDoc.get(d);
            double suma = 0;
            for (int k = 0; k < terminos.length; k++) {
                double w = tf[k] * nuevoIdf[terminos[k]];
                suma += w * w;
            }
            nuevasNormas[d] = Math.sqrt(suma);
        }
        idf = nuevoIdf;
        normas = nuevasNormas;
    }

    public double[] similitudes(List<String> tokensConsulta) {
        int n = normas.length;
        double[] puntajes = new double[n];
        double normaConsulta = 0;
        for (Map.Entry<Integer, Integer> e : contar(tokensConsulta, false).entrySet()) {
            int t = e.getKey();
            if (t >= idf.length) {
                continue;
            }
            double peso = (1 + Math.log(e.getValue())) * idf[t];
            normaConsulta += peso * peso;
            Postings p = postings.get(t);
            double factor = peso * idf[t];
            for (int k = 0; k < p.tamano; k++) {
                if (p.docs[k] < n) {
                    puntajes[p.docs[k]] += factor * p.tf[k];
                }
            }
        }
        if (normaConsulta == 0) {
            return puntajes;
        }
        normaConsulta = Math.sqrt(normaConsulta);
        for (int d = 0; d < n; d++) {
            if (puntajes[d] > 0) {
                puntajes[d] /= normaConsulta * normas[d];
            }
        }
        return puntajes;
    }

    public int tamano() {
        return terminosPorDoc.size();
    }

    public int tamanoVocabulario() {
        return vocabulario.size();
    }

    private Map<Integer, Integer> contar(List<String> tokens, boolean registrarNuevos) {
        Map<Integer, Integer> conteo = new HashMap<>();
        for (String token : tokens) {
            Integer id = registrarNuevos ? Integer.valueOf(registrar(token)) : vocabulario.get(token);
            if (id != null) {
                conteo.merge(id, 1, Integer::sum);
            }
        }
        return conteo;
    }

    private int registrar(String termino) {
        Integer id = vocabulario.get(termino);
        if (id != null) {
            return id;
        }
        id = vocabulario.size();
        vocabulario.put(termino, id);
        postings.add(new Postings());
        if (id >= df.length) {
            df = Arrays.copyOf(df, df.length * 2);
        }
        return id;
    }

    private static final class Postings {
        private int[] docs = new int[4];
        private float[] tf = new float[4];
        private int tamano;

        void agregar(int doc, float valor) {
            if (tamano == docs.length) {
                docs = Arrays.copyOf(docs, tamano * 2);
                tf = Arrays.copyOf(tf, tamano * 2);
            }
            docs[tamano] = doc;
            tf[tamano++] = valor;
        }
    }
}
