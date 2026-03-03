-- Creación de Enums (como tipos o tablas según prefieras, aquí como tablas para mejor compatibilidad)
CREATE TABLE dependencias_politecnicas (
    id VARCHAR(50) PRIMARY KEY,
    nombre VARCHAR(255) NOT NULL,
    titular_nombre VARCHAR(255),
    correo_oficial VARCHAR(255)
);

CREATE TABLE usuarios (
    id VARCHAR(50) PRIMARY KEY,
    username VARCHAR(50) UNIQUE NOT NULL,
    password_hash TEXT NOT NULL,
    email VARCHAR(100) UNIQUE NOT NULL,
    nombre_completo VARCHAR(200) NOT NULL,
    rol VARCHAR(50) NOT NULL,
    activo BOOLEAN DEFAULT TRUE,
    fecha_registro TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE quejosos (
    id VARCHAR(50) PRIMARY KEY,
    usuario_id VARCHAR(50) UNIQUE REFERENCES usuarios(id),
    boleta_empleado VARCHAR(50) UNIQUE,
    unidad_academica_id VARCHAR(50) REFERENCES dependencias_politecnicas(id),
    es_mayor_edad BOOLEAN DEFAULT TRUE,
    nombre_tutor VARCHAR(255),
    contacto_tutor VARCHAR(255)
);

CREATE TABLE quejas (
    id VARCHAR(50) PRIMARY KEY,
    folio VARCHAR(50) UNIQUE,
    quejoso_id VARCHAR(50) REFERENCES quejosos(id),
    tipo VARCHAR(50) NOT NULL,
    medio VARCHAR(50) NOT NULL,
    descripcion_hechos TEXT NOT NULL,
    estatus VARCHAR(50) DEFAULT 'BORRADOR',
    tiene_antecedentes BOOLEAN DEFAULT FALSE,
    requiere_medidas BOOLEAN DEFAULT FALSE,
    fecha_creacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    fecha_ultimo_estatus TIMESTAMP
);

CREATE TABLE documentos (
    id VARCHAR(50) PRIMARY KEY,
    queja_id VARCHAR(50) REFERENCES quejas(id),
    nombre_archivo VARCHAR(255) NOT NULL,
    url_archivo TEXT NOT NULL,
    tipo_doc VARCHAR(50),
    es_original BOOLEAN DEFAULT FALSE,
    firmado BOOLEAN DEFAULT FALSE,
    sello_digital TEXT,
    subido_por_usuario_id VARCHAR(50) REFERENCES usuarios(id),
    fecha_subida TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE acciones_investigacion (
    id VARCHAR(50) PRIMARY KEY,
    queja_id VARCHAR(50) REFERENCES quejas(id),
    abogado_id VARCHAR(50) REFERENCES usuarios(id),
    dependencia_id VARCHAR(50) REFERENCES dependencias_politecnicas(id),
    descripcion_solicitud TEXT,
    fecha_solicitud DATE,
    fecha_limite_respuesta DATE,
    respondida BOOLEAN DEFAULT FALSE,
    recordatorios_enviados INTEGER DEFAULT 0
);

CREATE TABLE resoluciones (
    id VARCHAR(50) PRIMARY KEY,
    queja_id VARCHAR(50) UNIQUE REFERENCES quejas(id),
    existe_violacion BOOLEAN,
    tipo_conclusion VARCHAR(100),
    fundamento_legal TEXT,
    fecha_emision DATE,
    firma_titular_id VARCHAR(50) REFERENCES usuarios(id)
);

CREATE TABLE seguimiento_recomendaciones (
    id VARCHAR(50) PRIMARY KEY,
    resolucion_id VARCHAR(50) REFERENCES resoluciones(id),
    autoridad_acepta BOOLEAN,
    estatus_cumplimiento VARCHAR(100),
    fecha_limite_cumplimiento DATE,
    prorroga_otorgada BOOLEAN DEFAULT FALSE,
    motivo_prorroga TEXT
);

CREATE TABLE bitacora_auditoria (
    id VARCHAR(50) PRIMARY KEY,
    usuario_id VARCHAR(50) REFERENCES usuarios(id),
    accion_realizada TEXT,
    ip_origen VARCHAR(45),
    fecha_hora TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);