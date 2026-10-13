package ipn.escom.defensoria.primercontacto.service.antecedentes;

import com.fasterxml.jackson.databind.JsonNode;
import ipn.escom.defensoria.primercontacto.dto.AntecedenteDTO;
import ipn.escom.defensoria.primercontacto.entity.ExpedientePrimerContacto;
import ipn.escom.defensoria.primercontacto.entity.QuejaReferencia;
import ipn.escom.defensoria.primercontacto.repository.QuejaReferenciaRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Motor que consulta el MODELO de antecedentes.
 *
 * MODELO V5 (Python, Backend/antecedentes_service, mismo puerto 8093): buscarV5().
 *
 *   POST {antecedentes.modelo.url}/api/v5/antecedentes/buscar
 *        { "descripcion", "folio", "origen", "fechaHechos", "unidadAcademicaClave",
 *          "quejoso": { nombre, apellido1, apellido2, numeroIdentificacion, correo },
 *          "denunciado": { nombre, apellido1, apellido2 }, "topK" }
 *   ->   { "por_hechos": [...], "por_involucrados": [...], "coincidencias_fuertes": [...] }
 *
 * Lee directo las quejas del sistema y las históricas. "por_involucrados" solo trae folio y
 * banderas de coincidencia: los datos de cada queja se toman de "por_hechos", por eso se
 * pide un topK alto (todas las candidatas quedan en "por_hechos").
 *
 * Si el modelo desplegado todavía es el anterior (sin la ruta V5), se usa buscar():
 *
 * MODELO ANTERIOR (antecedentes-service, proyecto Modelo-Java):
 *
 *   POST {antecedentes.modelo.url}/api/antecedentes/buscar
 *        { "texto": "...", "umbral": 0.0, "topK": 500 }
 *   →    { "antecedentes": [ { "queja": {...}, "similitud": 0.42, "historico": true } ] }
 *        (versión del repo) o, en la versión desplegada, cada item "plano":
 *        { "folio", "similitud", "historico", "quejoso": {...}, "denunciado": {...}, "fragmento", ... }
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
    private static final String ENDPOINT_V5 = "/api/v5/antecedentes/buscar";

    private final String url;
    private final double umbral;
    private final int maxResultados;
    private final RestTemplate restTemplate;
    private final QuejaReferenciaRepository quejaRepository;

    public MotorAntecedentesModelo(
            @Value("${antecedentes.modelo.url:}") String url,
            @Value("${antecedentes.modelo.umbral:0.0}") double umbral,
            @Value("${antecedentes.modelo.max-resultados:500}") int maxResultados,
            @Value("${antecedentes.modelo.timeout-segundos:45}") int timeoutSegundos,
            QuejaReferenciaRepository quejaRepository
    ) {
        this.url = url == null ? "" : url.strip().replaceAll("/+$", "");
        this.umbral = umbral;
        this.maxResultados = maxResultados;
        this.quejaRepository = quejaRepository;

        // La primera consulta tras un rato sin uso tarda más (el modelo se "despierta"); con
        // 15 s se caía al motor por reglas en ese primer intento. 45 s por defecto deja margen
        // sin pasar los 60 s con que nginx corta la petición.
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(3_000);
        factory.setReadTimeout(timeoutSegundos * 1_000);
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
        return "Modelo de búsqueda de antecedentes: compara la narrativa de la queja con las "
                + "quejas del sistema y los casos históricos, y reconoce a las mismas personas.";
    }

    /** Las tres listas del modelo V5, ya convertidas. */
    public record ResultadoV5(
            List<AntecedenteDTO> porHechos,
            List<AntecedenteDTO> porInvolucrados,
            List<AntecedenteDTO> coincidenciasFuertes
    ) {
    }

    /**
     * Consulta el modelo V5. Devuelve null si el modelo desplegado no tiene la ruta V5 (404),
     * para que el servicio use la ruta anterior; cualquier otro error se propaga.
     */
    public ResultadoV5 buscarV5(ExpedientePrimerContacto expediente, QuejaReferencia queja) {

        Map<String, Object> peticion = new LinkedHashMap<>();
        String texto = narrativa(expediente, queja);
        peticion.put("descripcion", texto == null || texto.isBlank() ? "(sin narrativa)" : texto);
        peticion.put("folio", expediente.getFolioOrigen());
        peticion.put("origen", NombresPersona.ORIGEN_SISTEMA);
        peticion.put("topK", maxResultados);
        if (queja != null) {
            if (queja.getFechaHechos() != null) {
                peticion.put("fechaHechos", queja.getFechaHechos().toString());
            }
            if (queja.getUnidadAcademicaClave() != null) {
                peticion.put("unidadAcademicaClave", queja.getUnidadAcademicaClave());
            }
            peticion.put("quejoso", sinVacios(
                    "nombre", queja.getNombreQuejoso(),
                    "apellido1", queja.getApellido1Quejoso(),
                    "apellido2", queja.getApellido2Quejoso(),
                    "numeroIdentificacion", queja.getNumeroIdentificacionQuejoso(),
                    "correo", queja.getCorreoInstitucional()));
            peticion.put("denunciado", sinVacios(
                    "nombre", queja.getNombreDenunciado(),
                    "apellido1", queja.getApellido1Denunciado(),
                    "apellido2", queja.getApellido2Denunciado()));
        } else {
            peticion.put("quejoso", sinVacios(
                    "nombre", expediente.getQuejosoNombre(),
                    "correo", expediente.getQuejosoCorreo()));
        }

        JsonNode respuesta;
        try {
            respuesta = restTemplate.postForObject(url + ENDPOINT_V5, peticion, JsonNode.class);
        } catch (HttpClientErrorException.NotFound ex) {
            return null;
        }

        if (respuesta == null || !respuesta.path("por_hechos").isArray()) {
            throw new IllegalStateException("El modelo V5 respondió sin la lista por_hechos.");
        }

        String folioPropio = expediente.getFolioOrigen();

        // Datos completos de cada candidata (por_hechos las trae todas, ordenadas por hechos).
        Map<String, AntecedenteDTO> porClave = new HashMap<>();
        List<AntecedenteDTO> porHechos = new ArrayList<>();
        for (JsonNode item : respuesta.path("por_hechos")) {
            AntecedenteDTO dto = convertirV5(item);
            if (dto.getFolioQueja() == null || esPropia(dto, folioPropio)) {
                continue;
            }
            porClave.put(clave(dto), dto);
            porHechos.add(dto);
        }

        List<AntecedenteDTO> porInvolucrados = new ArrayList<>();
        for (JsonNode item : respuesta.path("por_involucrados")) {
            AntecedenteDTO base = porClave.get(clave(texto(item, "origen"), texto(item, "folio")));
            AntecedenteDTO dto = base != null
                    ? base.toBuilder().coincidencias(new ArrayList<>(base.getCoincidencias())).build()
                    : AntecedenteDTO.builder()
                            .folioQueja(texto(item, "folio"))
                            .origen(origen(texto(item, "origen")))
                            .fecha(texto(item, "fechaHechos"))
                            .coincidencias(new ArrayList<>())
                            .build();
            if (dto.getFolioQueja() == null || esPropia(dto, folioPropio)) {
                continue;
            }
            marcarPersonas(dto, item);
            porInvolucrados.add(dto);
        }

        List<AntecedenteDTO> fuertes = new ArrayList<>();
        for (JsonNode item : respuesta.path("coincidencias_fuertes")) {
            AntecedenteDTO dto = convertirV5(item);
            if (dto.getFolioQueja() == null || esPropia(dto, folioPropio)) {
                continue;
            }
            marcarPersonas(dto, item);
            dto.setRazonCoincidencia(texto(item, "razon_coincidencia_fuerte"));
            fuertes.add(dto);
        }

        return new ResultadoV5(porHechos, porInvolucrados, fuertes);
    }

    private AntecedenteDTO convertirV5(JsonNode item) {
        String folio = texto(item, "folio");
        String narrativa = texto(item, "descripcion");
        String origen = origen(texto(item, "origen"));
        String nivel = texto(item, "nivel_similitud");
        List<String> coincidencias = new ArrayList<>();
        if (nivel != null) {
            coincidencias.add("Hechos: similitud " + nivel.toLowerCase());
        }
        return AntecedenteDTO.builder()
                .folioQueja(folio)
                .folioPrimerContacto(folio == null || NombresPersona.ORIGEN_HISTORICO.equals(origen)
                        ? null
                        : quejaRepository.findByNumeroFolio(folio)
                                .map(QuejaReferencia::getFolioPrimerContacto)
                                .orElse(null))
                .fecha(texto(item, "fechaHechos"))
                .asunto(etiqueta(texto(item, "motivo")))
                .extracto(extracto(narrativa))
                .descripcion(narrativa)
                .unidadAcademica(texto(item, "unidadAcademicaClave"))
                .nombreQuejoso(persona(item.path("quejoso")))
                .nombreDenunciado(persona(item.path("denunciado")))
                .correoQuejoso(texto(item.path("quejoso"), "correo"))
                .identificacionQuejoso(texto(item.path("quejoso"), "numeroIdentificacion"))
                .estatus(texto(item, "estatus"))
                .resultado(texto(item, "resultado"))
                .similitud((int) Math.round(
                        Math.max(0, Math.min(1.0, item.path("similitud_hechos").asDouble())) * 100))
                .nivelSimilitud(nivel)
                .coincidencias(coincidencias)
                .origen(origen)
                .build();
    }

    /** Banderas de persona que traen por_involucrados y coincidencias_fuertes. */
    private static void marcarPersonas(AntecedenteDTO dto, JsonNode item) {
        boolean idQuejoso = item.path("mismo_quejoso").asBoolean(false);
        boolean idDenunciado = item.path("mismo_denunciado").asBoolean(false);
        boolean nombreQuejoso = item.path("coincide_nombre_quejoso").asBoolean(false);
        boolean nombreDenunciado = item.path("coincide_nombre_denunciado").asBoolean(false);

        dto.setMismoQuejoso(idQuejoso || nombreQuejoso);
        dto.setMismoDenunciado(idDenunciado || nombreDenunciado);
        dto.setIdentidadVerificada(idQuejoso || idDenunciado);
        dto.setTipoCoincidencia(texto(item, "tipo_coincidencia"));
        JsonNode dias = item.path("dias_diferencia");
        dto.setDiasDiferencia(dias.isNumber() ? dias.asInt() : null);

        List<String> coincidencias = new ArrayList<>();
        if (dto.getTipoCoincidencia() != null) {
            // "Identificación del quejoso + Nombre del denunciado (por verificar)" → dos etiquetas.
            coincidencias.addAll(List.of(dto.getTipoCoincidencia().split(" \\+ ")));
        }
        if (dto.getCoincidencias() != null) {
            coincidencias.addAll(dto.getCoincidencias());
        }
        dto.setCoincidencias(coincidencias);
    }

    private static boolean esPropia(AntecedenteDTO dto, String folioPropio) {
        return NombresPersona.ORIGEN_SISTEMA.equals(dto.getOrigen())
                && dto.getFolioQueja().equals(folioPropio);
    }

    /** origen:folio, la misma clave con que el front distingue un antecedente. */
    static String clave(AntecedenteDTO dto) {
        return clave(dto.getOrigen(), dto.getFolioQueja());
    }

    private static String clave(String origen, String folio) {
        return origen(origen) + ":" + folio;
    }

    private static String origen(String valor) {
        return NombresPersona.ORIGEN_HISTORICO.equalsIgnoreCase(valor)
                ? NombresPersona.ORIGEN_HISTORICO
                : NombresPersona.ORIGEN_SISTEMA;
    }

    /** Mapa con los pares clave/valor cuyo valor no está vacío. */
    private static Map<String, Object> sinVacios(String... paresClaveValor) {
        Map<String, Object> mapa = new LinkedHashMap<>();
        for (int i = 0; i + 1 < paresClaveValor.length; i += 2) {
            String valor = paresClaveValor[i + 1];
            if (valor != null && !valor.isBlank()) {
                mapa.put(paresClaveValor[i], valor.strip());
            }
        }
        return mapa;
    }

    private static String narrativa(ExpedientePrimerContacto expediente, QuejaReferencia queja) {
        return NombresPersona.unir(
                expediente.getTema(),
                expediente.getDescripcionHechos(),
                queja != null ? queja.getMotivo() : null,
                queja != null ? queja.getDescripcion() : null
        );
    }

    @Override
    public List<AntecedenteDTO> buscar(
            ExpedientePrimerContacto expediente,
            QuejaReferencia queja,
            List<QuejaReferencia> candidatas
    ) {
        String texto = narrativa(expediente, queja);

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
            // Dos formatos: el del repo trae los datos dentro de "queja"; el desplegado en el
            // servidor los trae "planos" en el mismo item (y la narrativa en "fragmento").
            JsonNode q = item.has("queja") ? item.path("queja") : item;
            String folio = texto(q, "folio");
            if (folio != null && folio.equals(folioPropio)) {
                continue;
            }

            String nombreQuejoso = persona(q.path("quejoso"));
            List<String> coincidencias = new ArrayList<>();

            String narrativa = texto(q, "texto_original") != null
                    ? texto(q, "texto_original")
                    : texto(q, "fragmento");

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
