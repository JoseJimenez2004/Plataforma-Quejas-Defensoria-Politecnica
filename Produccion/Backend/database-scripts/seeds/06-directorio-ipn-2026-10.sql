-- =====================================================================================
-- Directorio IPN → catálogo de dependencias (2026-10-28)
-- Fuente: https://www.ipn.mx/directorio-funcionarios.html (Directorio de Servidores Públicos
-- de Mando y Dirección, consultado el 28/10/2026).
--
-- 1) Da de alta las unidades académicas que existen en el IPN y faltaban en el catálogo
--    (ESIQIE, ENBA, CECyT 20, UPIEM, UPIIH, UPIIP, UPIIT, UPIIAP y los 3 CIITA).
-- 2) Actualiza titular (grado + nombre) y correo de contacto de cada dependencia ligada.
--    Si el puesto aparece vacante en el directorio, solo se actualiza el correo.
--
-- Es idempotente: se puede correr varias veces. Correr en la base del catálogo
-- (la misma donde vive la tabla "dependencias"):
--    psql -h <host> -U <usuario> -d <base> -f 06-directorio-ipn-2026-10.sql
-- =====================================================================================

BEGIN;

-- 1) Unidades faltantes ---------------------------------------------------------------
INSERT INTO dependencias (clave, clave_padre, nombre, abreviatura, tipo, categoria, nivel, activo, notas, creado_en) VALUES
  ('CECYT20', NULL, 'CECyT No. 20 "Natalia Serdán Alatriste"', 'CECyT 20', 'Unidad Académica', 'Unidades Académicas de Nivel Medio Superior Interdisciplinarias', 1, TRUE, 'Alta desde el Directorio IPN (2026-10-28)', now()),
  ('ESIQIE', NULL, 'Escuela Superior de Ingeniería Química e Industrias Extractivas', 'ESIQIE', 'Unidad Académica', 'Unidades Académicas de Nivel Superior, Rama de Ingeniería y Ciencias Físico Matemáticas', 1, TRUE, 'Alta desde el Directorio IPN (2026-10-28)', now()),
  ('ENBA', NULL, 'Escuela Nacional de Biblioteconomía y Archivonomía', 'ENBA', 'Unidad Académica', 'Unidades Académicas de Nivel Superior, Rama de Ciencias Sociales y Administrativas', 1, TRUE, 'Alta desde el Directorio IPN (2026-10-28)', now()),
  ('UPIEM', NULL, 'Unidad Profesional Interdisciplinaria de Energía y Movilidad', 'UPIEM', 'Unidad Académica', 'Unidades Académicas de Nivel Superior Interdisciplinarias', 1, TRUE, 'Alta desde el Directorio IPN (2026-10-28)', now()),
  ('UPIIH', NULL, 'Unidad Profesional Interdisciplinaria de Ingeniería, Campus Hidalgo', 'UPIIH', 'Unidad Académica', 'Unidades Académicas de Nivel Superior Interdisciplinarias', 1, TRUE, 'Alta desde el Directorio IPN (2026-10-28)', now()),
  ('UPIIP', NULL, 'Unidad Profesional Interdisciplinaria de Ingeniería, Campus Palenque', 'UPIIP', 'Unidad Académica', 'Unidades Académicas de Nivel Superior Interdisciplinarias', 1, TRUE, 'Alta desde el Directorio IPN (2026-10-28)', now()),
  ('UPIIT', NULL, 'Unidad Profesional Interdisciplinaria en Ingeniería, Campus Tlaxcala', 'UPIIT', 'Unidad Académica', 'Unidades Académicas de Nivel Superior Interdisciplinarias', 1, TRUE, 'Alta desde el Directorio IPN (2026-10-28)', now()),
  ('UPIIAP', NULL, 'Unidad Profesional Interdisciplinaria de Ingeniería, Campus "Alejo Peralta" Puebla', 'UPIIAP', 'Unidad Académica', 'Unidades Académicas de Nivel Superior Interdisciplinarias', 1, TRUE, 'Alta desde el Directorio IPN (2026-10-28)', now()),
  ('CIITA-JUA', NULL, 'Centro de Innovación e Integración de Tecnologías Avanzadas, Unidad Ciudad Juárez', 'CIITA Cd. Juárez', 'Centro', 'Centros de Innovación e Integración de Tecnologías Avanzadas', 1, TRUE, 'Alta desde el Directorio IPN (2026-10-28)', now()),
  ('CIITA-PAP', NULL, 'Centro de Innovación e Integración de Tecnologías Avanzadas, Unidad Papantla', 'CIITA Papantla', 'Centro', 'Centros de Innovación e Integración de Tecnologías Avanzadas', 1, TRUE, 'Alta desde el Directorio IPN (2026-10-28)', now()),
  ('CIITA-PUE', NULL, 'Centro de Innovación e Integración de Tecnologías Avanzadas, Unidad Puebla', 'CIITA Puebla', 'Centro', 'Centros de Innovación e Integración de Tecnologías Avanzadas', 1, TRUE, 'Alta desde el Directorio IPN (2026-10-28)', now())
ON CONFLICT (clave) DO NOTHING;

-- 2) Titulares y correos -------------------------------------------------------------
UPDATE dependencias SET nombre_titular = 'Dr. Arturo Reyes Sandoval', correo_contacto = 'cgestion@ipn.mx' WHERE clave = 'DG';  -- Dirección General
UPDATE dependencias SET nombre_titular = 'Ing. Julio César Barker Vasco', correo_contacto = 'jbarker@ipn.mx' WHERE clave = 'DG.3';  -- Coordinación de Enlace y Atención Institucional / Coordinación de Rela
UPDATE dependencias SET nombre_titular = 'Mtro. Eric Toledo Contreras', correo_contacto = 'etoledoc@ipn.mx' WHERE clave = 'DG.2';  -- Coordinación Administrativa
UPDATE dependencias SET nombre_titular = 'Mtra. María Eulalia Mildred Castro Hernández', correo_contacto = 'cps@ipn.mx' WHERE clave = 'CPS';  -- Coordinación Politécnica para la Sustentabilidad (CPS)
UPDATE dependencias SET nombre_titular = 'M. en C. Ismael Jaidar Monter', correo_contacto = 'secretariageneral@ipn.mx' WHERE clave = 'SG';  -- Secretaría General
UPDATE dependencias SET correo_contacto = 'sgdprevencion@ipn.mx' WHERE clave = 'SG.1.1';  -- Coordinación de Prevención y Seguridad / División de Prevención
UPDATE dependencias SET nombre_titular = 'Mtro. Dax Hervest García Vidal', correo_contacto = 'dgarciav@ipn.mx' WHERE clave = 'SG.1.2';  -- Coordinación de Prevención y Seguridad / División de Apoyo y Seguridad
UPDATE dependencias SET nombre_titular = 'Mtra. Elizabeth Cabrera Chávez', correo_contacto = 'upgpg@ipn.mx' WHERE clave = 'UPGPG';  -- Unidad Politécnica de Gestión con Perspectiva de Género
UPDATE dependencias SET nombre_titular = 'Mtra. Luisa Alicia Sánchez Vidal', correo_contacto = 'defensoria@ipn.mx' WHERE clave = 'DDP';  -- Defensoría de los Derechos Politécnicos
UPDATE dependencias SET nombre_titular = 'Mtro. Marx Yazalde Ortiz Correa', correo_contacto = 'abogadogeneral@ipn.mx' WHERE clave = 'OAG';  -- Oficina del Abogado General
UPDATE dependencias SET nombre_titular = 'Lic. Jorge Eladio Solís Delgado', correo_contacto = 'jsolis@ipn.mx' WHERE clave = 'OAG.1';  -- Dirección de Asuntos Jurídicos
UPDATE dependencias SET nombre_titular = 'M. en E.N.A. María Isabel Rojas Ruíz', correo_contacto = 'seaca@ipn.mx' WHERE clave = 'SA';  -- Secretaría Académica
UPDATE dependencias SET nombre_titular = 'Q.B.P. David Juárez Mora', correo_contacto = 'djuarez@ipn.mx' WHERE clave = 'SA.1';  -- Dirección de Educación Media Superior
UPDATE dependencias SET nombre_titular = 'M. en A. Efrén Dorado Morales', correo_contacto = 'edorado@ipn.mx' WHERE clave = 'SA.1.2';  -- Dirección de Educación Media Superior / División de Innovación Académi
UPDATE dependencias SET nombre_titular = 'Lic. María de Jesús Arellano Mejía', correo_contacto = 'marellanom@ipn.mx' WHERE clave = 'SA.1.3';  -- Dirección de Educación Media Superior / División de Gestión y Calidad 
UPDATE dependencias SET nombre_titular = 'M. en A. Marco Antonio Sorcini Muñoz', correo_contacto = 'des@ipn.mx' WHERE clave = 'SA.2';  -- Dirección de Educación Superior
UPDATE dependencias SET nombre_titular = 'M. en A. Cinthya Susana Reyes Santos', correo_contacto = 'creyess@ipn.mx' WHERE clave = 'SA.2.1';  -- Dirección de Educación Superior / División de Procesos Formativos
UPDATE dependencias SET nombre_titular = 'Lic. Anabel Gómez Santamaría', correo_contacto = 'angomezs@ipn.mx' WHERE clave = 'SA.2.2';  -- Dirección de Educación Superior / División de Innovación Académica
UPDATE dependencias SET nombre_titular = 'Lic. Silvia Blanco Vázquez', correo_contacto = 'sblancov@ipn.mx' WHERE clave = 'SA.3';  -- Dirección de Formación en Lenguas Extranjeras
UPDATE dependencias SET nombre_titular = 'Dra. Martha Leticia Vázquez González', correo_contacto = 'sip@ipn.mx' WHERE clave = 'SIP';  -- Secretaría de Investigación y Posgrado
UPDATE dependencias SET nombre_titular = 'Dr. Samuel Pérez Rodríguez', correo_contacto = 'direccionposgrado@ipn.mx' WHERE clave = 'SIP.1';  -- Dirección de Posgrado
UPDATE dependencias SET nombre_titular = 'Dr. José Nicolás Fernández García', correo_contacto = 'dir_investigacion@ipn.mx' WHERE clave = 'SIP.3';  -- Dirección de Investigación
UPDATE dependencias SET nombre_titular = 'Dr. Luis Cuauhtémoc Gil Cisneros', correo_contacto = 'dopi@ipn.mx' WHERE clave = 'SIP.3.1';  -- Dirección de Investigación / División de Operación y Promoción a la In
UPDATE dependencias SET nombre_titular = 'Dr. Andrés Concepción Brindis', correo_contacto = 'dai@ipn.mx' WHERE clave = 'SIP.3.2';  -- Dirección de Investigación / División de Apoyo a la Investigación
UPDATE dependencias SET nombre_titular = 'Lic. Beatriz Rangel Romo', correo_contacto = 'dess@ipn.mx' WHERE clave = 'SEIS.1';  -- Dirección de Egresados y Servicio Social
UPDATE dependencias SET nombre_titular = 'C.P. Juan Antonio Martínez López', correo_contacto = 'jamartinezl@ipn.mx' WHERE clave = 'SEIS.1.2';  -- Dirección de Egresados y Servicio Social / División de Egresados
UPDATE dependencias SET nombre_titular = 'M. en C. Luis Alberto Méndez Cruz', correo_contacto = 'lumendez@ipn.mx' WHERE clave = 'SEIS.1.1';  -- Dirección de Egresados y Servicio Social / División de Servicio Social
UPDATE dependencias SET nombre_titular = 'Dr. Marco Antonio Sosa Palacios', correo_contacto = 'correspondenciasse@ipn.mx' WHERE clave = 'SSE';  -- Secretaría de Servicios Educativos
UPDATE dependencias SET correo_contacto = 'oficiosdae@ipn.mx' WHERE clave = 'SSE.1';  -- Dirección de Administración Escolar (DAE)
UPDATE dependencias SET correo_contacto = 'jracevedo@ipn.mx' WHERE clave = 'SSE.1.1';  -- Dirección de Administración Escolar (DAE) / División de Admisión y Con
UPDATE dependencias SET correo_contacto = 'jpazaran@ipn.mx' WHERE clave = 'SSE.1.2';  -- Dirección de Administración Escolar (DAE) / División de Registro y Cer
UPDATE dependencias SET nombre_titular = 'Dr. Jesús Antonio Hernández Pérez', correo_contacto = 'dap@ipn.mx' WHERE clave = 'SSE.2';  -- Dirección de Apoyos a Estudiantes
UPDATE dependencias SET nombre_titular = 'Mtra. Gabriela Concepción Arteaga Olivares', correo_contacto = 'divpas@ipn.mx' WHERE clave = 'SSE.2.1';  -- Dirección de Apoyos a Estudiantes / División de Prestaciones y Atenció
UPDATE dependencias SET nombre_titular = 'Mtra. Liliana Rebeca Martínez Sánchez', correo_contacto = 'dase@ipn.mx' WHERE clave = 'SSE.2.2';  -- Dirección de Apoyos a Estudiantes / División de Apoyo y Servicios a Es
UPDATE dependencias SET nombre_titular = 'Ing. José Antonio Cruz González', correo_contacto = 'ddc@ipn.mx' WHERE clave = 'SSE.4';  -- Dirección de Difusión Cultural
UPDATE dependencias SET nombre_titular = 'Mtra. Quetzalli Rosas Montes', correo_contacto = 'qrosas@ipn.mx' WHERE clave = 'SSE.4.1';  -- Dirección de Difusión Cultural / División de Fomento y Promoción Cultu
UPDATE dependencias SET nombre_titular = 'Lic. Laura Vega Rangel', correo_contacto = 'lavega@ipn.mx' WHERE clave = 'SSE.4.2';  -- Dirección de Difusión Cultural / División de Difusión
UPDATE dependencias SET nombre_titular = 'L.A.I. Carlos Aarón Fuentes Ambriz', correo_contacto = 'cfuentesf@ipn.mx' WHERE clave = 'SSE.5';  -- Dirección de Actividades Deportivas
UPDATE dependencias SET nombre_titular = 'Lic. José Guzmán Vera', correo_contacto = 'jguzmanve@ipn.mx' WHERE clave = 'SSE.5.1';  -- Dirección de Actividades Deportivas / División de Fomento Deportivo
UPDATE dependencias SET nombre_titular = 'Ing. Miguel Ángel Cortizo Rodríguez', correo_contacto = 'dbp@ipn.mx' WHERE clave = 'SSE.3';  -- Dirección de Bibliotecas y Publicaciones
UPDATE dependencias SET nombre_titular = 'Lic. Norma Jacqueline Delgadillo Velásquez', correo_contacto = 'ndelgadillo@ipn.mx' WHERE clave = 'SSE.3.1';  -- Dirección de Bibliotecas y Publicaciones / División de Servicios Bibli
UPDATE dependencias SET nombre_titular = 'Mtra. Ana María Arrona González', correo_contacto = 'sriaadm@ipn.mx' WHERE clave = 'SAD';  -- Secretaría de Administración
UPDATE dependencias SET nombre_titular = 'Mtro. Isaac Godínez Martínez', correo_contacto = 'igodinezm@ipn.mx' WHERE clave = 'SAD.1';  -- Dirección de Capital Humano
UPDATE dependencias SET nombre_titular = 'Mtra. Erika Ramírez Jiménez', correo_contacto = 'eramirezji@ipn.mx' WHERE clave = 'SAD.1.1';  -- Dirección de Capital Humano / División de Admisión y Desarrollo del Pe
UPDATE dependencias SET nombre_titular = 'Lic. Claudia Verónica López Martínez', correo_contacto = 'cvlopezm@ipn.mx' WHERE clave = 'SAD.1.2';  -- Dirección de Capital Humano / División de Trayectorias del Personal
UPDATE dependencias SET nombre_titular = 'Lic. Roberto Muñoz Membrilla', correo_contacto = 'rmunozm@ipn.mx' WHERE clave = 'SAD.1.3';  -- Dirección de Capital Humano / División de Sistemas de Información
UPDATE dependencias SET nombre_titular = 'C.P. Agustín Maldonado Córdoba', correo_contacto = 'agmaldonadoc@ipn.mx' WHERE clave = 'SAD.1.4';  -- Dirección de Capital Humano / División de Remuneraciones
UPDATE dependencias SET nombre_titular = 'C.P. Claudia Juárez García', correo_contacto = 'cjuarezg@ipn.mx' WHERE clave = 'SAD.2';  -- Dirección de Recursos Financieros
UPDATE dependencias SET nombre_titular = 'Lic. Luis Manuel González López', correo_contacto = 'lgonzalezlo@ipn.mx' WHERE clave = 'SAD.2.1';  -- Dirección de Recursos Financieros / División de Control y Ejercicio de
UPDATE dependencias SET nombre_titular = 'C.P. José Alonso García Salazar', correo_contacto = 'jagarcias@ipn.mx' WHERE clave = 'SAD.3';  -- Dirección de Recursos Materiales e Infraestructura
UPDATE dependencias SET nombre_titular = 'Lic. Gerardo Alfredo Icaza Palacio', correo_contacto = 'gicazap@ipn.mx' WHERE clave = 'SAD.3.1';  -- Dirección de Recursos Materiales e Infraestructura / División de Adqui
UPDATE dependencias SET nombre_titular = 'Lic. Gerardo Castro Vivas', correo_contacto = 'gcastrov@ipn.mx' WHERE clave = 'SAD.3.2';  -- Dirección de Recursos Materiales e Infraestructura / División de Infra
UPDATE dependencias SET nombre_titular = 'Lic. Mariana Cuadros Monroy', correo_contacto = 'macuadros@ipn.mx' WHERE clave = 'CCDI';  -- Coordinación de Centros de Desarrollo Infantil
UPDATE dependencias SET nombre_titular = 'Lic. Mayela Paulina Franco Vera', correo_contacto = 'cendi_asc@ipn.mx' WHERE clave = 'CCDI.2';  -- Coordinación de Centros de Desarrollo Infantil / Centro de Desarrollo 
UPDATE dependencias SET nombre_titular = 'Lic. Luz de Lourdes Balcázar Damián', correo_contacto = 'cendi_cbb@ipn.mx' WHERE clave = 'CCDI.4';  -- Coordinación de Centros de Desarrollo Infantil / Centro de Desarrollo 
UPDATE dependencias SET nombre_titular = 'Profa. Rocío Gómez Malagón', correo_contacto = 'cendi_eva@ipn.mx' WHERE clave = 'CCDI.3';  -- Coordinación de Centros de Desarrollo Infantil / Centro de Desarrollo 
UPDATE dependencias SET nombre_titular = 'Lic. Rocío Gabriela Rangel Onofre', correo_contacto = 'cendi_lpb@ipn.mx' WHERE clave = 'CCDI.1';  -- Coordinación de Centros de Desarrollo Infantil / Centro de Desarrollo 
UPDATE dependencias SET nombre_titular = 'Lic. Mariana Margarita Hernández Rojas', correo_contacto = 'cendi_mse@ipn.mx' WHERE clave = 'CCDI.5';  -- Coordinación de Centros de Desarrollo Infantil / Centro de Desarrollo 
UPDATE dependencias SET nombre_titular = 'Lic. Gabriela Emilia García Guerra', correo_contacto = 'gegarciag@ipn.mx' WHERE clave = 'CCDI.6';  -- Coordinación de Centros de Desarrollo Infantil / Centro de Desarrollo 
UPDATE dependencias SET nombre_titular = 'Ing. Andrés Falcón García', correo_contacto = 'cenac@ipn.mx' WHERE clave = 'CNC';  -- Coordinación General del Centro Nacional de Cálculo (CENAC)
UPDATE dependencias SET nombre_titular = 'Ing. Fernando Mario Varela López', correo_contacto = 'dcyc@ipn.mx' WHERE clave = 'CGSI.1';  -- Dirección de Cómputo y Comunicaciones
UPDATE dependencias SET nombre_titular = 'Ing. José Jorge Pérez Mora', correo_contacto = 'jjperezmo@ipn.mx' WHERE clave = 'CGSI.1.1';  -- Dirección de Cómputo y Comunicaciones / División de Cómputo
UPDATE dependencias SET nombre_titular = 'Ing. Enrique Segura Ruíz', correo_contacto = 'esegura@ipn.mx' WHERE clave = 'CGSI.1.2';  -- Dirección de Cómputo y Comunicaciones / División de Telecomunicaciones
UPDATE dependencias SET nombre_titular = 'Mtra. Pamela Sue Vergara Alemán', correo_contacto = 'pvergaraa@ipn.mx' WHERE clave = 'CGSI.1.3';  -- Dirección de Cómputo y Comunicaciones / División de Servicios Técnicos
UPDATE dependencias SET nombre_titular = 'Mtro. Jorge Luis Martínez Agama', correo_contacto = 'jlmartineza@ipn.mx' WHERE clave = 'CNC.1';  -- Dirección de Sistemas Informáticos / División de Ingeniería de Softwar
UPDATE dependencias SET nombre_titular = 'Ing. Laura Gabriela Aguilar Medina', correo_contacto = 'laguilarm@ipn.mx' WHERE clave = 'CNC.2';  -- Dirección de Sistemas Informáticos / División de Desarrollo de Sistema
UPDATE dependencias SET nombre_titular = 'M. en C. Modesto Cárdenas García', correo_contacto = 'presidenciadeldecanato@ipn.mx' WHERE clave = 'PD';  -- Presidencia del Decanato
UPDATE dependencias SET nombre_titular = 'Lic. Mireya María Zuleta Monsiváis', correo_contacto = 'toic@ipn.mx' WHERE clave = 'OIC';  -- Órgano Interno de Control
UPDATE dependencias SET nombre_titular = 'Mtro. Cornelio Fernando Miranda López', correo_contacto = 'cecyt1@ipn.mx' WHERE clave = 'CECYT1';  -- Centro de Estudios Científicos y Tecnológicos (CECyT 1) "Gonzalo Vázqu
UPDATE dependencias SET nombre_titular = 'M. en E. Víctor Gabriel Islas Nieva', correo_contacto = 'cecyt2@ipn.mx' WHERE clave = 'CECYT2';  -- Centro de Estudios Científicos y Tecnológicos (CECyT 2) "Miguel Bernar
UPDATE dependencias SET nombre_titular = 'Dr. José Camilo Martínez Ochoa', correo_contacto = 'cecyt3@ipn.mx' WHERE clave = 'CECYT3';  -- Centro de Estudios Científicos y Tecnológicos (CECyT 3) "Estanislao Ra
UPDATE dependencias SET nombre_titular = 'Ing. Gabriel Cortés Martínez', correo_contacto = 'cecyt4direccion@ipn.mx' WHERE clave = 'CECYT4';  -- Centro de Estudios Científicos y Tecnológicos (CECyT 4) "Lázaro Cárden
UPDATE dependencias SET nombre_titular = 'Ing. Francisco Javier Méndez Escamilla', correo_contacto = 'cecyt7@ipn.mx' WHERE clave = 'CECYT7';  -- Centro de Estudios Científicos y Tecnológicos (CECyT 7) "Cuauhtémoc"
UPDATE dependencias SET nombre_titular = 'Ing. Ricardo Cervantes Reyes', correo_contacto = 'direccioncecyt8@ipn.mx' WHERE clave = 'CECYT8';  -- Centro de Estudios Científicos y Tecnológicos (CECyT 8) "Narciso Basso
UPDATE dependencias SET nombre_titular = 'Dra. Dorina Ortega Sánchez', correo_contacto = 'cecyt9direccion@ipn.mx' WHERE clave = 'CECYT9';  -- Centro de Estudios Científicos y Tecnológicos (CECyT 9) "Juan de Dios 
UPDATE dependencias SET nombre_titular = 'Mtro. Emilio Martínez Delfín', correo_contacto = 'cecyt10@ipn.mx' WHERE clave = 'CECYT10';  -- Centro de Estudios Científicos y Tecnológicos (CECyT 10) "Carlos Valle
UPDATE dependencias SET nombre_titular = 'M. en E. Fernando Uribe Montesinos', correo_contacto = 'direccioncecyt11@ipn.mx' WHERE clave = 'CECYT11';  -- Centro de Estudios Científicos y Tecnológicos (CECyT 11) "Wilfrido Mas
UPDATE dependencias SET nombre_titular = 'Ing. Ramón Gómez Ramírez', correo_contacto = 'rgomezr@ipn.mx' WHERE clave = 'CET1';  -- Centro de Estudios Tecnológicos (CET 1) "Walter Cross Buchanan"
UPDATE dependencias SET nombre_titular = 'Mtro. Haner Rubén Rubio Ibarra', correo_contacto = 'direccioncecyt6@ipn.mx' WHERE clave = 'CECYT6';  -- Centro de Estudios Científicos y Tecnológicos (CECyT 6) "Miguel Othón 
UPDATE dependencias SET nombre_titular = 'Ing. Norma Lorena Baéz Contreras', correo_contacto = 'direccioncecyt15@ipn.mx' WHERE clave = 'CECYT15';  -- Centro de Estudios Científicos y Tecnológicos (CECyT 15) "Diódoro Antú
UPDATE dependencias SET nombre_titular = 'M. en D. Edith Gabriela Martínez Moreno', correo_contacto = 'cecyt5@ipn.mx' WHERE clave = 'CECYT5';  -- Centro de Estudios Científicos y Tecnológicos (CECyT 5) "Benito Juárez
UPDATE dependencias SET nombre_titular = 'Dr. Mario Alberto Pérez Garduño', correo_contacto = 'cecyt12@ipn.mx' WHERE clave = 'CECYT12';  -- Centro de Estudios Científicos y Tecnológicos (CECyT 12) "José María M
UPDATE dependencias SET nombre_titular = 'M. en E. Miguel Ángel Cruz Rodríguez', correo_contacto = 'cecyt13direccion@ipn.mx' WHERE clave = 'CECYT13';  -- Centro de Estudios Científicos y Tecnológicos (CECyT 13) "Ricardo Flor
UPDATE dependencias SET nombre_titular = 'M. en C. María Griselda Valencia Ramírez', correo_contacto = 'direccion_cecyt14@ipn.mx' WHERE clave = 'CECYT14';  -- Centro de Estudios Científicos y Tecnológicos (CECyT 14) "Luis Enrique
UPDATE dependencias SET nombre_titular = 'M. en C. Alejandro Escamilla Navarro', correo_contacto = 'cecyt16@ipn.mx' WHERE clave = 'CECYT16';  -- Centro de Estudios Científicos y Tecnológicos (CECyT 16) "Hidalgo"
UPDATE dependencias SET nombre_titular = 'Mtro. Paul Octavio Gómez Castro', correo_contacto = 'cecytleon@ipn.mx' WHERE clave = 'CECYT17';  -- Centro de Estudios Científicos y Tecnológicos (CECyT 17) "León, Guanaj
UPDATE dependencias SET nombre_titular = 'M. en Edu. Julio Reynoso Suárez', correo_contacto = 'cecyt18@ipn.mx' WHERE clave = 'CECYT18';  -- Centro de Estudios Científicos y Tecnológicos (CECyT 18) "Zacatecas"
UPDATE dependencias SET nombre_titular = 'M. en E. Enrique Macías Maya', correo_contacto = 'cecyt19@ipn.mx' WHERE clave = 'CECYT19';  -- Centro de Estudios Científicos y Tecnológicos (CECyT 19) "Leona Vicari
UPDATE dependencias SET nombre_titular = 'Ing. Carlos Alberto Paredes Treviño', correo_contacto = 'cparedest@ipn.mx' WHERE clave = 'CECYT20';  -- Centro de Estudios Científicos y Tecnológicos (CECyT 20) "Natalia Serd
UPDATE dependencias SET nombre_titular = 'Dr. Mauro Alberto Enciso Aguilar', correo_contacto = 'contactoesimez@ipn.mx' WHERE clave = 'ESIME-ZAC';  -- Escuela Superior de Ingeniería Mecánica y Eléctrica (ESIME) Unidad Zac
UPDATE dependencias SET nombre_titular = 'M. en E. Dalia Ruíz Domínguez', correo_contacto = 'dirección_esimecu@ipn.mx' WHERE clave = 'ESIME-CUL';  -- Escuela Superior de Ingeniería Mecánica y Eléctrica (ESIME) Unidad Cul
UPDATE dependencias SET nombre_titular = 'Mtro. Hilario Bautista Morales', correo_contacto = 'direccion.esimeazc@ipn.mx' WHERE clave = 'ESIME-AZC';  -- Escuela Superior de Ingeniería Mecánica y Eléctrica (ESIME) Unidad Azc
UPDATE dependencias SET nombre_titular = 'M. en C. Armando Oropeza Osornio', correo_contacto = 'aoropeza@ipn.mx' WHERE clave = 'ESIME-TIC';  -- Escuela Superior de Ingeniería Mecánica y Eléctrica (ESIME) Unidad Tic
UPDATE dependencias SET nombre_titular = 'Dr. Juan José Villalpando Cázares', correo_contacto = 'diresiaic@ipn.mx' WHERE clave = 'ESIA-ZAC';  -- Escuela Superior de Ingeniería y Arquitectura (ESIA) Unidad Zacatenco
UPDATE dependencias SET nombre_titular = 'Dr. José Ignacio Hernández Vázquez', correo_contacto = 'direc_esiatec@ipn.mx' WHERE clave = 'ESIA-TEC';  -- Escuela Superior de Ingeniería y Arquitectura (ESIA) Unidad Tecamachal
UPDATE dependencias SET nombre_titular = 'Mtro. Mario Alberto González Torres', correo_contacto = 'direccion_esiatic@ipn.mx' WHERE clave = 'ESIA-TIC';  -- Escuela Superior de Ingeniería y Arquitectura (ESIA) Unidad Ticomán
UPDATE dependencias SET nombre_titular = 'Dr. Efraín Robledo Godínez', correo_contacto = 'esit@ipn.mx' WHERE clave = 'ESIT';  -- Escuela Superior de Ingeniería Textil (ESIT)
UPDATE dependencias SET nombre_titular = 'M. en C. Sergio Hernández Garrido', correo_contacto = 'sehernandezg@ipn.mx' WHERE clave = 'ESIQIE';  -- Escuela Superior de Ingeniería Química e Industrias Extractivas (ESIQI
UPDATE dependencias SET nombre_titular = 'Dr. Miguel Neri Rosas', correo_contacto = 'direccionesfm@ipn.mx' WHERE clave = 'ESFM';  -- Escuela Superior de Física y Matemáticas (ESFM)
UPDATE dependencias SET nombre_titular = 'M. en C. Iván Giovanny Mosso García', correo_contacto = 'direccion_escom@ipn.mx' WHERE clave = 'ESCOM';  -- Escuela Superior de Cómputo (ESCOM)
UPDATE dependencias SET nombre_titular = 'Dr. Mario Alberto Rodríguez Casas', correo_contacto = 'direccion.encb@ipn.mx' WHERE clave = 'ENCB';  -- Escuela Nacional de Ciencias Biológicas (ENCB)
UPDATE dependencias SET nombre_titular = 'Dr. Celso Pérez Rostro', correo_contacto = 'esm.direccion@ipn.mx' WHERE clave = 'ESM';  -- Escuela Superior de Medicina (ESM)
UPDATE dependencias SET nombre_titular = 'Mtro. Marco Antonio Martínez Silva', correo_contacto = 'enmh@ipn.mx' WHERE clave = 'ENMH';  -- Escuela Nacional de Medicina y Homeopatía (ENMyH)
UPDATE dependencias SET nombre_titular = 'M. en C. Ma Atanacia Silvia Cárdenas Oscoy', correo_contacto = 'eseo@ipn.mx' WHERE clave = 'ESEO';  -- Escuela Superior de Enfermería y Obstetricia (ESEO)
UPDATE dependencias SET nombre_titular = 'M.C.P. Carlos Rivera García', correo_contacto = 'crivera@ipn.mx' WHERE clave = 'CICS-MA';  -- Centro Interdisciplinario de Ciencias de la Salud, Unidad Milpa Alta (
UPDATE dependencias SET nombre_titular = 'Dr. Juan Daniel Rodríguez Choreño', correo_contacto = 'jrodriguezch@ipn.mx' WHERE clave = 'CICS-ST';  -- Centro Interdisciplinario de Ciencias de la Salud, Unidad Santo Tomás 
UPDATE dependencias SET nombre_titular = 'Dra. Dulce María Monroy Becerril', correo_contacto = 'escasto@ipn.mx' WHERE clave = 'ESCA-ST';  -- Escuela Superior de Comercio y Administración Unidad Santo Tomás (ESCA
UPDATE dependencias SET nombre_titular = 'Dra. Ivett Guillén Morales', correo_contacto = 'direcciontep@ipn.mx' WHERE clave = 'ESCA-TEP';  -- Escuela Superior de Comercio y Administración Unidad Tepepan (ESCA- Te
UPDATE dependencias SET nombre_titular = 'Mtro. Salvador Monroy Saldívar', correo_contacto = 'direccion.ese@ipn.mx' WHERE clave = 'ESE';  -- Escuela Superior de Economía (ESE)
UPDATE dependencias SET nombre_titular = 'Dra. Marissa Alonso Marbán', correo_contacto = 'direccionest@ipn.mx' WHERE clave = 'EST';  -- Escuela Superior de Turismo (EST)
UPDATE dependencias SET nombre_titular = 'Dr. Luis Francisco Rivero Zambrano', correo_contacto = 'enba_oficial@ipn.mx' WHERE clave = 'ENBA';  -- Escuela Nacional de Biblioteconomía y Archivonomía (ENBA)
UPDATE dependencias SET nombre_titular = 'Dra. Karina Cruz Pacheco', correo_contacto = 'direccionupibi@ipn.mx' WHERE clave = 'UPIBI';  -- Unidad Profesional Interdisciplinaria de Biotecnología (UPIBI)
UPDATE dependencias SET nombre_titular = 'M. en C. Andrea Alejandra Rendón Peña', correo_contacto = 'arendonp@ipn.mx' WHERE clave = 'UPIEM';  -- Unidad Profesional Interdisciplinaria de Energía y Movilidad (UPIEM)
UPDATE dependencias SET nombre_titular = 'M. en A. Antonio Romero Hernández', correo_contacto = 'direccionupiicsa@ipn.mx' WHERE clave = 'UPIICSA';  -- Unidad Profesional Interdisciplinaria de Ingeniería y Ciencias Sociale
UPDATE dependencias SET nombre_titular = 'Ing. Israel Vázquez Cianca', correo_contacto = 'dir.upiita@ipn.mx' WHERE clave = 'UPIITA';  -- Unidad Profesional Interdisciplinaria en Ingeniería y Tecnologías Avan
UPDATE dependencias SET nombre_titular = 'Dra. Rosa Hernández Soto', correo_contacto = 'rohernandezs@ipn.mx' WHERE clave = 'UPIG';  -- Unidad Profesional Interdisciplinaria de Ingeniería Campus Guanajuato 
UPDATE dependencias SET nombre_titular = 'Ing. Francisco Javier Escamilla López', correo_contacto = 'direccionupiih@ipn.mx' WHERE clave = 'UPIIH';  -- Unidad Profesional Interdisciplinaria de Ingeniería Campus Hidalgo (UP
UPDATE dependencias SET nombre_titular = 'Mtro. David Vázquez Álvarez', correo_contacto = 'dvazqueza@ipn.mx' WHERE clave = 'UPIIP';  -- Unidad Profesional Interdisciplinaria de Ingeniería Campus Palenque (U
UPDATE dependencias SET nombre_titular = 'Ing. Enrique Lima Morales', correo_contacto = 'upiit@ipn.mx' WHERE clave = 'UPIIT';  -- Unidad Profesional Interdisciplinaria en Ingeniería Campus Tlaxcala (U
UPDATE dependencias SET nombre_titular = 'Mtro. Roberto Oswaldo Cruz Leija', correo_contacto = 'rocruzl@ipn.mx' WHERE clave = 'UPIZ';  -- Unidad Profesional Interdisciplinaria de Ingeniería Campus Zacatecas (
UPDATE dependencias SET nombre_titular = 'Ing. Carlos Alberto Paredes Treviño', correo_contacto = 'upiiap@ipn.mx' WHERE clave = 'UPIIAP';  -- Unidad Profesional Interdisciplinaria de Ingeniería Campus 'Alejo Pera
UPDATE dependencias SET nombre_titular = 'Dra. Amanda Alejandra Oliva Hernández', correo_contacto = 'aoliva@ipn.mx' WHERE clave = 'CBG';  -- Centro de Biotecnología Genómica (CBG)
UPDATE dependencias SET nombre_titular = 'Dr. Diego Alfredo Padilla Pérez', correo_contacto = 'dir_cda@ipn.mx' WHERE clave = 'CDA';  -- Centro de Desarrollo Aeroespacial (CDA)
UPDATE dependencias SET nombre_titular = 'Dra. Kalina Bermúdez Torres', correo_contacto = 'kbermudes@ipn.mx' WHERE clave = 'CEPROBI';  -- Centro de Desarrollo de Productos Bióticos (CEPROBI)
UPDATE dependencias SET nombre_titular = 'Dr. Mario Aldape Pérez', correo_contacto = 'maldape@ipn.mx' WHERE clave = 'CIDETEC';  -- Centro de Innovación y Desarrollo Tecnológico en Cómputo (CIDETEC)
UPDATE dependencias SET nombre_titular = 'Dr. Ricardo Rafael Ambriz Rojas', correo_contacto = 'ciitec@ipn.mx' WHERE clave = 'CIITEC';  -- Centro de Investigación e Innovación Tecnológica (CIITEC)
UPDATE dependencias SET nombre_titular = 'Dra. Diana Verónica Cortés Espinosa', correo_contacto = 'dcortes@ipn.mx' WHERE clave = 'CIBA';  -- Centro de Investigación en Biotecnología Aplicada, IPN-Tlaxcala (CIBA)
UPDATE dependencias SET nombre_titular = 'Dr. Rogelio Ortega Izaguirre', correo_contacto = 'dir.cicata.alt@ipn.mx' WHERE clave = 'CICATA-ALT';  -- Centro de Investigación en Ciencia Aplicada y Tecnología Avanzada (CIC
UPDATE dependencias SET correo_contacto = 'cicata@ipn.mx' WHERE clave = 'CICATA-LEG';  -- Centro de Investigación en Ciencia Aplicada y Tecnología Avanzada (CIC
UPDATE dependencias SET nombre_titular = 'Dr. Paul Mondragón Terán', correo_contacto = 'cicatamor@ipn.mx' WHERE clave = 'CICATA-MOR';  -- Centro de Investigación en Ciencia Aplicada y Tecnología Avanzada (CIC
UPDATE dependencias SET nombre_titular = 'Dr. Juan Bautista Hurtado Ramos', correo_contacto = 'dircicataqro@ipn.mx' WHERE clave = 'CICATA-QRO';  -- Centro de Investigación en Ciencia Aplicada y Tecnología Avanzada (CIC
UPDATE dependencias SET nombre_titular = 'Dr. Juan Humberto Sossa Azuela', correo_contacto = 'hsossa@cic.ipn.mx' WHERE clave = 'CIC';  -- Centro de Investigación en Computación (CIC)
UPDATE dependencias SET nombre_titular = 'Dr. Julio César Rolón Garrido', correo_contacto = 'direccion.citedi@ipn.mx' WHERE clave = 'CITEDI';  -- Centro de Investigación y Desarrollo de Tecnología Digital (CITEDI)
UPDATE dependencias SET nombre_titular = 'Dra. María del Pilar Monserrat Pérez Hernández', correo_contacto = 'ciecas@ipn.mx' WHERE clave = 'CIECAS';  -- Centro de Investigaciones Económicas, Administrativas y Sociales (CIEC
UPDATE dependencias SET nombre_titular = 'Dr. Marcial Arellano Martínez', correo_contacto = 'direccioncicimar@ipn.mx' WHERE clave = 'CICIMAR';  -- Centro Interdisciplinario de Ciencias Marinas (CICIMAR)
UPDATE dependencias SET nombre_titular = 'Dr. Eduardo Sánchez Ortíz', correo_contacto = 'esanchezo@ipn.mx' WHERE clave = 'CIIDIR-DGO';  -- Centro Interdisciplinario de Investigación para el Desarrollo Integral
UPDATE dependencias SET nombre_titular = 'Dra. María Valentina Angoa Pérez', correo_contacto = 'vangoa@ipn.mx' WHERE clave = 'CIIDIR-MICH';  -- Centro Interdisciplinario de Investigación para el Desarrollo Integral
UPDATE dependencias SET nombre_titular = 'Dr. Baldomero Hortencio Zárate Nicolás', correo_contacto = 'ciidiroaxaca@ipn.mx' WHERE clave = 'CIIDIR-OAX';  -- Centro Interdisciplinario de Investigación para el Desarrollo Integral
UPDATE dependencias SET nombre_titular = 'Dr. Héctor Abelardo González Ocampo', correo_contacto = 'hgonzalezo@ipn.mx' WHERE clave = 'CIIDIR-SIN';  -- Centro Interdisciplinario de Investigación para el Desarrollo Integral
UPDATE dependencias SET nombre_titular = 'Dr. Pedro Francisco Rodríguez Espinosa', correo_contacto = 'direccion.ciiemad@ipn.mx' WHERE clave = 'CIIEMAD';  -- Centro Interdisciplinario de Investigaciones y Estudios sobre Medio Am
UPDATE dependencias SET nombre_titular = 'Dr. Raúl Hernández Altamirano', correo_contacto = 'direccioncmpl@ipn.mx' WHERE clave = 'CMPMASL';  -- Centro Mexicano para la Producción más Limpia (CMP+L)
UPDATE dependencias SET nombre_titular = 'Dr. Óscar Camacho Nieto', correo_contacto = 'cnmn@ipn.mx' WHERE clave = 'CNMN';  -- Centro de Nanociencias y Micro y Nanotecnologías (CNMN)
UPDATE dependencias SET nombre_titular = 'Mtro. Fidel Cienfuegos Valencia', correo_contacto = 'fcienfuegosv@ipn.mx' WHERE clave = 'CEC-CAJ';  -- Centro de Vinculación y Desarrollo Regional (CVDR), Unidad Cajeme Sono
UPDATE dependencias SET nombre_titular = 'Mtro. Guillermo Aid Jiménez Falcón', correo_contacto = 'cvdrcampeche@ipn.mx' WHERE clave = 'CEC-CAM';  -- Centro de Vinculación y Desarrollo Regional (CVDR), Unidad Campeche
UPDATE dependencias SET nombre_titular = 'L.C. Eurídice Arlae Iturbe Rogel', correo_contacto = 'eiturber@ipn.mx' WHERE clave = 'CEC-CAN';  -- Centro de Vinculación y Desarrollo Regional (CVDR), Unidad Cancún
UPDATE dependencias SET nombre_titular = 'Mtro. Carlos Alberto Camarillo Castro', correo_contacto = 'ccamarilloc@ipn.mx' WHERE clave = 'CEC-CUL';  -- Centro de Vinculación y Desarrollo Regional (CVDR), Unidad Culiacán
UPDATE dependencias SET nombre_titular = 'Mtra. Ma. del Carmen Aguilar Avalos', correo_contacto = 'maguilara@ipn.mx' WHERE clave = 'CEC-DGO';  -- Centro de Vinculación y Desarrollo Regional (CVDR), Unidad Durango
UPDATE dependencias SET nombre_titular = 'Mtro. Jesús Irán Grageda Arellano', correo_contacto = 'jgragedaa@ipn.mx' WHERE clave = 'CEC-MOC';  -- Centro de Vinculación y Desarrollo Regional (CVDR), Unidad Los Mochis
UPDATE dependencias SET nombre_titular = 'Dra. Adela Leonor Rendón Ramírez', correo_contacto = 'arendonr@ipn.mx' WHERE clave = 'CEC-MAZ';  -- Centro de Vinculación y Desarrollo Regional (CVDR), Unidad Mazatlán
UPDATE dependencias SET nombre_titular = 'Dra. Elizabeth Vázquez Bernal', correo_contacto = 'evazquezb@ipn.mx' WHERE clave = 'CEC-MORA';  -- Centro de Vinculación y Desarrollo Regional (CVDR), Unidad Morelia
UPDATE dependencias SET nombre_titular = 'Lic. Jusey Martínez Carrasco', correo_contacto = 'jumartinezc@ipn.mx' WHERE clave = 'CEC-OAX';  -- Centro de Vinculación y Desarrollo Regional (CVDR), Unidad Oaxaca
UPDATE dependencias SET nombre_titular = 'Lic. Isidro Papalotzi Cruz', correo_contacto = 'ipapalotzi@ipn.mx' WHERE clave = 'CEC-TAM';  -- Centro de Vinculación y Desarrollo Regional (CVDR), Unidad Tampico
UPDATE dependencias SET nombre_titular = 'Ing. Carlos Dante Cavallero', correo_contacto = 'cvdrtijuana@ipn.mx' WHERE clave = 'CEC-TIJ';  -- Centro de Vinculación y Desarrollo Regional (CVDR), Unidad Tijuana
UPDATE dependencias SET nombre_titular = 'Lic. Víctor Oswaldo Rodríguez Arreola', correo_contacto = 'cvdrtlaxcala@ipn.mx' WHERE clave = 'CEC-TLAX';  -- Centro de Vinculación y Desarrollo Regional (CVDR), Unidad Tlaxcala
UPDATE dependencias SET nombre_titular = 'M. en T.E. María del Pilar Negrete Barajas', correo_contacto = 'mnegreteb@ipn.mx' WHERE clave = 'CENLEX-ZAC';  -- Centro de Lenguas Extranjeras (CENLEX), Unidad Zacatenco
UPDATE dependencias SET nombre_titular = 'Mtro. Agustín Domínguez Flores', correo_contacto = 'adominguezf@ipn.mx' WHERE clave = 'CENLEX-ST';  -- Centro de Lenguas Extranjeras (CENLEX), Unidad Santo Tomás
UPDATE dependencias SET nombre_titular = 'C.P. Elizabeth Burrola Meléndez', correo_contacto = 'ciitajuarez@ipn.mx' WHERE clave = 'CIITA-JUA';  -- Centro de Innovación e Integración de Tecnologías Avanzadas, (CIITA) U
UPDATE dependencias SET nombre_titular = 'Dr. Francisco Javier Picaso Castañeda', correo_contacto = 'fjpicasoc@ipn.mx' WHERE clave = 'CIITA-PAP';  -- Centro de Innovación e Integración de Tecnologías Avanzadas, (CIITA) U
UPDATE dependencias SET nombre_titular = 'Mtro. Tomás Garduño Pérez', correo_contacto = 'tgardunop@ipn.mx' WHERE clave = 'CIITA-PUE';  -- Centro de Innovación e Integración de Tecnologías Avanzadas, (CIITA) U

-- Áreas que cambiaron de nombre en la estructura vigente del IPN (se liga al área sucesora)
UPDATE dependencias SET nombre_titular = 'Mtro. Raymundo Raúl Castro Arrona', correo_contacto = 'consultivo@ipn.mx' WHERE clave = 'OAG.2';  -- Dirección de Legislación, Consulta y Transparencia
UPDATE dependencias SET nombre_titular = 'M. en C. Emmanuel González Rogel', correo_contacto = 'dfie@ipn.mx' WHERE clave = 'CGFIE';  -- Dirección de Formación e Innovación Educativa
UPDATE dependencias SET nombre_titular = 'Mtro. Jonathan Rosales Juan', correo_contacto = 'direcciondev@ipn.mx' WHERE clave = 'POLIVIRTUAL';  -- Dirección de Educación Virtual
UPDATE dependencias SET nombre_titular = 'Dr. Fernando Montes Carrillo', correo_contacto = 'ddicyt@ipn.mx' WHERE clave = 'CDCYT';  -- Dirección de Difusión de Ciencia y Tecnología
UPDATE dependencias SET nombre_titular = 'Mtra. Yessica Gasca Castillo', correo_contacto = 'siis@ipn.mx' WHERE clave = 'SEIS';  -- Secretaría de Innovación e Integración Social
UPDATE dependencias SET nombre_titular = 'Mtro. Juan José Ramírez Gutiérrez', correo_contacto = 'direccion.dri@ipn.mx' WHERE clave = 'SEIS.4';  -- Dirección de Relaciones Internacionales
UPDATE dependencias SET nombre_titular = 'Lic. Claudia Marina Espitia Dager', correo_contacto = 'dsett@ipn.mx' WHERE clave = 'POLIEMPRESARIAL';  -- Dirección de Servicios Empresariales y Transferencia Tecnológica
UPDATE dependencias SET nombre_titular = 'Lic. Alfredo Santiago Ortiz', correo_contacto = 'diet@ipn.mx' WHERE clave = 'POLIINCUBA';  -- Dirección de Incubación de Empresas Tecnológicas
UPDATE dependencias SET nombre_titular = 'Lic. Oscar Ignacio Pérez Iturbide', correo_contacto = 'tecnopoli@ipn.mx' WHERE clave = 'TECHNOPOLI';  -- Dirección de Prospectiva e Inteligencia Tecnológica, Tecnópoli
UPDATE dependencias SET nombre_titular = 'Mtra. Irene Sánchez Vargas', correo_contacto = 'dppr@ipn.mx' WHERE clave = 'SGE.3';  -- Dirección de Programación y Presupuesto
UPDATE dependencias SET nombre_titular = 'Mtro. José Javier Aguado Domínguez', correo_contacto = 'jaguado@ipn.mx' WHERE clave = 'SGE.3.1';  -- Dirección de Programación y Presupuesto / División de Programación
UPDATE dependencias SET nombre_titular = 'Mtro. Arturo Alejandro Ledesma Santiago', correo_contacto = 'aledesmas@ipn.mx' WHERE clave = 'SGE.3.2';  -- Dirección de Programación y Presupuesto / División de Presupuesto
UPDATE dependencias SET nombre_titular = 'Mtro. Pedro Alberto Martínez Sustaita', correo_contacto = 'dserviciosgrales@ipn.mx' WHERE clave = 'SAD.3.3';  -- Dirección de Servicios Generales
UPDATE dependencias SET nombre_titular = 'Mtro. Orlando David Parada Vicente', correo_contacto = 'oparada@ipn.mx' WHERE clave = 'SGE';  -- Coordinación General de Planeación e Información Institucional
UPDATE dependencias SET nombre_titular = 'Mtro. Alejandro Romero Pérez', correo_contacto = 'aromerope@ipn.mx' WHERE clave = 'SGE.1';  -- Dirección de Planeación y Organización
UPDATE dependencias SET nombre_titular = 'Lic. Lilia Selene Belmares Alegría', correo_contacto = 'lbelmaresa@ipn.mx' WHERE clave = 'SGE.1.1';  -- Dirección de Planeación y Organización / División de Planeación y Pros
UPDATE dependencias SET nombre_titular = 'Lic. Angélica Díaz Quintanar', correo_contacto = 'aquintan@ipn.mx' WHERE clave = 'SGE.2';  -- Dirección de Información Institucional
UPDATE dependencias SET nombre_titular = 'Mtro. Héctor Valencia Figueroa', correo_contacto = 'hvalenciaf@ipn.mx' WHERE clave = 'SGE.2.1';  -- Dirección de Información Institucional / División de Evaluación
UPDATE dependencias SET nombre_titular = 'Mtro. Rubén Santos Pérez', correo_contacto = 'rsantosp@ipn.mx' WHERE clave = 'SGE.2.2';  -- Dirección de Información Institucional / División de Información y Est
UPDATE dependencias SET nombre_titular = 'Lic. Esteban Raya Frutos', correo_contacto = 'erayaf@ipn.mx' WHERE clave = 'CGSI';  -- Dirección de Sistemas Informáticos
UPDATE dependencias SET nombre_titular = 'Lic. Edgar Román González', correo_contacto = 'eromango@ipn.mx' WHERE clave = 'OIC.4';  -- Área de Responsabilidades

COMMIT;
