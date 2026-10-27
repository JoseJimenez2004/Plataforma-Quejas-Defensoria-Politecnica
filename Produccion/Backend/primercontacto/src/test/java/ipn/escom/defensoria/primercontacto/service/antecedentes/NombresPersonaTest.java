package ipn.escom.defensoria.primercontacto.service.antecedentes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Pruebas unitarias de {@link NombresPersona}: comparación de nombres sin acentos ni mayúsculas. */
@DisplayName("NombresPersona — comparación de nombres")
class NombresPersonaTest {

    @Test
    void unir_omiteVaciosYEspacios() {
        assertEquals("Ana López", NombresPersona.unir(" Ana ", null, "", "López"), "Une sin huecos");
    }

    @Test
    void unir_todoVacio_regresaNull() {
        assertNull(NombresPersona.unir(null, " "), "Sin partes no hay nombre");
    }

    @Test
    void normalizar_quitaAcentosPeroConservaLaEnie() {
        assertEquals("jose nuñez perez", NombresPersona.normalizar("  JOSÉ   Núñez-Pérez "), "Sin acentos, con ñ");
    }

    @Test
    void normalizar_nulo_cadenaVacia() {
        assertEquals("", NombresPersona.normalizar(null), "Nulo se trata como vacío");
    }

    @Test
    void mismoNombre_subconjuntoDeDosPalabras_esLaMismaPersona() {
        assertTrue(NombresPersona.mismoNombre("Ana López", "ANA LOPEZ RUIZ"), "Las palabras del corto están en el largo");
    }

    @Test
    void mismoNombre_unaSolaPalabra_noBasta() {
        assertFalse(NombresPersona.mismoNombre("Ana", "Ana López"), "Con una palabra no se confunde a dos Ana");
    }

    @Test
    void mismoNombre_apellidoDistinto_noEsLaMisma() {
        assertFalse(NombresPersona.mismoNombre("Ana López", "Ana Ruiz"), "Apellido distinto");
    }

    @Test
    void mismoDato_ignoraMayusculasYEspacios() {
        assertTrue(NombresPersona.mismoDato(" 2021630001 ", "2021630001"), "Mismo dato");
        assertFalse(NombresPersona.mismoDato(null, "x"), "Nulo nunca coincide");
        assertFalse(NombresPersona.mismoDato(" ", " "), "Vacío nunca coincide");
    }

    @Test
    void coincide_todasLasPalabrasEnCualquierOrden() {
        List<String> palabras = NombresPersona.palabras("lopez ana");
        assertTrue(NombresPersona.coincide("Ana López Ruiz", palabras), "El orden no importa");
        assertFalse(NombresPersona.coincide("Ana Ruiz", palabras), "Falta una palabra");
        assertFalse(NombresPersona.coincide("Ana López", List.of()), "Sin palabras no hay coincidencia");
    }
}
