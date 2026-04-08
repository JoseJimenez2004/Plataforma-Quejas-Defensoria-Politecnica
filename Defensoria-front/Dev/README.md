
# 🚀 Configuración del Entorno: Plataforma DDP (Micro-frontends)

Este documento contiene los pasos necesarios para preparar el entorno de desarrollo y levantar la arquitectura de **Micro-frontends** basada en **Angular 18** y **Module Federation**.

## 🛠️ Requisitos Previos

### 1. Gestión de Versiones de Node (NVM)
Para evitar conflictos de versiones entre desarrolladores, utilizaremos **NVM**.


# Instalar NVM
curl -o- https://raw.githubusercontent.com/nvm-sh/nvm/v0.39.7/install.sh | 

# Recargar la configuración de la terminal
source ~/.rc


### 2. Instalación de Node.js v20 (LTS)
Angular 18 requiere como mínimo Node.js v20.19 o v22.12. Usaremos la **v20** por estabilidad:


# Instalar y activar la versión 20
nvm install 20
nvm use 20
nvm alias default 20

# Verificar versión
node -v # Debería mostrar v20.x.x


---

## 🏗️ Creación del Workspace y Aplicaciones

Sigue este orden estricto para generar la estructura del monorepo:

### Paso A: Crear el Workspace de Angular
Generamos un contenedor vacío para gestionar múltiples proyectos:

npx -p @angular/cli ng new ddp-sistema --create-application=false --style=scss
cd ddp-sistema


### Paso B: Generar Aplicaciones (Shell y MFE)
Creamos el contenedor principal y el primer micro-frontend:

# Shell (Orquestador)
ng generate application ddp-shell --routing --style=scss

# Micro-frontend: Quejoso
ng generate application mfe-quejoso --routing --style=scss


---

## 🔗 Configuración de Module Federation

Para habilitar la carga dinámica de módulos entre aplicaciones, instalamos el plugin de arquitectura:


# Configurar el Shell (Puerto 4200)
ng add @angular-architects/module-federation --project ddp-shell --port 4200

# Configurar el MFE Quejoso (Puerto 4201)
ng add @angular-architects/module-federation --project mfe-quejoso --port 4201


> [!IMPORTANT]
> Cuando la terminal pregunte: *"The package @angular-architects/module-federation will be installed and executed. Do you want to proceed?"*, escribe **Yes** (o presiona Enter).

---

## 💻 Ejecución del Proyecto en Desarrollo

Para ver el sistema funcionando, es necesario correr ambos proyectos en terminales separadas:

| Proyecto | Comando | Puerto |
| :--- | :--- | :--- |
| **DDP Shell** | `ng serve ddp-shell` | `http://localhost:4200` |
| **MFE Quejoso** | `ng serve mfe-quejoso` | `http://localhost:4201` |

---

## 📝 Notas Adicionales
* **Estilos:** Se recomienda el uso de **Tailwind CSS** configurado en la raíz del proyecto para mantener la identidad institucional (Guinda IPN).
* **Dependencias:** Si agregas una librería nueva, asegúrate de correr `npm install` y verificar que no haya conflictos en el `webpack.config.js` de los MFEs.


Sigue estos pasos:

1. Ejecutar el Micro-frontend (MFE Quejoso)
Primero levantamos el proyecto que contiene las pantallas (el "donador" de código).
En la Terminal 1:


cd ddp-sistema
ng serve mfe-quejoso
Puerto: http://localhost:4201

Nota: Si entras a esa URL, verás la aplicación del quejoso funcionando solita.

2. Ejecutar el Shell (El Orquestador)
Ahora levantamos el contenedor principal que va a "llamar" al quejoso.
En la Terminal 2:


cd ddp-sistema
ng serve ddp-shell
Puerto: http://localhost:4200

Nota: Esta es la URL principal que los usuarios usarán. El Shell cargará dinámicamente el contenido del puerto 4201 cuando navegues a la ruta correspondiente.

3. Verificación de "Module Federation"
Para confirmar que todo está conectado, abre tu navegador en http://localhost:4200.

Si configuraste las rutas como lo planeamos:

Al entrar a http://localhost:4200/quejoso, el Shell debería ir al puerto 4201, traer el componente y mostrarlo dentro de su propio marco.

Si inspeccionas la red en el navegador (F12 -> Network), verás que se descarga un archivo llamado remoteEntry.json. ¡Esa es la magia del Micro-frontend!






---


