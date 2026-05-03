export interface LoginDTO {
  correo: string;
  password: string;
}

export interface ActivacionCuentaDTO {
  correo: string;
  numeroFolio: string;
  password: string;
  confirmarPassword: string;
}

export interface ResetPasswordDTO {
  correo: string;
  codigo: string;
  nuevaPassword: string;
}

export interface UsuarioPerfilDTO {
  nombre: string;
  correoInstitucional: string;
  boleta: string;
  unidadAcademica: string;
  correoPersonal: string;
  telefonoCelular: string;
  nombreTutor: string;
  parentescoTutor: string;
  telefonoTutor: string;
}

export interface AuthRespuestaDTO {
  token: string;
  perfil: UsuarioPerfilDTO;
}
