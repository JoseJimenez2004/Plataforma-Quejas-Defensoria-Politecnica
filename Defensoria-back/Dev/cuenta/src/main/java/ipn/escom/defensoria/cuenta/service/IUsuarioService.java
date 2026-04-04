package ipn.escom.defensoria.cuenta.service;

import ipn.escom.defensoria.cuenta.dto.LoginRequest;
import ipn.escom.defensoria.cuenta.dto.RegistroRequest;
import ipn.escom.defensoria.cuenta.dto.UsuarioResponse;

/**
 * Interfaz que define las operaciones de negocio para la gestión de cuentas.
 * Sigue el principio de Inversión de Dependencias.
 */
public interface IUsuarioService {

    /**
     * Procesa el registro de un nuevo usuario politécnico.
     * @param registroDTO Datos del formulario de registro.
     * @return UsuarioResponse con los datos públicos del usuario creado.
     */
    UsuarioResponse registrar(RegistroRequest registroDTO);

    /**
     * Valida las credenciales para el inicio de sesión.
     * @param loginDTO Correo y contraseña.
     * @return UsuarioResponse si las credenciales son válidas.
     */
    UsuarioResponse login(LoginRequest loginDTO);

    /**
     * Gestiona la solicitud de recuperación de contraseña.
     * @param correo Correo electrónico del usuario.
     */
    void recuperarPassword(String correo);

}