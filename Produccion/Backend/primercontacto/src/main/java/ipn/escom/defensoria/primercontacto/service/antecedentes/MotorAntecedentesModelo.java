package ipn.escom.defensoria.primercontacto.service.antecedentes;

import com.fasterxml.jackson.databind.JsonNode;
import ipn.escom.defensoria.primercontacto.dto.AntecedenteDTO;
import ipn.escom.defensoria.primercontacto.entity.ExpedientePrimerContacto;
import ipn.escom.defensoria.primercontacto.entity.QuejaReferencia;
import ipn.escom.defensoria.primercontacto.repository.QuejaReferenciaRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Motor que consulta el MODELO de antecedentes (antecedentes-service, proyecto Modelo-Java):
 *
 *   POST {antecedentes.modelo.url}/api/antecedentes/buscar
 *        { "texto": "...", "umbral": 0.0, "topK": 500 }
 *   →    { "antecedentes": [ { "queja": {...}, "similitud": 0.42, "historico": true } ] }
 *
 * El modelo trae su propio índice de quejas (hoy, su dataset de prueba) y solo compara
 * TEXTO: no sabe quién es el quejoso. Por eso se le piden TODOS sus resultados (umbral 0,
 * topK alto) y AntecedentesService se queda solo con los del mismo quejoso o del mismo
 * denunciado. El modelo en sí no se modifica (es de otro compañero). Si la URL no está configurada el motor
 * queda deshabilitado, y si falla, AntecedentesService usa el motor por reglas.
 */
@Component
public class MotorAntecedentesModelo implements MotorAntecedentes {

    private static final String ENDPOINT = "/api/antecedentes/buscar";

    private final String url;
    private final double umbral;
    private final int maxResultados;
    private final RestTemplate restTemplate;
    private final QuejaReferenciaRepository quejaRepository;

    public MotorAntecedentesModelo(
            @Value("${antecedentes.modelo.url:}") String url,
            @Value("${antecedentes.modelo.umbral:0.0}") double umbral,
            @Value("${antecedentes.modelo.max-resultados:500}") int maxResultados,
            QuejaReferenciaRepository quejaRepository
    ) {
        this.url = url == null ? "" : url.strip().replaceAll("/+$", "");
        this.umbral = umbral;
        this.maxResultados = maxResultados;
        this.quejaRepository = quejaRepository;

        // Tiempos cortos: si el modelo no contesta, se cae al motor por reglas en vez de
        // dejar la pantalla esperando.
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(3_000);
        factory.setReadTimeout(15_000);
        this.restTemplate = new RestTemplate(factory);
    }

    public boolean habilitado() {
        return !url.isEmpty();
    }

    @Override
    public String nombre() {
        return "MODELO";
    }

    @Override
    public String descripcion() {
        return "Modelo de búsqueda de antecedentes: compara la narrativa de la queja con su "
                + "índice de quejas por similitud de texto (TF-IDF).";
    }

    @Override
    public List<AntecedenteDTO> buscar(
            ExpedientePrimerContacto expediente,
            QuejaReferencia queja,
            List<QuejaReferencia> candidatas
    ) {
        String texto = NombresPersona.unir(
                expediente.getTema(),
                expediente.getDescripcionHechos(),
                queja != null ? queja.getMotivo() : null,
                queja != null ? queja.getDescripcion() : null
        );

        JsonNode respuesta = restTemplate.postForObject(
                url + ENDPOINT,
                Map.of("texto", texto == null ? "" : texto, "umbral", umbral, "topK", maxResultados),
                JsonNode.class
        );

        if (respuesta == null || !respuesta.path("antecedentes").isArray()) {
            throw new IllegalStateException("El modelo respondió sin lista de antecedentes.");
        }

        String folioPropio = expediente.getFolioOrigen();

        List<AntecedenteDTO> resultados = new ArrayList<>();
        for (JsonNode item : respuesta.path("antecedentes")) {
            JsonNode q = item.path("queja");
            String folio = texto(q, "folio");
            if (folio != null && folio.equals(folioPropio)) {
                continue;
            }

            String nombreQuejoso = persona(q.path("quejoso"));
            List<String> coincidencias = new ArrayList<>();

            String narrativa = texto(q, "texto_original");

            resultados.add(AntecedenteDTO.builder()
                    .folioQueja(folio)
                    .folioPrimerContacto(folio == null ? null : quejaRepository.findByNumeroFolio(folio)
                            .map(QuejaReferencia::getFolioPrimerContacto)
                            .orElse(null))
                    .fecha(texto(q, "fecha_hechos"))
                    .asunto(etiqueta(texto(q, "tipo_violencia")))
                    .extracto(extracto(narrativa))
                    .descripcion(narrativa)
                    .unidadAcademica(texto(q, "unidad_academica"))
                    .nombreQuejoso(nombreQuejoso)
                    .nombreDenunciado(persona(q.path("denunciado")))
                    .correoQuejoso(texto(q.path("quejoso"), "correo"))
                    .identificacionQuejoso(texto(q.path("quejoso"), "numero_identificacion"))
                    .estatus(texto(q, "estatus"))
                    .similitud((int) Math.round(Math.min(1.0, item.path("similitud").asDouble()) * 100))
                    .coincidencias(coincidencias)
                    .origen(item.path("historico").asBoolean(false)
                            ? NombresPersona.ORIGEN_HISTORICO
                            : NombresPersona.ORIGEN_SISTEMA)
                    .build());
        }
        return resultados;
    }

    private static String texto(JsonNode nodo, String campo) {
        JsonNode valor = nodo.path(campo);
        return valor.isMissingNode() || valor.isNull() || valor.asText().isBlank() ? null : valor.asText();
    }

    private static String persona(JsonNode p) {
        return NombresPersona.unir(texto(p, "nombre"), texto(p, "apellido1"), texto(p, "apellido2"));
    }

    /** ACOSO_ESCOLAR → "Acoso escolar". */
    private static String etiqueta(String valor) {
        if (valor == null) {
            return null;
        }
        String texto = valor.replace('_', ' ').toLowerCase();
        return Character.toUpperCase(texto.charAt(0)) + texto.substring(1);
    }

    private static String extracto(String texto) {
        if (texto == null) {
            return null;
        }
        String limpio = texto.strip();
        return limpio.length() <= 220 ? limpio : limpio.substring(0, 217) + "…";
    }
}
