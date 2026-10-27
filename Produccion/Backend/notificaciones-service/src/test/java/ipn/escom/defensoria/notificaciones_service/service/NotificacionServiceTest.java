package ipn.escom.defensoria.notificaciones_service.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import ipn.escom.defensoria.notificaciones_service.entity.Notificacion;
import ipn.escom.defensoria.notificaciones_service.exception.AccesoNotificacionDenegadoException;
import ipn.escom.defensoria.notificaciones_service.exception.DatosNotificacionInvalidosException;
import ipn.escom.defensoria.notificaciones_service.exception.NotificacionNoEncontradaException;
import ipn.escom.defensoria.notificaciones_service.model.RegistrarNotificacionRequest;
import ipn.escom.defensoria.notificaciones_service.repository.NotificacionRepository;

/** Pruebas unitarias de {@link NotificacionService}: centro de notificaciones (CU-Q09). */
@ExtendWith(MockitoExtension.class)
@DisplayName("NotificacionService — centro de notificaciones")
class NotificacionServiceTest {

    private static final Long ID = 4L;
    private static final String CORREO = "quejoso@ipn.mx";

    @Mock
    private NotificacionRepository repositorio;

    @InjectMocks
    private NotificacionService servicio;

    private static RegistrarNotificacionRequest datos(String correo, String titulo, String tipo) {
        RegistrarNotificacionRequest d = new RegistrarNotificacionRequest();
        d.setCorreoDestino(correo);
        d.setTitulo(titulo);
        d.setTipo(tipo);
        d.setMensaje("Mensaje");
        return d;
    }

    @Test
    void registrar_sinTipo_usaGeneral() {
        when(repositorio.save(any())).thenAnswer(inv -> inv.getArgument(0));
        Notificacion n = servicio.registrar(datos(CORREO, "Tu queja fue turnada", " "));
        assertEquals("GENERAL", n.getTipo(), "Tipo por defecto");
    }

    @Test
    void registrar_conTipo_loRespeta() {
        when(repositorio.save(any())).thenAnswer(inv -> inv.getArgument(0));
        Notificacion n = servicio.registrar(datos(CORREO, "Título", "CAMBIO_ESTATUS"));
        assertEquals("CAMBIO_ESTATUS", n.getTipo(), "Tipo recibido");
    }

    @Test
    void registrar_sinTitulo_lanzaExcepcion() {
        DatosNotificacionInvalidosException ex = assertThrows(DatosNotificacionInvalidosException.class,
                () -> servicio.registrar(datos(CORREO, null, "GENERAL")));
        assertEquals("Faltan datos obligatorios de la notificación.", ex.getMessage(), "Título obligatorio");
        verify(repositorio, never()).save(any());
    }

    @Test
    void registrar_sinCorreo_lanzaExcepcion() {
        assertThrows(DatosNotificacionInvalidosException.class, () -> servicio.registrar(datos("", "Título", null)),
                "Correo destino obligatorio");
    }

    @Test
    void marcarLeida_propiaSinImportarMayusculas_laMarca() {
        Notificacion n = new Notificacion();
        n.setCorreoDestino(CORREO);
        when(repositorio.findById(ID)).thenReturn(Optional.of(n));
        when(repositorio.save(any())).thenAnswer(inv -> inv.getArgument(0));

        assertTrue(servicio.marcarLeida(ID, "QUEJOSO@ipn.mx").isLeida(), "Queda leída");
    }

    @Test
    void marcarLeida_deOtraCuenta_lanzaExcepcion() {
        Notificacion n = new Notificacion();
        n.setCorreoDestino(CORREO);
        when(repositorio.findById(ID)).thenReturn(Optional.of(n));
        assertThrows(AccesoNotificacionDenegadoException.class, () -> servicio.marcarLeida(ID, "otro@ipn.mx"),
                "No se marca la notificación de otra persona");
        verify(repositorio, never()).save(any());
    }

    @Test
    void marcarLeida_inexistente_lanzaExcepcion() {
        when(repositorio.findById(ID)).thenReturn(Optional.empty());
        assertThrows(NotificacionNoEncontradaException.class, () -> servicio.marcarLeida(ID, CORREO), "No existe");
    }

    @Test
    void contarNoLeidas_delegaAlRepositorio() {
        when(repositorio.countByCorreoDestinoAndLeidaFalse(CORREO)).thenReturn(3L);
        assertEquals(3L, servicio.contarNoLeidas(CORREO), "Conteo del repositorio");
    }
}
