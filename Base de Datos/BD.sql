// Use DBML to define your database structure
// Docs: https://dbml.dbdiagram.io/docs

// --- ENUMS PARA REGLAS DE NEGOCIO ---

Enum RolActor {
  QUEJOSO
  AREA_SECRETARIAL
  PRIMER_CONTACTO
  SUBDEFENSORIA_ABOGADO
  RESOLUCIONES
  TITULAR_DDP
  SEGUIMIENTO
  ADMIN_SISTEMA
  AUTORIDAD_POLITECNICA
}

Enum TipoSolicitud {
  ORIENTACION
  QUEJA
}

Enum MedioRecepcion {
  PERSONAL
  CORREO_ELECTRONICO
  REDES_SOCIALES
  SITIO_WEB
}

Enum EstatusQueja {
  BORRADOR
  RECIBIDA
  EN_REVISION
  INADMITIDA
  ADMITIDA
  EN_INVESTIGACION
  EN_RESOLUCION
  CONCILIADA
  CONCLUIDA
  EN_SEGUIMIENTO
}

Enum TipoDocumento {
  EVIDENCIA
  ACUERDO_ADMISION
  ACUERDO_INCOMPETENCIA
  SOLICITUD_MEDIDAS
  OFICIO_INVESTIGACION
  RESOLUCION
  RECOMENDACION
  ACTA_CONCILIACION
}

// --- TABLAS DE USUARIOS Y ACTORES ---

Table Usuario {
  id varchar [pk]
  username varchar [unique, not null]
  password_hash varchar [not null]
  email varchar [unique, not null]
  nombre_completo varchar [not null]
  rol RolActor [not null]
  activo boolean [default: true]
  fecha_registro timestamp [default: `now()`]
}

Table Quejoso {
  id varchar [pk]
  usuarioId varchar [unique]
  boleta_empleado varchar [unique]
  unidad_academica_id varchar
  es_mayor_edad boolean [default: true]
  nombre_tutor varchar 
  contacto_tutor varchar
}

Table DependenciaPolitecnica {
  id varchar [pk]
  nombre varchar [not null]
  titular_nombre varchar
  correo_oficial varchar
}

// --- TABLAS DE GESTIÓN DE EXPEDIENTES ---

Table Queja {
  id varchar [pk]
  folio varchar [unique] 
  quejosoId varchar
  tipo TipoSolicitud [not null]
  medio MedioRecepcion [not null]
  descripcion_hechos text [not null]
  estatus EstatusQueja [default: 'BORRADOR']
  tiene_antecedentes boolean [default: false]
  requiere_medidas boolean [default: false]
  fecha_creacion timestamp [default: `now()`]
  fecha_ultimo_estatus timestamp
}

Table Documento {
  id varchar [pk]
  quejaId varchar
  nombre_archivo varchar [not null]
  url_archivo varchar [not null]
  tipo_doc TipoDocumento
  es_original boolean [default: false]
  firmado boolean [default: false]
  sello_digital text
  subido_por_usuarioId varchar
  fecha_subida timestamp [default: `now()`]
}

// --- TABLAS DE INVESTIGACIÓN Y RESOLUCIÓN ---

Table AccionInvestigacion {
  id varchar [pk]
  quejaId varchar
  abogado_id varchar 
  dependencia_id varchar
  descripcion_solicitud text
  fecha_solicitud date
  fecha_limite_respuesta date 
  respondida boolean [default: false]
  recordatorios_enviados integer [default: 0]
}

Table Resolucion {
  id varchar [pk]
  quejaId varchar [unique]
  existe_violacion boolean
  tipo_conclusion varchar 
  fundamento_legal text
  fecha_emision date
  firma_titular_id varchar
}

Table SeguimientoRecomendacion {
  id varchar [pk]
  resolucionId varchar
  autoridad_acepta boolean
  estatus_cumplimiento varchar 
  fecha_limite_cumplimiento date
  prorroga_otorgada boolean [default: false]
  motivo_prorroga text
}

// --- AUDITORÍA ---

Table BitacoraAuditoria {
  id varchar [pk]
  usuarioId varchar
  accion_realizada text
  ip_origen varchar
  fecha_hora timestamp [default: `now()`]
}

// --- RELACIONES (REFERENCIAS TOP-LEVEL) ---

Ref: Quejoso.usuarioId > Usuario.id
Ref: Queja.quejosoId > Quejoso.id
Ref: Documento.quejaId > Queja.id
Ref: Documento.subido_por_usuarioId > Usuario.id
Ref: AccionInvestigacion.quejaId > Queja.id
Ref: AccionInvestigacion.abogado_id > Usuario.id
Ref: AccionInvestigacion.dependencia_id > DependenciaPolitecnica.id
Ref: Resolucion.quejaId > Queja.id
Ref: Resolucion.firma_titular_id > Usuario.id
Ref: SeguimientoRecomendacion.resolucionId > Resolucion.id
Ref: BitacoraAuditoria.usuarioId > Usuario.id
Ref: Quejoso.unidad_academica_id > DependenciaPolitecnica.id