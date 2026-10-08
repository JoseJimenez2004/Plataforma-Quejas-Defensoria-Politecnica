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

    /** Todas las palabras buscadas aparecen en el nombre (en cualquier orden). */
    static boolean coincide(String nombreCompleto, List<String> palabras) {
        if (palabras.isEmpty() || nombreCompleto == null) {
            return false;
        }
        String nombre = " " + normalizar(nombreCompleto) + " ";
        return palabras.stream().allMatch(nombre::contains);
    }
}
