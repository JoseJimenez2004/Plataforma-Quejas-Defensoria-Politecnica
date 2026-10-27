package ipn.escom.defensoria.revision_service.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import ipn.escom.defensoria.revision_service.entity.AcuerdoConciliacion;
import ipn.escom.defensoria.revision_service.entity.Queja;
import ipn.escom.defensoria.revision_service.model.CrearConciliacionRequest;
import ipn.escom.defensoria.revision_service.model.PrimerContactoIngresoRequest;
import ipn.escom.defensoria.revision_service.model.PrimerContactoIngresoResponse;
import ipn.escom.defensoria.revision_service.repository.AcuerdoConciliacionRepository;
import ipn.escom.defensoria.revision_service.repository.QuejaEvidenciaRepository;
import ipn.escom.defensoria.revision_service.repository.QuejaRepository;

/** Servicios de apoyo de Recepción: notificaciones, cliente de Primer Contacto y conciliación. */
@DisplayName("revision-service — servicios de apoyo")
class ServiciosApoyoRevisionTest {

    private static final String URL_NOTIFICACIONES = "http://notificaciones";
    private static final String URL_PC = "http://pc";
    private static final String FOLIO = "FOL-ABCD1234";

    @Nested
    @ExtendWith(MockitoExtension.class)
    @DisplayName("NotificacionQuejaService — RNF-R04 (el correo no tumba la operación)")
    class Notificaciones {

        @Mock
        private RestTemplate restTemplate;
        @InjectMocks
        private NotificacionQuejaService servicio;

        @BeforeEach
        void setUp() {
            ReflectionTestUtils.setField(servicio, "notificacionesServiceUrl", URL_NOTIFICACIONES);
        }

        @Test
        void enviarCorreoRechazo_publicaAsuntoYCuerpoConElFolio() {
            servicio.enviarCorreoRechazo("q@ipn.mx", "Ana", FOLIO, "Falta INE");

            ArgumentCaptor<Object> cuerpo = ArgumentCaptor.forClass(Object.class);
            verify(restTemplate).postForObject(eq(URL_NOTIFICACIONES + "/api/notificaciones/enviar"), cuerpo.capture(), eq(String.class));
            Map<?, ?> datos = (Map<?, ?>) cuerpo.getValue();
            assertTrue(datos.get("asunto").toString().endsWith(FOLIO), "El asunto lleva el folio");
            assertTrue(datos.get("cuerpo").toString().contains("Falta INE"), "El cuerpo lleva los motivos");
        }

        @Test
        void enviarCorreoRechazo_servicioCaido_noLanzaExcepcion() {
            when(restTemplate.postForObject(anyString(), any(), eq(String.class)))
                    .thenThrow(new ResourceAccessException("Connection refused"));
            assertDoesNotThrow(() -> servicio.enviarCorreoRechazo("q@ipn.mx", "Ana", FOLIO, "x"),
                    "Si falla el correo, el rechazo no se revierte");
        }

        @Test
        void notificarPersonal_enlaceNulo_mandaCadenaVacia() {
            servicio.notificarPersonal("abogado@ipn.mx", "Título", "Mensaje", null);
            ArgumentCaptor<Object> cuerpo = ArgumentCaptor.forClass(Object.class);
            verify(restTemplate).postForObject(eq(URL_NOTIFICACIONES + "/api/notificaciones/registrar"), cuerpo.capture(), eq(String.class));
            Map<?, ?> datos = (Map<?, ?>) cuerpo.getValue();
            assertEquals("", datos.get("enlace"), "Map.of no admite null: el enlace va vacío");
            assertEquals("GENERAL", datos.get("tipo"), "Tipo de aviso para personal");
        }
    }

    @Nested
    @ExtendWith(MockitoExtension.class)
    @DisplayName("PrimerContactoClientService — envío del expediente")
    class ClientePrimerContacto {

        @Mock
        private RestTemplate restTemplate;
        @Mock
        private QuejaEvidenciaRepository evidencias;
        @InjectMocks
        private PrimerContactoClientService servicio;

        private Queja queja;

        @BeforeEach
        void setUp() {
            ReflectionTestUtils.setField(servicio, "primerContactoBaseUrl", URL_PC);
            queja = new Queja();
            queja.setNumeroFolio(FOLIO);
            queja.setNombreQuejoso(" Ana ");
            queja.setApellido2Quejoso("Ruiz");
            queja.setOrigenRegistro("MANUAL");
            queja.setTipoUsuarioManual("externo");
            queja.setTipoIdentificacionQuejoso("alumno");
            queja.setFechaRecepcionFisica(LocalDate.of(2026, 10, 1));
        }

        private PrimerContactoIngresoResponse respuesta(String folio) {
            PrimerContactoIngresoResponse r = new PrimerContactoIngresoResponse();
            r.setFolio(folio);
            return r;
        }

        @Test
        void enviar_quejaManual_usaTipoManualYFechaFisica() {
            when(restTemplate.postForObject(eq(URL_PC + "/api/primer-contacto/ingesta/expedientes"), any(),
                    eq(PrimerContactoIngresoResponse.class))).thenReturn(respuesta("PC-1"));

            servicio.enviarAPrimerContacto(queja);

            ArgumentCaptor<PrimerContactoIngresoRequest> req = ArgumentCaptor.forClass(PrimerContactoIngresoRequest.class);
            verify(restTemplate).postForObject(anyString(), req.capture(), eq(PrimerContactoIngresoResponse.class));
            assertEquals(FOLIO, req.getValue().getFolioOrigen(), "Se manda el folio, no el id");
            assertEquals("Ana Ruiz", req.getValue().getQuejoso().getNombreCompleto(), "Nombre sin huecos");
            assertEquals("externo", req.getValue().getQuejoso().getTipoUsuario(), "Tipo de usuario manual");
            assertEquals("2026-10-01", req.getValue().getFechaRecepcion(), "Fecha de recepción física");
            verify(evidencias, never()).findByQuejaId(any());
        }

        @Test
        void enviar_respuestaNula_lanzaExcepcion() {
            when(restTemplate.postForObject(anyString(), any(), eq(PrimerContactoIngresoResponse.class))).thenReturn(null);
            RuntimeException ex = assertThrows(RuntimeException.class, () -> servicio.enviarAPrimerContacto(queja));
            assertEquals("Primer Contacto no devolvio informacion del expediente.", ex.getMessage(), "Sin respuesta");
        }

        @Test
        void enviar_folioEnBlanco_lanzaExcepcion() {
            when(restTemplate.postForObject(anyString(), any(), eq(PrimerContactoIngresoResponse.class)))
                    .thenReturn(respuesta(" "));
            RuntimeException ex = assertThrows(RuntimeException.class, () -> servicio.enviarAPrimerContacto(queja));
            assertEquals("Primer Contacto no devolvio un folio valido.", ex.getMessage(), "Folio inválido");
        }

        @Test
        void enviar_quejaWebSinNombre_nombreNulo() {
            Queja web = new Queja();
            web.setId(5L);
            web.setNumeroFolio(FOLIO);
            web.setOrigenRegistro("PUBLICO");
            web.setTipoIdentificacionQuejoso("empleado");
            when(evidencias.findByQuejaId(5L)).thenReturn(List.of());
            when(restTemplate.postForObject(anyString(), any(), eq(PrimerContactoIngresoResponse.class))).thenReturn(respuesta("PC-2"));

            servicio.enviarAPrimerContacto(web);

            ArgumentCaptor<PrimerContactoIngresoRequest> req = ArgumentCaptor.forClass(PrimerContactoIngresoRequest.class);
            verify(restTemplate).postForObject(anyString(), req.capture(), eq(PrimerContactoIngresoResponse.class));
            assertNull(req.getValue().getQuejoso().getNombreCompleto(), "Sin nombre se manda null");
            assertEquals("empleado", req.getValue().getQuejoso().getTipoUsuario(), "Tipo de la queja web");
        }
    }

    @Nested
    @ExtendWith(MockitoExtension.class)
    @DisplayName("ConciliacionRevisionService — CU-R07")
    class Conciliacion {

        @Mock
        private AcuerdoConciliacionRepository acuerdos;
        @Mock
        private QuejaRepository quejas;
        @Mock
        private NotificacionQuejaService notificaciones;
        @InjectMocks
        private ConciliacionRevisionService servicio;

        private CrearConciliacionRequest datos() {
            CrearConciliacionRequest d = new CrearConciliacionRequest();
            d.setNumeroFolio(FOLIO);
            d.setAsunto("Reposición de examen");
            d.setTerminos("Se aplica nuevo examen");
            return d;
        }

        @Test
        void crear_quejaExistente_dirigeAlCorreoDelQuejosoYNotifica() {
            Queja queja = new Queja();
            queja.setCorreoInstitucional("quejoso@ipn.mx");
            when(quejas.findByNumeroFolio(FOLIO)).thenReturn(Optional.of(queja));
            when(acuerdos.save(any())).thenAnswer(inv -> inv.getArgument(0));

            AcuerdoConciliacion r = servicio.crear(datos(), "staff@ipn.mx");

            assertEquals("quejoso@ipn.mx", r.getCorreoInstitucional(), "Va al correo del quejoso");
            assertEquals("staff@ipn.mx", r.getCreadoPor(), "Queda quién lo emitió");
            verify(notificaciones).registrarConciliacion("quejoso@ipn.mx", FOLIO, "Reposición de examen");
        }

        @Test
        void crear_folioInexistente_lanzaExcepcion() {
            when(quejas.findByNumeroFolio(FOLIO)).thenReturn(Optional.empty());
            assertThrows(RuntimeException.class, () -> servicio.crear(datos(), "staff@ipn.mx"), "Folio inexistente");
            verify(acuerdos, never()).save(any());
        }

        @Test
        void listar_sinFolio_regresaTodos() {
            when(acuerdos.findAllByOrderByFechaEmisionDesc()).thenReturn(List.of());
            servicio.listar("  ");
            verify(acuerdos).findAllByOrderByFechaEmisionDesc();
            verify(acuerdos, never()).findByNumeroFolioOrderByFechaEmisionDesc(anyString());
        }
    }
}
