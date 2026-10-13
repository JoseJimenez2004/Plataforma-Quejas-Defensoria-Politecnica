package ipn.escom.defensoria.ia.resumen;

import ipn.escom.defensoria.ia.normalizacion.Normalizador;
import ipn.escom.defensoria.ia.tfidf.VectorDisperso;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Resumen extractivo con TextRank: grafo de oraciones ponderado por coseno TF-IDF,
 * PageRank con criterio de convergencia y seleccion que evita oraciones redundantes.
 * Solo devuelve oraciones literales del texto, en su orden original.
 */
public final class TextRankResumen {

    private static final double D = 0.85;
    private static final double EPSILON = 1e-6;
    private static final int MAX_ITERACIONES = 100;
    private static final int MIN_TOKENS = 3;
    private static final Pattern FIN_ORACION = Pattern.compile("(?<=[.!?])\\s+|[\\r\\n]+");

    public record Oracion(int posicion, String texto, double puntaje) {
    }

    public record Resultado(String resumen, List<Oracion> oraciones, int totalOraciones) {
    }

    private TextRankResumen() {
    }

    public static Resultado resumir(String texto, int numOraciones, double umbralRedundancia) {
        List<String> oraciones = FIN_ORACION.splitAsStream(texto).map(String::trim).filter(s -> !s.isEmpty()).toList();
        int n = oraciones.size();
        if (n == 0) {
            return new Resultado("", List.of(), 0);
        }

        List<List<String>> tokens = oraciones.stream().map(Normalizador::tokenizar).toList();
        VectorDisperso[] vectores = vectorizar(tokens);

        double[][] pesos = new double[n][n];
        double[] grado = new double[n];
        for (int i = 0; i < n; i++) {
            for (int j = i + 1; j < n; j++) {
                double s = vectores[i].coseno(vectores[j]);
                pesos[i][j] = s;
                pesos[j][i] = s;
                grado[i] += s;
                grado[j] += s;
            }
        }

        double[] puntajes = pageRank(pesos, grado);
        List<Integer> elegidas = seleccionar(puntajes, pesos, tokens, Math.max(1, numOraciones), umbralRedundancia);
        Collections.sort(elegidas);

        List<Oracion> resultado = elegidas.stream()
                .map(i -> new Oracion(i, oraciones.get(i), Math.round(puntajes[i] * 10000) / 10000.0))
                .toList();
        String resumen = resultado.stream().map(Oracion::texto).collect(Collectors.joining(" "));
        return new Resultado(resumen, resultado, n);
    }

    private static VectorDisperso[] vectorizar(List<List<String>> tokens) {
        Map<String, Integer> ids = new HashMap<>();
        Map<Integer, Integer> df = new HashMap<>();
        List<Map<Integer, Integer>> conteos = new ArrayList<>(tokens.size());
        for (List<String> oracion : tokens) {
            Map<Integer, Integer> conteo = new HashMap<>();
            for (String t : oracion) {
                conteo.merge(ids.computeIfAbsent(t, k -> ids.size()), 1, Integer::sum);
            }
            conteo.keySet().forEach(id -> df.merge(id, 1, Integer::sum));
            conteos.add(conteo);
        }

        int n = tokens.size();
        VectorDisperso[] vectores = new VectorDisperso[n];
        for (int i = 0; i < n; i++) {
            Map<Integer, Double> pesos = new HashMap<>();
            conteos.get(i).forEach((id, c) ->
                    pesos.put(id, (1 + Math.log(c)) * (Math.log((1.0 + n) / (1.0 + df.get(id))) + 1.0)));
            vectores[i] = VectorDisperso.de(pesos);
        }
        return vectores;
    }

    private static double[] pageRank(double[][] pesos, double[] grado) {
        int n = grado.length;
        double[] pr = new double[n];
        Arrays.fill(pr, 1.0 / n);
        for (int iter = 0; iter < MAX_ITERACIONES; iter++) {
            double colgantes = 0;
            for (int j = 0; j < n; j++) {
                if (grado[j] == 0) {
                    colgantes += pr[j];
                }
            }
            double base = (1 - D) / n + D * colgantes / n;
            double[] nuevo = new double[n];
            double delta = 0;
            for (int i = 0; i < n; i++) {
                double suma = 0;
                double[] fila = pesos[i];
                for (int j = 0; j < n; j++) {
                    if (fila[j] > 0) {
                        suma += fila[j] / grado[j] * pr[j];
                    }
                }
                nuevo[i] = base + D * suma;
                delta += Math.abs(nuevo[i] - pr[i]);
            }
            pr = nuevo;
            if (delta < EPSILON) {
                break;
            }
        }
        return pr;
    }

    private static List<Integer> seleccionar(double[] puntajes, double[][] pesos, List<List<String>> tokens,
                                             int k, double umbralRedundancia) {
        List<Integer> orden = IntStream.range(0, puntajes.length).boxed()
                .sorted((a, b) -> Double.compare(puntajes[b], puntajes[a]))
                .toList();
        List<Integer> elegidas = new ArrayList<>(k);
        for (int i : orden) {
            if (elegidas.size() == k) {
                break;
            }
            if (tokens.get(i).size() < MIN_TOKENS) {
                continue;
            }
            boolean redundante = elegidas.stream().anyMatch(e -> pesos[i][e] > umbralRedundancia);
            if (!redundante) {
                elegidas.add(i);
            }
        }
        for (int i : orden) {
            if (elegidas.size() >= k) {
                break;
            }
            if (!elegidas.contains(i)) {
                elegidas.add(i);
            }
        }
        return elegidas;
    }
}
