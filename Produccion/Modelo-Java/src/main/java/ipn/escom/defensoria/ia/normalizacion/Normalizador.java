package ipn.escom.defensoria.ia.normalizacion;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

public final class Normalizador {

    private static final Pattern DIACRITICOS = Pattern.compile("\\p{M}+");
    private static final Pattern SEPARADORES = Pattern.compile("[^a-z0-9]+");
    private static final int LONGITUD_MINIMA = 2;

    private static final Set<String> STOPWORDS = Set.copyOf(Arrays.asList(
        "al", "ante", "bajo", "con", "contra", "de", "del", "desde", "durante", "el", "ella",
        "ellas", "ellos", "en", "entre", "esa", "esas", "ese", "eso", "esos", "esta", "estas",
        "este", "esto", "estos", "estoy", "estamos", "estan", "estaba", "estaban", "estar",
        "fue", "fueron", "fui", "ha", "han", "has", "he", "hay", "haber", "habia", "hacia",
        "hasta", "la", "las", "le", "les", "lo", "los", "me", "mi", "mis", "mio", "mia", "muy",
        "mas", "nos", "nosotros", "nuestra", "nuestro", "no", "ni", "para", "pero", "por",
        "porque", "que", "quien", "se", "segun", "sin", "sobre", "soy", "eres", "es", "son",
        "ser", "sido", "era", "su", "sus", "te", "ti", "tu", "tus", "un", "una", "unas", "unos",
        "yo", "ya", "aqui", "ahi", "alli", "como", "cuando", "donde", "cual", "asi", "tambien",
        "solo", "pues", "osea", "entonces", "luego", "despues", "antes", "ademas", "todo",
        "toda", "todos", "todas", "nada", "algo", "otro", "otra", "otros", "otras", "cada",
        "vez", "si", "sea", "tengo", "tiene", "tenia", "tener", "hacer", "hace", "hizo",
        "mucho", "mucha", "muchos", "poco", "bien", "ahora"
    ));

    private Normalizador() {
    }

    public static String normalizar(String texto) {
        if (texto == null || texto.isEmpty()) {
            return "";
        }
        String sinAcentos = DIACRITICOS.matcher(Normalizer.normalize(texto, Normalizer.Form.NFD)).replaceAll("");
        return sinAcentos.toLowerCase(Locale.ROOT);
    }

    public static List<String> tokenizar(String texto) {
        String[] partes = SEPARADORES.split(normalizar(texto));
        List<String> tokens = new ArrayList<>(partes.length);
        for (String parte : partes) {
            if (parte.length() >= LONGITUD_MINIMA && !STOPWORDS.contains(parte)) {
                tokens.add(parte);
            }
        }
        return tokens;
    }
}
