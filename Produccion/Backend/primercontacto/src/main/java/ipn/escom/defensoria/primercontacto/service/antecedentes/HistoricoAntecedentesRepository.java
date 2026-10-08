package ipn.escom.defensoria.primercontacto.service.antecedentes;

import ipn.escom.defensoria.primercontacto.dto.AntecedenteDTO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.stereotype.Component;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Lectura de casos HISTÓRICOS (base historico_db, tabla quejas_historicas, dueño:
 * historico-service) para la búsqueda manual por quejoso o denunciado.
 *
 * Se lee directo y en solo lectura porque la API interna de historico-service solo busca al
 * quejoso por nombre exacto (no al denunciado). A propósito NO se registra como bean un
 * DataSource: eso desplazaría al DataSource principal (defensoria_db) que autoconfigura
 * Spring Boot. Si historico.datasource.url está vacío, la búsqueda solo usa el sistema.
 */
@Component
public class HistoricoAntecedentesRepository {

    private static final int MAX_FILAS = 200;

    /*
     * Filtro grueso en SQL (minúsculas y sin acentos); el filtro fino con las mismas reglas
     * que el resto de la búsqueda se hace en Java (NombresPersona.coincide).
     */
    private static final String QUITAR_ACENTOS = "translate(lower(%s), 'áéíóúü', 'aeiouu')";
    private static final String NOMBRE_QUEJOSO =
            "concat_ws(' ', quejoso_nombre, quejoso_apellido1, quejoso_apellido2)";
    private static final String NOMBRE_DENUNCIADO =
            "concat_ws(' ', denunciado_nombre, denunciado_apellido1, denunciado_apellido2)";

    private final JdbcTemplate jdbc;

    public HistoricoAntecedentesRepository(
            @Value("${historico.datasource.url:}") String url,
            @Value("${historico.datasource.username:postgres}") String username,
            @Value("${historico.datasource.password:}") String password
    ) {
        if (url == null || url.isBlank()) {
            this.jdbc = null;
            return;
        }
        DriverManagerDataSource dataSource = new DriverManagerDataSource(url, username, password);
        dataSource.setDriverClassName("org.postgresql.Driver");
        this.jdbc = new JdbcTemplate(dataSource);
        this.jdbc.setQueryTimeout(10);
    }

    public boolean habilitado() {
        return jdbc != null;
    }

    /**
     * Casos históricos donde el quejoso coincide con palabrasQuejoso O el denunciado con
     * palabrasDenunciado (las que no estén vacías).
     */
    public List<AntecedenteDTO> buscar(List<String> palabrasQuejoso, List<String> palabrasDenunciado) {
        List<String> condiciones = new ArrayList<>();
        List<Object> parametros = new ArrayList<>();
        agregarCondicion(condiciones, parametros, NOMBRE_QUEJOSO, palabrasQuejoso);
        agregarCondicion(condiciones, parametros, NOMBRE_DENUNCIADO, palabrasDenunciado);
        if (condiciones.isEmpty()) {
            return List.of();
        }

        String sql = "SELECT folio, fecha_presentacion_original, unidad_academica_clave, motivo, "
                + "descripcion, estatus, resultado, quejoso_nombre, quejoso_apellido1, "
                + "quejoso_apellido2, denunciado_nombre, denunciado_apellido1, denunciado_apellido2 "
                + "FROM quejas_historicas WHERE " + String.join(" OR ", condiciones)
                + " ORDER BY fecha_presentacion_original DESC NULLS LAST LIMIT " + MAX_FILAS;

        return jdbc.query(sql, (rs, i) -> convertir(rs), parametros.toArray());
    }

    private void agregarCondicion(
            List<String> condiciones,
            List<Object> parametros,
            String columnaNombre,
            List<String> palabras
    ) {
        if (palabras.isEmpty()) {
            return;
        }
        List<String> partes = new ArrayList<>();
        for (String palabra : palabras) {
            partes.add(QUITAR_ACENTOS.formatted(columnaNombre) + " LIKE ?");
            parametros.add("%" + palabra + "%");
        }
        condiciones.add("(" + String.join(" AND ", partes) + ")");
    }

    private AntecedenteDTO convertir(ResultSet rs) throws SQLException {
        java.sql.Date fecha = rs.getDate("fecha_presentacion_original");
        String descripcion = rs.getString("descripcion");
        return AntecedenteDTO.builder()
                .folioQueja(rs.getString("folio"))
                .fecha(fecha != null ? fecha.toLocalDate().toString() : null)
                .unidadAcademica(rs.getString("unidad_academica_clave"))
                .asunto(rs.getString("motivo"))
                .descripcion(descripcion)
                .estatus(rs.getString("estatus"))
                .resultado(rs.getString("resultado"))
                .nombreQuejoso(NombresPersona.unir(rs.getString("quejoso_nombre"),
                        rs.getString("quejoso_apellido1"), rs.getString("quejoso_apellido2")))
                .nombreDenunciado(NombresPersona.unir(rs.getString("denunciado_nombre"),
                        rs.getString("denunciado_apellido1"), rs.getString("denunciado_apellido2")))
                .origen(NombresPersona.ORIGEN_HISTORICO)
                .build();
    }
}
