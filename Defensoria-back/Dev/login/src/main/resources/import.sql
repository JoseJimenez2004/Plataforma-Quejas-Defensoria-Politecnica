-- Insert de roles
INSERT INTO roles (nombre_rol) VALUES ('ADMIN') ON CONFLICT (nombre_rol) DO NOTHING;
INSERT INTO roles (nombre_rol) VALUES ('QUEJOSO') ON CONFLICT (nombre_rol) DO NOTHING;
INSERT INTO roles (nombre_rol) VALUES ('DEFENSOR') ON CONFLICT (nombre_rol) DO NOTHING;
INSERT INTO roles (nombre_rol) VALUES ('RECEPCIONISTA') ON CONFLICT (nombre_rol) DO NOTHING;
INSERT INTO roles (nombre_rol) VALUES ('PRIMER_CONTACTO') ON CONFLICT (nombre_rol) DO NOTHING;
INSERT INTO roles (nombre_rol) VALUES ('SUBDEFENSOR') ON CONFLICT (nombre_rol) DO NOTHING;
INSERT INTO roles (nombre_rol) VALUES ('TITULAR_DDP') ON CONFLICT (nombre_rol) DO NOTHING;