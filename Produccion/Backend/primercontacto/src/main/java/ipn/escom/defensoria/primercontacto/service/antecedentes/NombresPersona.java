package ipn.escom.defensoria.primercontacto.service.antecedentes;

import java.text.Normalizer;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Utilidades compartidas por los buscadores de antecedentes: armar nombres completos y
 * compararlos sin que importen mayúsculas ni acentos ("José" encuentra a "JOSE").
 */
final class NombresPersona {

    static final String ORIGEN_SISTEMA = "SISTEMA";
    static final String ORIGEN_HISTORICO = "HISTORICO";

    private NombresPersona() {
    }

    /** "Ana", "López", null → "Ana López"; todo vacío → null. */
    static String unir(String... partes) {
        String nombre = Arrays.stream(partes)
                .filter(Objects::nonNull)
                .map(String::strip)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.joining(" "));
        return nombre.isEmpty() ? null : nombre;
    }

    /** Minúsculas, sin acentos (la ñ se conserva) y con espacios simples. */
    static String normalizar(String texto) {
        if (texto == null) {
            return "";
        }
        String sinAcentos = Normalizer.normalize(
                        texto.toLowerCase().replace('ñ', '\u0001'), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .replace('\u0001', 'ñ');
        return sinAcentos.replaceAll("[^a-z0-9ñ ]", " ").replaceAll("\\s+", " ").strip();
    }

    /** Palabras de búsqueda ya normalizadas; vacío si no se escribió nada útil. */
    static List<String> palabras(String busqueda) {
        String limpio = normalizar(busqueda);
        return limpio.isEmpty() ? List.of() : List.of(limpio.split(" "));
    }

    /**
     * Mismo nombre de persona, sin importar acentos ni mayúsculas: las palabras del nombre
     * más corto están todas en el más largo ("Ana López" = "ANA LOPEZ RUIZ"). Se exigen al
     * menos 2 palabras (nombre y apellido) para no confundir a dos "Ana".
     */
    static boolean mismoNombre(String a, String b) {
        List<String> pa = palabras(a);
        List<String> pb = palabras(b);
        List<String> corto = pa.size() <= pb.size() ? pa : pb;
        List<String> largo = pa.size() <= pb.size() ? pb : pa;
        return corto.size() >= 2 && largo.containsAll(corto);
    }

    /** Ambos valores existen y son iguales (sin espacios ni mayúsculas). */
    static boolean mismoDato(String a, String b) {
        return a != null && b != null && !a.isBlank()
                && a.strip().equalsIgnoreCase(b.strip());
    }

    /** Todas las palabras buscadas aparecen en el nombre (en cualquier orden). */
    static boolean coincide(String nombreCompleto, List<String> palabras) {
        if (palabras.isEmpty() || nombreCompleto == null) {
            return false;
        }
        String nombre = " " + normalizar(nombreCompleto) + " ";
        return palabras.stream().allMatch(nombre::contains);
    }
}
