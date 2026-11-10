package ipn.escom.defensoria.subdefensoria.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.web.server.ResponseStatusException;

import ipn.escom.defensoria.subdefensoria.entity.ExpedienteInvestigacion;
import ipn.escom.defensoria.subdefensoria.entity.OficioInformacion;
import ipn.escom.defensoria.subdefensoria.repository.ExpedienteInvestigacionRepository;
import ipn.escom.defensoria.subdefensoria.repository.OficioInformacionRepository;
import ipn.escom.defensoria.subdefensoria.repository.RespuestaExternaRepository;

/** 2026-11: cada abogado solo ve y tramita sus expedientes; el titular ve todos. */
@ExtendWith(MockitoExtension.class)
@DisplayName("AbogadoSesionService — expedientes por abogado")
class AbogadoSesionServiceTest {

    @Mock
    private JdbcTemplate jdbc;
    @Mock
    private ExpedienteInvestigacionRepository expedientes;
    @Mock
    private OficioInformacionRepository oficios;
    @Mock
    private RespuestaExternaRepository respuestas;

    @InjectMocks
    private AbogadoSesionService servicio;

    private ExpedienteInvestigacion deGerardo;

    @BeforeEach
    void setUp() {
        deGerardo = new ExpedienteInvestigacion();
        deGerardo.setId(1L);
        deGerardo.setAbogadoAsesorId(9L);
        deGerardo.setAbogadoAsesorNombre("Gerardo Ramirez");
        lenient().when(expedientes.findById(1L)).thenReturn(Optional.of(deGerardo));
        lenient().when(oficios.findById(7L))
                .thenReturn(Optional.of(OficioInformacion.builder().id(7L).expedienteId(1L).build()));
    }

    private Authentication sesion(String correo, String rol, Long idEnPersonal) {
        lenient().when(jdbc.query(anyString(), any(RowMapper.class), ArgumentMatchers.<Object>any()))
                .thenReturn(idEnPersonal == null ? List.of() : List.of(idEnPersonal));
        return new UsernamePasswordAuthenticationToken(correo, null,
                List.of(new SimpleGrantedAuthority("ROLE_" + rol)));
    }

    @Test
    void titular_veTodos() {
        Authentication titular = sesion("defensor@ipn.mx", "DEFENSOR", 5L);
        assertTrue(servicio.visibles(titular).test(deGerardo), "El Defensor titular supervisa todo");
    }

    @Test
    void abogado_soloVeLosSuyos() {
        assertTrue(servicio.visibles(sesion("gerardo@ipn.mx", "SUBDEFENSOR", 9L)).test(deGerardo));
        assertFalse(servicio.visibles(sesion("sofia@ipn.mx", "SUBDEFENSOR", 16L)).test(deGerardo));
    }

    @Test
    void abogadoSinRegistroEnPersonal_noVeNada() {
        assertFalse(servicio.visibles(sesion("x@ipn.mx", "SUBDEFENSOR", null)).test(deGerardo));
    }

    @Test
    void accionSobreOficioDeOtroAbogado_403() {
        Authentication sofia = sesion("sofia@ipn.mx", "SUBDEFENSOR", 16L);
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> servicio.exigirOficio(7L, sofia));
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
        assertTrue(ex.getReason().contains("Gerardo Ramirez"), "Dice a quién está asignado");
    }

    @Test
    void accionSobreOficioPropio_pasa() {
        Authentication gerardo = sesion("gerardo@ipn.mx", "SUBDEFENSOR", 9L);
        assertDoesNotThrow(() -> servicio.exigirOficio(7L, gerardo));
        assertDoesNotThrow(() -> servicio.exigirExpediente(1L, gerardo));
    }

    @Test
    void idInexistente_loResuelveElServicioDeLaAccion() {
        when(expedientes.findById(99L)).thenReturn(Optional.empty());
        assertDoesNotThrow(() -> servicio.exigirExpediente(99L, sesion("sofia@ipn.mx", "SUBDEFENSOR", 16L)));
    }
}
