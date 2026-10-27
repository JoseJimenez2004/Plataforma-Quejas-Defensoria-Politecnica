package ipn.escom.defensoria.catalogo_service.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Optional;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import ipn.escom.defensoria.catalogo_service.dto.DependenciaRequest;
import ipn.escom.defensoria.catalogo_service.dto.ImportacionResumenModel;
import ipn.escom.defensoria.catalogo_service.entity.Dependencia;
import ipn.escom.defensoria.catalogo_service.repository.DependenciaRepository;

/** Pruebas unitarias de {@link DependenciaService}: RF-A04 y RNF-A04 (importación tolerante a errores). */
@ExtendWith(MockitoExtension.class)
@DisplayName("DependenciaService — catálogo de unidades")
class DependenciaServiceTest {

    private static final int NIVEL_POR_DEFECTO = 1;

    @Mock
    private DependenciaRepository repositorio;

    @InjectMocks
    private DependenciaService servicio;

    private static DependenciaRequest datos(String clave, String nombre, String tipo) {
        DependenciaRequest d = new DependenciaRequest();
        d.setClave(clave);
        d.setNombre(nombre);
        d.setTipo(tipo);
        return d;
    }

    /** Arma un .xlsx en memoria: la primera fila es el encabezado. */
    private static MockMultipartFile excel(String[]... filas) throws IOException {
        try (Workbook libro = new XSSFWorkbook(); ByteArrayOutputStream salida = new ByteArrayOutputStream()) {
            Sheet hoja = libro.createSheet();
            Row encabezado = hoja.createRow(0);
            String[] titulos = {"Clave", "Nombre", "Abreviatura", "Tipo", "Correo", "Titular"};
            for (int c = 0; c < titulos.length; c++) {
                encabezado.createCell(c).setCellValue(titulos[c]);
            }
            for (int f = 0; f < filas.length; f++) {
                Row fila = hoja.createRow(f + 1);
                for (int c = 0; c < filas[f].length; c++) {
                    if (filas[f][c] != null) {
                        fila.createCell(c).setCellValue(filas[f][c]);
                    }
                }
            }
            libro.write(salida);
            return new MockMultipartFile("archivo", "catalogo.xlsx", "application/vnd.ms-excel", salida.toByteArray());
        }
    }

    @Test
    void crear_datosCompletos_quedaActivaConNivelPorDefecto() {
        when(repositorio.findByClave("ESCOM")).thenReturn(Optional.empty());
        when(repositorio.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Dependencia d = servicio.crear(datos("ESCOM", "Escuela Superior de Cómputo", "UNIDAD_ACADEMICA"));

        assertTrue(d.getActivo(), "Una dependencia nueva queda activa");
        assertEquals(NIVEL_POR_DEFECTO, d.getNivel(), "Nivel 1 por defecto");
    }

    @Test
    void crear_sinTipo_lanzaExcepcion() {
        RuntimeException ex = assertThrows(RuntimeException.class, () -> servicio.crear(datos("ESCOM", "ESCOM", " ")));
        assertEquals("Clave, nombre y tipo son obligatorios.", ex.getMessage(), "Tipo obligatorio");
        verify(repositorio, never()).save(any());
    }

    @Test
    void crear_claveDuplicada_lanzaExcepcion() {
        when(repositorio.findByClave("ESCOM")).thenReturn(Optional.of(new Dependencia()));
        RuntimeException ex = assertThrows(RuntimeException.class, () -> servicio.crear(datos("ESCOM", "ESCOM", "UA")));
        assertEquals("Ya existe una dependencia con esa clave.", ex.getMessage(), "Clave única");
    }

    @Test
    void editar_camposVaciosNoSobrescriben() {
        Dependencia existente = new Dependencia();
        existente.setClave("ESCOM");
        existente.setNombre("Nombre original");
        when(repositorio.findByClave("ESCOM")).thenReturn(Optional.of(existente));
        when(repositorio.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Dependencia d = servicio.editar("ESCOM", datos(null, "", null));

        assertEquals("Nombre original", d.getNombre(), "Un nombre vacío no borra el actual");
        assertEquals("ESCOM", d.getClave(), "La clave se conserva");
    }

    @Test
    void editar_claveInexistente_lanzaExcepcion() {
        when(repositorio.findByClave("NOEXISTE")).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class, () -> servicio.editar("NOEXISTE", datos("X", "X", "X")), "Clave inexistente");
    }

    @Test
    void importarDesdeExcel_creaActualizaYReportaErroresSinFallarEnBloque() throws IOException {
        when(repositorio.findByClave("NUEVA")).thenReturn(Optional.empty());
        when(repositorio.findByClave("EXISTE")).thenReturn(Optional.of(new Dependencia()));
        when(repositorio.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ImportacionResumenModel r = servicio.importarDesdeExcel(excel(
                new String[] {"NUEVA", "Unidad nueva", "UN", null, "contacto@ipn.mx", "Titular"},
                new String[] {"EXISTE", "Unidad existente", "UE", "CENTRO", null, null},
                new String[] {null, "Sin clave", null, null, null, null},
                new String[] {null, null, null, null, null, null}));

        assertEquals(1, r.getFilasCreadas(), "Se creó una");
        assertEquals(1, r.getFilasActualizadas(), "Se actualizó una");
        assertEquals(1, r.getErrores().size(), "La fila sin clave se reporta; la vacía se ignora");
        assertTrue(r.getErrores().get(0).startsWith("Fila 4"), "El error indica la fila de Excel");
    }

    @Test
    void importarDesdeExcel_tipoVacio_usaUnidadAcademica() throws IOException {
        when(repositorio.findByClave("NUEVA")).thenReturn(Optional.empty());
        when(repositorio.save(any())).thenAnswer(inv -> inv.getArgument(0));

        servicio.importarDesdeExcel(excel(new String[] {"NUEVA", "Unidad", null, null, null, null}));

        ArgumentCaptor<Dependencia> guardada = ArgumentCaptor.forClass(Dependencia.class);
        verify(repositorio).save(guardada.capture());
        assertEquals("UNIDAD_ACADEMICA", guardada.getValue().getTipo(), "Tipo por defecto");
    }

    @Test
    void importarDesdeExcel_archivoQueNoEsExcel_lanzaExcepcion() {
        MockMultipartFile texto = new MockMultipartFile("archivo", "x.xlsx", "text/plain", "no soy excel".getBytes());
        RuntimeException ex = assertThrows(RuntimeException.class, () -> servicio.importarDesdeExcel(texto));
        assertTrue(ex.getMessage().startsWith("No se pudo leer el archivo de Excel"), "Mensaje claro");
        verify(repositorio, never()).findByClave(anyString());
    }
}
