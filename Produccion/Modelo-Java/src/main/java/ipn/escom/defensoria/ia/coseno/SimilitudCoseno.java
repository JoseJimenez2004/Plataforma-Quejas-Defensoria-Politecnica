package ipn.escom.defensoria.ia.coseno;

import java.util.Map;

public class SimilitudCoseno {

    public static double calcular(Map<String, Double> a, Map<String, Double> b) {
        double productoPunto = 0.0;
        double normaA = 0.0;
        double normaB = 0.0;

        for (String termino : a.keySet()) {
            double va = a.get(termino);
            double vb = b.getOrDefault(termino, 0.0);
            productoPunto += va * vb;
            normaA += va * va;
        }

        for (double vb : b.values()) {
            normaB += vb * vb;
        }

        if (normaA == 0.0 || normaB == 0.0) {
            return 0.0;
        }

        return productoPunto / (Math.sqrt(normaA) * Math.sqrt(normaB));
    }
}
