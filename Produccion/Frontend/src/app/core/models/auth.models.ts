export interface LoginRequest {
  correo: string;
  password: string;
}

export interface AuthResponse {
  token: string;
  nombre: string;
}

export interface ActivacionCuentaRequest {
  correo: string;
  numeroFolio: string;
  password: string;
  confirmarPassword: string;
}

export interface ResetPasswordRequest {
  correo: string;
  codigo: string;
  nuevaPassword: string;
}

/** Lo que guardamos en memoria/sesión tras un login exitoso. El backend no expone
 * un endpoint "/me", así que esto es lo único que sabemos del usuario actual:
 * lo que vino en la respuesta del login (token + nombre) más el correo con el que
 * inició sesión. */
export interface UsuarioActual {
  correo: string;
  nombre: string;
  token: string;
}
