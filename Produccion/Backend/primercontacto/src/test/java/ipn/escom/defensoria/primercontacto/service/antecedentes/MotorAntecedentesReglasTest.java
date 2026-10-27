package ipn.escom.defensoria.primercontacto.service.antecedentes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ipn.escom.defensoria.primercontacto.dto.AntecedenteDTO;
import ipn.escom.defensoria.primercontacto.entity.ExpedientePrimerContacto;
import ipn.escom.defensoria.primercontacto.entity.QuejaReferencia;

/**
 * Pruebas unitarias de {@link MotorAntecedentesReglas}. Puntaje: mismo quejoso 45, misma
 * unidad 15, hechos similares hasta 40; umbral 25; máximo 15 resultados.
 */
@DisplayName("MotorAntecedentesReglas — búsqueda provisional de antecedentes")
class MotorAntecedentesReglasTest {

    private static final int PUNTOS_MISMO_QUEJOSO = 45;
    private static final int PUNTOS_MISMA_UNIDAD = 15;
    private static final int MAX_RESULTADOS = 15;
    private static final String CORREO = "quejoso@ipn.mx";

    private MotorAntecedentesReglas motor;
    private ExpedientePrimerContacto expediente;

    @BeforeEach
    void setUp() {
        motor = new MotorAntecedentesReglas();
        expediente = new ExpedientePrimerContacto();
        expediente.setQuejosoCorreo(CORREO);
        expediente.setUnidadAcademica("ESCOM");
        expediente.setTema("Acoso del profesor");
        expediente.setDescripcionHechos("El profesor gritaba insultos durante la clase de cálculo");
    }

    private static QuejaReferencia candidata(String folio, String correo, String unidad, String descripcion) {
        QuejaReferencia q = new QuejaReferencia();
        q.setNumeroFolio(folio);
        q.setCorreoInstitucional(correo);
        q.setUnidadAcademicaClave(unidad);
        q.setMotivo("Queja");
        q.setDescripcion(descripcion);
        q.setFechaCreacion(LocalDateTime.of(2026, 1, 1, 10, 0));
        return q;
    }

    @Test
    void buscar_mismoQuejoso_siempreApareceAunqueNoSumeUmbral() {
        List<AntecedenteDTO> r = motor.buscar(expediente, null,
                List.of(candidata("FOL-1", "QUEJOSO@ipn.mx", "UPIITA", "Tema sin relación alguna")));

        assertEquals(1, r.size(), "El mismo quejoso siempre es antecedente");
        assertEquals(PUNTOS_MISMO_QUEJOSO, r.get(0).getSimilitud(), "45 puntos por mismo quejoso");
        assertTrue(r.get(0).isMismoQuejoso(), "Marcado como mismo quejoso");
    }

    @Test
    void buscar_soloMismaUnidad_noAlcanzaElUmbral() {
        List<AntecedenteDTO> r = motor.buscar(expediente, null,
                List.of(candidata("FOL-2", "otro@ipn.mx", "ESCOM", "Tema sin relación alguna")));
        assertTrue(r.isEmpty(), PUNTOS_MISMA_UNIDAD + " puntos no alcanzan el umbral de 25");
    }

    @Test
    void buscar_mismaUnidadYHechosSimilares_apareceConCoincidencias() {
        List<AntecedenteDTO> r = motor.buscar(expediente, null,
                List.of(candidata("FOL-3", "otro@ipn.mx", "escom", "El profesor gritaba insultos a todo el grupo")));

        assertEquals(1, r.size(), "Unidad + texto superan el umbral");
        assertTrue(r.get(0).getSimilitud() > PUNTOS_MISMA_UNIDAD, "Suma puntos por texto");
        assertTrue(r.get(0).getCoincidencias().stream().anyMatch(c -> c.startsWith("Hechos similares")),
                "Explica por qué salió");
    }

    @Test
    void buscar_ordenaDeMayorAMenorSimilitud() {
        List<AntecedenteDTO> r = motor.buscar(expediente, null, List.of(
                candidata("FOL-BAJA", "otro@ipn.mx", "ESCOM", "El profesor gritaba insultos"),
                candidata("FOL-ALTA", CORREO, "ESCOM", "El profesor gritaba insultos durante la clase")));
        assertEquals("FOL-ALTA", r.get(0).getFolioQueja(), "Primero la de mayor puntaje");
    }

    @Test
    void buscar_similitudNuncaPasaDe100() {
        List<AntecedenteDTO> r = motor.buscar(expediente, null, List.of(
                candidata("FOL-4", CORREO, "ESCOM", "Acoso profesor gritaba insultos durante clase cálculo")));
        assertTrue(r.get(0).getSimilitud() <= 100, "Tope de 100");
    }

    @Test
    void buscar_masDeQuinceCandidatas_limitaA15() {
        List<QuejaReferencia> candidatas = new ArrayList<>();
        for (int i = 0; i < MAX_RESULTADOS + 5; i++) {
            candidatas.add(candidata("FOL-" + i, CORREO, "ESCOM", "texto"));
        }
        assertEquals(MAX_RESULTADOS, motor.buscar(expediente, null, candidatas).size(), "Máximo 15 resultados");
    }

    @Test
    void buscar_extractoLargo_seRecortaA220() {
        List<AntecedenteDTO> r = motor.buscar(expediente, null,
                List.of(candidata("FOL-5", CORREO, "ESCOM", "x".repeat(500))));
        assertEquals(218, r.get(0).getExtracto().length(), "217 caracteres + puntos suspensivos");
    }

    @Test
    void buscar_sinCandidatas_listaVacia() {
        assertTrue(motor.buscar(expediente, null, List.of()).isEmpty(), "Sin candidatas no hay antecedentes");
    }
}
