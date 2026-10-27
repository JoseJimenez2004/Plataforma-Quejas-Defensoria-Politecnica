package ipn.escom.defensoria.admin_service.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import ipn.escom.defensoria.admin_service.entity.BitacoraAccion;
import ipn.escom.defensoria.admin_service.entity.PlantillaDocumento;
import ipn.escom.defensoria.admin_service.model.RespaldoResumenModel;
import ipn.escom.defensoria.admin_service.repository.BitacoraAccionRepository;
import ipn.escom.defensoria.admin_service.repository.PlantillaDocumentoRepository;

/** Bitácora (RF-A07), plantillas (RF-A05), respaldos (RF-A06) y dashboard (RF-A02). */
@DisplayName("admin-service — servicios de administración")
class ServiciosAdministracionTest {

    private static final int LONGITUD_MAXIMA_ACCION = 250;

    @Nested
    @ExtendWith(MockitoExtension.class)
    @DisplayName("BitacoraService — RNF-A02 trazabilidad con IP")
    class Bitacora {

        @Mock
        private BitacoraAccionRepository repositorio;
        @InjectMocks
        private BitacoraService servicio;

        private BitacoraAccion registrada() {
            ArgumentCaptor<BitacoraAccion> accion = ArgumentCaptor.forClass(BitacoraAccion.class);
            verify(repositorio).save(accion.capture());
            return accion.getValue();
        }

        @Test
        void registrar_conXRealIp_usaLaIpDelCliente() {
            MockHttpServletRequest req = new MockHttpServletRequest();
            req.addHeader("X-Real-IP", " 201.10.0.5 ");
            req.addHeader("X-Forwarded-For", "8.8.8.8");
            servicio.registrar("admin@ipn.mx", "Alta de usuario", req);
            assertEquals("201.10.0.5", registrada().getIp(), "X-Real-IP tiene prioridad");
        }

        @Test
        void registrar_conXForwardedFor_tomaLaPrimeraIp() {
            MockHttpServletRequest req = new MockHttpServletRequest();
            req.addHeader("X-Forwarded-For", "201.10.0.7, 10.0.0.1");
            servicio.registrar("admin@ipn.mx", "Respaldo", req);
            assertEquals("201.10.0.7", registrada().getIp(), "Primera IP de la cadena");
        }

        @Test
        void registrar_sinCabeceras_usaRemoteAddr() {
            MockHttpServletRequest req = new MockHttpServletRequest();
            req.setRemoteAddr("169.58.62.111");
            servicio.registrar("admin@ipn.mx", "Login", req);
            assertEquals("169.58.62.111", registrada().getIp(), "IP de la conexión");
        }

        @Test
        void registrar_sinPeticion_ipDesconocida() {
            servicio.registrar("sistema", "Respaldo automático", null);
            assertEquals("desconocida", registrada().getIp(), "Tareas programadas no tienen IP");
        }

        @Test
        void registrar_accionMuyLarga_seRecortaA250() {
            servicio.registrar("admin@ipn.mx", "x".repeat(LONGITUD_MAXIMA_ACCION + 1), null);
            assertEquals(LONGITUD_MAXIMA_ACCION, registrada().getAccionRealizada().length(), "Máximo 250 caracteres");
        }
    }

    @Nested
    @ExtendWith(MockitoExtension.class)
    @DisplayName("PlantillaService — plantillas oficiales")
    class Plantillas {

        @Mock
        private PlantillaDocumentoRepository repositorio;
        @InjectMocks
        private PlantillaService servicio;

        private PlantillaDocumento plantilla(String contenido) {
            PlantillaDocumento p = new PlantillaDocumento();
            p.setTipo("OFICIO");
            p.setContenido(contenido);
            return p;
        }

        @Test
        void previsualizar_reemplazaPlaceholdersConocidos() {
            when(repositorio.findByTipo("OFICIO")).thenReturn(Optional.of(plantilla("Queja {folio_queja} de {nombre_quejoso} {otro}")));
            String r = servicio.previsualizar("OFICIO");
            assertEquals("Queja DDP-2026-0421 de Juan Pérez García {otro}", r, "Los desconocidos se dejan igual");
        }

        @Test
        void obtener_tipoInexistente_lanzaExcepcion() {
            when(repositorio.findByTipo("NADA")).thenReturn(Optional.empty());
            RuntimeException ex = assertThrows(RuntimeException.class, () -> servicio.obtener("NADA"));
            assertEquals("No existe una plantilla de tipo NADA", ex.getMessage(), "Tipo inexistente");
        }

        @Test
        void actualizar_registraQuienYCuando() {
            PlantillaDocumento p = plantilla("viejo");
            when(repositorio.findByTipo("OFICIO")).thenReturn(Optional.of(p));
            when(repositorio.save(any())).thenAnswer(inv -> inv.getArgument(0));
            PlantillaDocumento r = servicio.actualizar("OFICIO", "nuevo", "admin@ipn.mx");
            assertEquals("nuevo", r.getContenido(), "Contenido nuevo");
            assertEquals("admin@ipn.mx", r.getActualizadoPor(), "Quién la editó");
        }

        @Test
        void sembrarPlantillasBase_conPlantillasExistentes_noHaceNada() {
            when(repositorio.count()).thenReturn(3L);
            servicio.sembrarPlantillasBase();
            verify(repositorio, never()).save(any());
        }

        @Test
        void sembrarPlantillasBase_baseVacia_creaTres() {
            when(repositorio.count()).thenReturn(0L);
            when(repositorio.findByTipo(any())).thenReturn(Optional.empty());
            servicio.sembrarPlantillasBase();
            verify(repositorio, times(3)).save(any());
        }

        @Test
        void placeholdersDisponibles_incluyeLosSeis() {
            assertEquals(6, servicio.placeholdersDisponibles().size(), "Seis placeholders documentados");
        }
    }

    @Nested
    @DisplayName("RespaldoService — listado y archivos (sin ejecutar pg_dump)")
    class Respaldos {

        @TempDir
        Path carpeta;

        private RespaldoService servicio;

        @BeforeEach
        void setUp() {
            servicio = new RespaldoService();
            ReflectionTestUtils.setField(servicio, "directorio", carpeta.toString());
        }

        private void archivo(String nombre, long epochSegundos) throws IOException {
            Path p = Files.writeString(carpeta.resolve(nombre), "-- respaldo");
            Files.setLastModifiedTime(p, FileTime.from(Instant.ofEpochSecond(epochSegundos)));
        }

        @Test
        void listar_soloSqlYDelMasRecienteAlMasAntiguo() throws IOException {
            archivo("viejo.sql", 1_000_000);
            archivo("nuevo.sql", 2_000_000);
            archivo("notas.txt", 3_000_000);

            List<RespaldoResumenModel> r = servicio.listar();

            assertEquals(2, r.size(), "Solo archivos .sql");
            assertEquals("nuevo.sql", r.get(0).getNombreArchivo(), "El más reciente primero");
        }

        @Test
        void ultimoRespaldoTexto_sinRespaldos_mensajeClaro() {
            assertEquals("Sin respaldos todavía", servicio.ultimoRespaldoTexto(), "Carpeta vacía");
        }

        @Test
        void restaurar_archivoInexistente_lanzaExcepcion() {
            RuntimeException ex = assertThrows(RuntimeException.class, () -> servicio.restaurar("no-existe.sql"));
            assertEquals("No se encontró el archivo de respaldo indicado.", ex.getMessage(), "No ejecuta psql");
        }

        @Test
        void obtenerArchivo_existente_regresaElRecurso() throws IOException {
            archivo("r.sql", 1_000_000);
            assertTrue(servicio.obtenerArchivo("r.sql").exists(), "Se puede descargar");
        }

        @Test
        void listar_carpetaInexistente_laCrea() {
            ReflectionTestUtils.setField(servicio, "directorio", carpeta.resolve("nueva").toString());
            assertTrue(servicio.listar().isEmpty(), "Sin respaldos");
            assertTrue(Files.isDirectory(carpeta.resolve("nueva")), "Se creó la carpeta");
        }
    }

    @Nested
    @ExtendWith(MockitoExtension.class)
    @DisplayName("DashboardService — RF-A02")
    class Dashboard {

        @Mock
        private PersonalAdministrativoService personal;
        @Mock
        private PlantillaService plantillas;
        @Mock
        private RespaldoService respaldos;
        @Mock
        private RestTemplate restTemplate;
        @Mock
        private IndicadoresQuejasService indicadores;

        @Test
        void obtenerResumen_catalogoCaido_muestraCeroSinFallar() {
            DashboardService servicio = new DashboardService(indicadores, personal, plantillas, respaldos, restTemplate,
                    "http://catalogo");
            when(restTemplate.getForObject(eq("http://catalogo/api/catalogos/dependencias"), eq(Object[].class)))
                    .thenThrow(new ResourceAccessException("Connection refused"));
            when(personal.contarActivos()).thenReturn(5L);
            when(respaldos.ultimoRespaldoTexto()).thenReturn("Sin respaldos todavía");

            var r = servicio.obtenerResumen();

            assertEquals(0L, r.getTotalDependencias(), "Si catalogo-service no responde se muestra 0");
            assertEquals(5L, r.getTotalPersonalActivo(), "El resto del tablero sí se llena");
            assertEquals("Sin respaldos todavía", r.getUltimoRespaldo(), "Texto del último respaldo");
        }
    }
}
