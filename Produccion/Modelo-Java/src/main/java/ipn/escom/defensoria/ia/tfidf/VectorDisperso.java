package ipn.escom.defensoria.ia.tfidf;

import java.util.Map;

public record VectorDisperso(int[] indices, double[] valores, double norma) {

    public static VectorDisperso de(Map<Integer, Double> pesos) {
        int[] indices = pesos.keySet().stream().mapToInt(Integer::intValue).sorted().toArray();
        double[] valores = new double[indices.length];
        double suma = 0;
        for (int i = 0; i < indices.length; i++) {
            valores[i] = pesos.get(indices[i]);
            suma += valores[i] * valores[i];
        }
        return new VectorDisperso(indices, valores, Math.sqrt(suma));
    }

    public double coseno(VectorDisperso otro) {
        if (norma == 0 || otro.norma == 0) {
            return 0;
        }
        double punto = 0;
        int i = 0;
        int j = 0;
        while (i < indices.length && j < otro.indices.length) {
            int a = indices[i];
            int b = otro.indices[j];
            if (a == b) {
                punto += valores[i++] * otro.valores[j++];
            } else if (a < b) {
                i++;
            } else {
                j++;
            }
        }
        return punto / (norma * otro.norma);
    }
}
