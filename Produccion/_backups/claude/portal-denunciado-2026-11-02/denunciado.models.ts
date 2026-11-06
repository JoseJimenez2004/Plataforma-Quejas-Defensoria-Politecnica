/** Datos que captura el denunciado (POST /api/denunciado/respuestas, multipart). */
export interface RespuestaDenunciadoRequest {
  folioQueja: string;
  nombre: string;
  apellido1: string;
  apellido2: string;
  unidadProcedenciaClave: string;
  tipoIdentificacion: 'ALUMNO' | 'EMPLEADO';
  numeroIdentificacion: string;
  descripcionHechos: string;
  avisoPrivacidadVersion: string;
}

/** Acuse que devuelve denunciado-service al registrar. */
export interface RespuestaRegistrada {
  folioRespuesta: string;
  folioQueja: string;
  nombreCompleto: string;
  fechaRegistro: string;
  credenciales: number;
  evidencias: number;
}
