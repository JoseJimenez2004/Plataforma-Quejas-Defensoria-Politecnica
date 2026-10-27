package ipn.escom.defensoria.queja_service.validacion;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import ipn.escom.defensoria.queja_service.dto.RegistroQuejaPublicaRequest;

/** Pruebas unitarias de {@link ValidadorQuejaPublica} — formulario público CU-Q01. */
@DisplayName("ValidadorQuejaPublica — reglas del formulario público")
class ValidadorQuejaPublicaTest {

    private static final int NOMBRE_MIN = ReglasQueja.NOMBRE_LONGITUD_MINIMA;
    private static final int NOMBRE_MAX = ReglasQueja.NOMBRE_LONGITUD_MAXIMA;
    private static final int DESCRIPCION_MIN = ReglasQueja.DESCRIPCION_LONGITUD_MINIMA;
    private static final int BOLETA_MAX = ReglasQueja.NUMERO_IDENTIFICACION_LONGITUD_MAXIMA;

    private ValidadorQuejaPublica validador;
    private RegistroQuejaPublicaRequest datos;

    @BeforeEach
    void setUp() {
        validador = new ValidadorQuejaPublica();
        datos = new RegistroQuejaPublicaRequest();
        datos.setAvisoPrivacidadAceptado(Boolean.TRUE);
        datos.setNombre("María José");
        datos.setApellido1("Pérez-Gómez");
        datos.setApellido2("D'Angelo");
        datos.setCorreo("maria.perez@ipn.mx");
        datos.setFechaNacimiento(LocalDate.of(2003, 4, 15));
        datos.setTipoIdentificacion("alumno");
        datos.setNumeroIdentificacion("2021630001");
        datos.setUnidadAcademicaClave("ESCOM");
        datos.setFechaHechos(LocalDate.of(2024, 9, 1));
        datos.setDescripcion("Descripción suficientemente larga de los hechos.");
        datos.setArchivos(List.of(ArchivosPrueba.identificacion("ine.jpg", ArchivosPrueba.JPEG)));
    }

    @Test
    void validar_datosCompletos_noLanzaYClasificaArchivos() {
        ValidadorArchivos.ArchivosClasificados r = validador.validar(datos);
        assertEquals(1, r.identificaciones().size(), "La credencial se clasifica aparte");
    }

    @Test
    void validar_normalizaEspaciosYCorreo() {
        datos.setNombre("  Juan    Carlos ");
        datos.setCorreo("  Juan.Carlos@IPN.mx ");
        datos.setApellido2("   ");

        validador.validar(datos);

        assertEquals("Juan Carlos", datos.getNombre(), "Espacios colapsados");
        assertEquals("juan.carlos@ipn.mx", datos.getCorreo(), "Correo en minúsculas");
        assertNull(datos.getApellido2(), "Un campo opcional en blanco queda en null");
    }

    @Test
    void validar_sinAceptarAviso_lanzaExcepcion() {
        datos.setAvisoPrivacidadAceptado(Boolean.FALSE);
        ValidacionException ex = assertThrows(ValidacionException.class, () -> validador.validar(datos));
        assertTrue(ex.getMessage().contains("aviso de privacidad"), "Debe exigir el aviso");
    }

    @Test
    void validar_avisoNulo_lanzaExcepcion() {
        datos.setAvisoPrivacidadAceptado(null);
        assertThrows(ValidacionException.class, () -> validador.validar(datos), "null no equivale a aceptar");
    }

    @Test
    void validar_sinNombre_lanzaFaltaNombre() {
        datos.setNombre(null);
        ValidacionException ex = assertThrows(ValidacionException.class, () -> validador.validar(datos));
        assertEquals("Falta el nombre.", ex.getMessage(), "Nombre obligatorio");
    }

    @Test
    void validar_nombreDeUnaLetra_lanzaExcepcion() {
        datos.setNombre("a".repeat(NOMBRE_MIN - 1));
        ValidacionException ex = assertThrows(ValidacionException.class, () -> validador.validar(datos));
        assertTrue(ex.getMessage().contains("al menos 2 letras"), "Debajo del mínimo");
    }

    @Test
    void validar_nombreEnLimites_esValido() {
        datos.setNombre("a".repeat(NOMBRE_MIN));
        datos.setApellido1("b".repeat(NOMBRE_MAX));
        assertDoesNotThrow(() -> validador.validar(datos), "2 y 50 caracteres son bordes válidos");
    }

    @Test
    void validar_apellidoDe51Caracteres_lanzaExcepcion() {
        datos.setApellido1("b".repeat(NOMBRE_MAX + 1));
        ValidacionException ex = assertThrows(ValidacionException.class, () -> validador.validar(datos));
        assertTrue(ex.getMessage().contains("no puede exceder 50"), "Arriba del máximo");
    }

    @ParameterizedTest
    @ValueSource(strings = {"Juan2", "Ana@", "--Luis", "José_"})
    void validar_nombreConSimbolosNoPermitidos_lanzaExcepcion(String nombre) {
        datos.setNombre(nombre);
        ValidacionException ex = assertThrows(ValidacionException.class, () -> validador.validar(datos));
        assertTrue(ex.getMessage().contains("solo se permiten letras"), "Rechaza: " + nombre);
    }

    @Test
    void validar_denunciadoOpcionalAusente_esValido() {
        datos.setNombreDenunciado(null);
        datos.setApellido1Denunciado(" ");
        assertDoesNotThrow(() -> validador.validar(datos), "El denunciado puede omitirse");
    }

    @Test
    void validar_denunciadoConNumeros_lanzaExcepcion() {
        datos.setNombreDenunciado("Prof 3");
        ValidacionException ex = assertThrows(ValidacionException.class, () -> validador.validar(datos));
        assertTrue(ex.getMessage().contains("del denunciado"), "El mensaje identifica el campo");
    }

    @ParameterizedTest
    @ValueSource(strings = {"ALUMNO", "Empleado"})
    void validar_tipoIdentificacionSinImportarMayusculas_esValido(String tipo) {
        datos.setTipoIdentificacion(tipo);
        assertDoesNotThrow(() -> validador.validar(datos), "Acepta " + tipo);
    }

    @Test
    void validar_tipoIdentificacionDesconocido_lanzaExcepcion() {
        datos.setTipoIdentificacion("externo");
        ValidacionException ex = assertThrows(ValidacionException.class, () -> validador.validar(datos));
        assertEquals("Indica si eres alumno o empleado.", ex.getMessage(), "Solo alumno o empleado");
    }

    @Test
    void validar_boletaDeDiezDigitos_esValida() {
        datos.setNumeroIdentificacion("9".repeat(BOLETA_MAX));
        assertDoesNotThrow(() -> validador.validar(datos), "10 dígitos es el máximo válido");
    }

    @Test
    void validar_boletaDeOnceDigitos_lanzaExcepcion() {
        datos.setNumeroIdentificacion("9".repeat(BOLETA_MAX + 1));
        ValidacionException ex = assertThrows(ValidacionException.class, () -> validador.validar(datos));
        assertTrue(ex.getMessage().startsWith("El número de boleta"), "Mensaje para alumno");
    }

    @Test
    void validar_numeroEmpleadoConLetras_mensajeDeEmpleado() {
        datos.setTipoIdentificacion("empleado");
        datos.setNumeroIdentificacion("12A45");
        ValidacionException ex = assertThrows(ValidacionException.class, () -> validador.validar(datos));
        assertTrue(ex.getMessage().startsWith("El número de empleado"), "Mensaje para empleado");
    }

    @Test
    void validar_sinNumeroIdentificacion_lanzaExcepcion() {
        datos.setNumeroIdentificacion("  ");
        ValidacionException ex = assertThrows(ValidacionException.class, () -> validador.validar(datos));
        assertEquals("Falta el número de boleta.", ex.getMessage(), "Número obligatorio");
    }

    @Test
    void validar_sinUnidadAcademica_lanzaExcepcion() {
        datos.setUnidadAcademicaClave("");
        ValidacionException ex = assertThrows(ValidacionException.class, () -> validador.validar(datos));
        assertEquals("Selecciona el lugar donde sucedieron los hechos.", ex.getMessage(), "Unidad obligatoria");
    }

    @Test
    void validar_descripcionEnElMinimo_esValida() {
        datos.setDescripcion("x".repeat(DESCRIPCION_MIN));
        assertDoesNotThrow(() -> validador.validar(datos), "20 caracteres es el borde válido");
    }

    @Test
    void validar_descripcionUnCaracterDebajoDelMinimo_lanzaExcepcion() {
        datos.setDescripcion("x".repeat(DESCRIPCION_MIN - 1));
        ValidacionException ex = assertThrows(ValidacionException.class, () -> validador.validar(datos));
        assertTrue(ex.getMessage().contains("al menos 20 caracteres"), "19 caracteres no alcanza");
    }

    @Test
    void validar_descripcionConEspaciosAlrededor_cuentaSinEspacios() {
        datos.setDescripcion("   " + "x".repeat(DESCRIPCION_MIN - 1) + "   ");
        assertThrows(ValidacionException.class, () -> validador.validar(datos),
                "Los espacios exteriores no cuentan para el mínimo");
    }

    @Test
    void validar_sinDescripcion_lanzaExcepcion() {
        datos.setDescripcion(null);
        ValidacionException ex = assertThrows(ValidacionException.class, () -> validador.validar(datos));
        assertEquals("Falta la descripción de los hechos.", ex.getMessage(), "Descripción obligatoria");
    }
}
