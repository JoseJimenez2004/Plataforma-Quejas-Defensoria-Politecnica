-- ==========================================
-- 1. CATÁLOGOS (Tablas de Referencia)
-- ==========================================

CREATE TABLE cat_roles (
    id SERIAL PRIMARY KEY,
    nombre VARCHAR(50) UNIQUE NOT NULL,
    descripcion VARCHAR(255)
);

CREATE TABLE cat_dependencias (
    id SERIAL PRIMARY KEY,
    nombre VARCHAR(150) UNIQUE NOT NULL,
    acronimo VARCHAR(20)
);

CREATE TABLE cat_estatus_queja (
    id SERIAL PRIMARY KEY,
    nombre VARCHAR(50) UNIQUE NOT NULL,
    descripcion VARCHAR(255)
);

CREATE TABLE cat_tema (
    id SERIAL PRIMARY KEY,
    nombre VARCHAR(100) UNIQUE NOT NULL
);

CREATE TABLE cat_calidad_persona (
    id SERIAL PRIMARY KEY,
    nombre VARCHAR(50) UNIQUE NOT NULL
);

CREATE TABLE cat_tipo_documento (
    id SERIAL PRIMARY KEY,
    nombre VARCHAR(50) UNIQUE NOT NULL
);

CREATE TABLE cat_subdefensoria (
    id SERIAL PRIMARY KEY,
    nombre VARCHAR(50) UNIQUE NOT NULL
);

CREATE TABLE cat_calificacion (
    id SERIAL PRIMARY KEY,
    nombre VARCHAR(50) UNIQUE NOT NULL
);

CREATE TABLE cat_tipo_contacto (
    id SERIAL PRIMARY KEY,
    nombre VARCHAR(50) UNIQUE NOT NULL
);

-- ==========================================
-- 2. AUTENTICACIÓN Y ROLES
-- ==========================================

CREATE TABLE usuarios (
    id SERIAL PRIMARY KEY,
    username VARCHAR(50) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    email VARCHAR(100) UNIQUE NOT NULL,
    activo BOOLEAN DEFAULT TRUE,
    fecha_registro TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE usuario_rol (
    usuario_id INT NOT NULL REFERENCES usuarios(id) ON DELETE CASCADE,
    rol_id INT NOT NULL REFERENCES cat_roles(id) ON DELETE CASCADE,
    PRIMARY KEY (usuario_id, rol_id)
);

-- ==========================================
-- 3. PERFILES (Separación de Dominio)
-- ==========================================

CREATE TABLE perfiles_quejoso (
    id SERIAL PRIMARY KEY,
    usuario_id INT UNIQUE NOT NULL REFERENCES usuarios(id) ON DELETE CASCADE,
    nombre VARCHAR(50) NOT NULL,
    primer_apellido VARCHAR(50) NOT NULL,
    segundo_apellido VARCHAR(50),
    es_mayor_edad BOOLEAN NOT NULL,
    boleta_empleado VARCHAR(50),
    dependencia_id INT REFERENCES cat_dependencias(id)
);

CREATE TABLE contactos_quejoso (
    id SERIAL PRIMARY KEY,
    quejoso_id INT NOT NULL REFERENCES perfiles_quejoso(id) ON DELETE CASCADE,
    tipo_contacto_id INT NOT NULL REFERENCES cat_tipo_contacto(id),
    valor VARCHAR(150) NOT NULL
);

CREATE TABLE perfiles_personal (
    id SERIAL PRIMARY KEY,
    usuario_id INT UNIQUE NOT NULL REFERENCES usuarios(id) ON DELETE CASCADE,
    nombre VARCHAR(50) NOT NULL,
    primer_apellido VARCHAR(50) NOT NULL,
    segundo_apellido VARCHAR(50),
    numero_empleado VARCHAR(50) UNIQUE,
    dependencia_id INT REFERENCES cat_dependencias(id),
    subdefensoria_id INT REFERENCES cat_subdefensoria(id)
);

-- ==========================================
-- 4. NÚCLEO: EXPEDIENTES E IMPLICADOS
-- ==========================================

CREATE TABLE quejas (
    id SERIAL PRIMARY KEY,
    no_folio VARCHAR(50) UNIQUE,
    no_expediente VARCHAR(100) UNIQUE,
    turno INT,
    fecha_recepcion DATE NOT NULL,
    num_fojas INT DEFAULT 0,
    fecha_revision_antecedentes TIMESTAMP,
    descripcion_hechos TEXT NOT NULL,
    
    tipo_documento_id INT NOT NULL REFERENCES cat_tipo_documento(id),
    tema_id INT NOT NULL REFERENCES cat_tema(id),
    estatus_id INT NOT NULL REFERENCES cat_estatus_queja(id),
    
    usuario_captura_id INT NOT NULL REFERENCES usuarios(id),
    quejoso_id INT NOT NULL REFERENCES perfiles_quejoso(id),
    
    fecha_creacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    fecha_actualizacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE denunciados (
    id SERIAL PRIMARY KEY,
    queja_id INT NOT NULL REFERENCES quejas(id) ON DELETE CASCADE,
    nombre_completo VARCHAR(150) NOT NULL,
    calidad_id INT REFERENCES cat_calidad_persona(id),
    dependencia_id INT REFERENCES cat_dependencias(id)
);

-- ==========================================
-- 5. TRAZABILIDAD, AUDITORÍA Y ARCHIVOS
-- ==========================================

CREATE TABLE turnos_asignaciones (
    id SERIAL PRIMARY KEY,
    queja_id INT NOT NULL REFERENCES quejas(id) ON DELETE CASCADE,
    abogado_asignado_id INT NOT NULL REFERENCES usuarios(id),
    calificacion_id INT REFERENCES cat_calificacion(id),
    observaciones TEXT,
    fecha_asignacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE historial_estatus_queja (
    id SERIAL PRIMARY KEY,
    queja_id INT NOT NULL REFERENCES quejas(id) ON DELETE CASCADE,
    estatus_id INT NOT NULL REFERENCES cat_estatus_queja(id),
    usuario_modifico_id INT NOT NULL REFERENCES usuarios(id),
    comentarios TEXT,
    fecha_cambio TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE documentos (
    id SERIAL PRIMARY KEY,
    queja_id INT NOT NULL REFERENCES quejas(id) ON DELETE CASCADE,
    nombre_original VARCHAR(255) NOT NULL,
    tipo_archivo VARCHAR(50) NOT NULL,
    peso_bytes BIGINT NOT NULL,
    hash_sha256 VARCHAR(64) NOT NULL,
    url_almacenamiento TEXT NOT NULL,
    sello_digital TEXT,
    usuario_subio_id INT NOT NULL REFERENCES usuarios(id),
    fecha_subida TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE bitacora_auditoria (
    id SERIAL PRIMARY KEY,
    usuario_id INT REFERENCES usuarios(id),
    accion_realizada VARCHAR(255) NOT NULL,
    tabla_afectada VARCHAR(50),
    registro_id INT,
    ip_origen VARCHAR(45),
    detalles TEXT,
    fecha_hora TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ==========================================
-- 6. DATOS INICIALES (Opcional pero recomendado)
-- ==========================================

INSERT INTO cat_roles (nombre, descripcion) VALUES 
('ADMIN', 'Administrador total del sistema'),
('ABOGADO', 'Personal encargado de dar seguimiento a las quejas'),
('QUEJOSO', 'Usuario que interpone la denuncia');

INSERT INTO cat_estatus_queja (nombre, descripcion) VALUES 
('RECIBIDA', 'La queja ha sido registrada en el sistema'),
('EN_REVISION', 'La queja está siendo analizada por un abogado'),
('ATENDIDA', 'Se ha dado respuesta formal a la queja');