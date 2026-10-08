package ipn.escom.defensoria.ia.normalizacion;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

public class Normalizador {

    private static final Set<String> STOPWORDS = new java.util.HashSet<>(java.util.Arrays.asList(
        "a", "al", "ante", "bajo", "con", "contra", "de", "del", "desde",
        "durante", "el", "ella", "ellas", "ellos", "en", "entre", "esa",
        "esas", "ese", "eso", "esos", "esta", "estas", "estoy",
        "estamos", "estan", "esto", "estos", "excepto", "fue",
        "fueron", "ha", "haber", "habia", "han", "has", "hay", "he",
        "hacia", "hasta", "la", "las", "le", "les", "lo", "los", "mediante",
        "me", "mi", "mio", "mis", "muy", "nos", "nosotros", "nuestra",
        "nuestro", "o", "para", "pero", "por", "que", "quien", "salvo",
        "se", "segun", "sin", "sobre", "soy", "eres", "es", "son", "ser",
        "sido", "su", "suyo", "te", "tenia", "tengo", "tiene", "tu", "tuyo",
        "un", "una", "unas", "unos", "vosotros", "vuestro", "y", "ya",
        "aqui", "ahi", "alli", "como", "cuando", "donde", "cuyo", "cuya"
    ));

    public static List<String> tokenizar(String texto) {
        return Arrays.stream(texto.toLowerCase().split("\\W+"))
                .filter(p -> !p.isBlank())
                .filter(p -> !STOPWORDS.contains(p))
                .toList();
    }
}
