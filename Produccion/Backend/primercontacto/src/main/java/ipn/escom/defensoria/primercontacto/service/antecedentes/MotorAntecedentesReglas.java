package ipn.escom.defensoria.primercontacto.service.antecedentes;

import ipn.escom.defensoria.primercontacto.dto.AntecedenteDTO;
import ipn.escom.defensoria.primercontacto.entity.ExpedientePrimerContacto;
import ipn.escom.defensoria.primercontacto.entity.QuejaReferencia;
import org.springframework.stereotype.Component;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Motor PROVISIONAL de antecedentes, mientras llega el modelo. Puntaje de 0 a 100:
 *
 *   mismo quejoso (mismo correo)          45
 *   misma unidad académica                15
 *   hechos similares (palabras en común)  hasta 40
 *
 * Aparece como antecedente si es del mismo quejoso o si suma al menos UMBRAL. Es
 * deliberadamente simple y explicable: cada resultado dice por qué salió.
 */
@Component
public class MotorAntecedentesReglas implements MotorAntecedentes {

    private static final int PUNTOS_MISMO_QUEJOSO = 45;
    private static final int PUNTOS_MISMA_UNIDAD = 15;
    private static final int PUNTOS_MAX_TEXTO = 40;
    private static final int UMBRAL = 25;
    private static final int MAX_RESULTADOS = 15;

    private static final Set<String> VACIAS = Set.of(
            "para", "como", "pero", "porque", "desde", "hasta", "sobre", "entre", "cuando",
            "donde", "este", "esta", "estos", "estas", "tambien", "tiene", "tenia", "fueron",
            "sido", "hace", "hacia", "ante", "sino", "solo", "todo", "todos", "otra", "otro",
            "otros", "mismo", "misma", "queja", "quejo", "senalo", "manera", "parte", "ademas",
            "dicho", "dicha", "cual", "cuales", "quien", "quienes", "estaba", "estan", "esto",
            "unos", "unas", "muy", "mas", "menos", "ellos", "ellas", "nuestro", "nuestra",
            "registro", "manual", "prueba"
    );

    @Override
    public String nombre() {
        return "REGLAS_PROVISIONAL";
    }

    @Override
    public String descripcion() {
        return "Búsqueda provisional por reglas: mismo quejoso, misma unidad académica y "
                + "palabras en común en el motivo y los hechos. Se sustituirá por el modelo.";
    }

    @Override
    public List<AntecedenteDTO> buscar(
            ExpedientePrimerContacto expediente,
            QuejaReferencia queja,
            List<QuejaReferencia> candidatas
    ) {

        String correo = queja != null && queja.getCorreoInstitucional() != null
                ? queja.getCorreoInstitucional()
                : expediente.getQuejosoCorreo();

        String unidad = queja != null && queja.getUnidadAcademicaClave() != null
                ? queja.getUnidadAcademicaClave()
                : expediente.getUnidadAcademica();

        Set<String> palabras = palabrasClave(
                expediente.getTema() + " " + expediente.getDescripcionHechos()
                        + (queja != null ? " " + queja.getMotivo() + " " + queja.getDescripcion() : "")
        );

        List<AntecedenteDTO> resultados = new ArrayList<>();

        for (QuejaReferencia candidata : candidatas) {

            List<String> coincidencias = new ArrayList<>();
            int puntaje = 0;

            boolean mismoQuejoso = correo != null
                    && correo.equalsIgnoreCase(candidata.getCorreoInstitucional());

            if (mismoQuejoso) {
                puntaje += PUNTOS_MISMO_QUEJOSO;
                coincidencias.add("Mismo quejoso");
            }

            if (unidad != null && unidad.equalsIgnoreCase(candidata.getUnidadAcademicaClave())) {
                puntaje += PUNTOS_MISMA_UNIDAD;
                coincidencias.add("Misma unidad académica");
            }

            Set<String> otras = palabrasClave(candidata.getMotivo() + " " + candidata.getDescripcion());
            Set<String> comunes = new LinkedHashSet<>(palabras);
            comunes.retainAll(otras);

            if (!comunes.isEmpty() && !palabras.isEmpty() && !otras.isEmpty()) {
                // Coeficiente de traslape: tolera que una narrativa sea mucho más larga que la otra.
                double traslape = (double) comunes.size() / Math.min(palabras.size(), otras.size());
                int puntosTexto = (int) Math.round(Math.min(1.0, traslape * 1.5) * PUNTOS_MAX_TEXTO);

                if (puntosTexto >= 8) {
                    puntaje += puntosTexto;
                    coincidencias.add("Hechos similares: " + comunes.stream().limit(5).collect(Collectors.joining(", ")));
                }
            }

            if (mismoQuejoso || puntaje >= UMBRAL) {
                resultados.add(AntecedenteDTO.builder()
                        .folioQueja(candidata.getNumeroFolio())
                        .folioPrimerContacto(candidata.getFolioPrimerContacto())
                        .fecha(candidata.getFechaCreacion() != null ? candidata.getFechaCreacion().toString() : null)
                        .asunto(candidata.getMotivo())
                        .extracto(extracto(candidata.getDescripcion()))
                        .unidadAcademica(candidata.getUnidadAcademicaClave())
                        .nombreQuejoso(nombre(candidata))
                        .estatus(candidata.getEstatus())
                        .similitud(Math.min(100, puntaje))
                        .coincidencias(coincidencias)
                        .mismoQuejoso(mismoQuejoso)
                        .build());
            }
        }

        resultados.sort(Comparator.comparingInt(AntecedenteDTO::getSimilitud).reversed()
                .thenComparing(AntecedenteDTO::getFecha, Comparator.nullsLast(Comparator.reverseOrder())));

        return resultados.stream().limit(MAX_RESULTADOS).toList();
    }

    private Set<String> palabrasClave(String texto) {
        if (texto == null) {
            return Set.of();
        }

        String limpio = Normalizer.normalize(texto.toLowerCase(), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .replaceAll("[^a-z0-9ñ ]", " ");

        return Arrays.stream(limpio.split("\\s+"))
                .filter(p -> p.length() >= 4)
                .filter(p -> !VACIAS.contains(p))
                .filter(p -> !"null".equals(p))
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private String extracto(String descripcion) {
        if (descripcion == null) {
            return null;
        }
        String texto = descripcion.strip();
        return texto.length() <= 220 ? texto : texto.substring(0, 217) + "…";
    }

    private String nombre(QuejaReferencia queja) {
        String nombre = Arrays.stream(new String[]{queja.getNombreQuejoso(), queja.getApellido1Quejoso()})
                .filter(Objects::nonNull)
                .map(String::strip)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.joining(" "));
        return nombre.isEmpty() ? null : nombre;
    }
}
