# Iconos de la aplicación

**Librería:** `lucide-angular` 1.0.0
**Catálogo:** `Frontend/src/app/shared/iconos/iconos.ts`
**Registro:** `Frontend/src/app/app.config.ts`
**Fecha:** 2026-09-09

---

## Por qué una librería y no archivos descargados

La petición original era descargar los iconos y ponerlos en una carpeta. Se optó por la
librería, y estas son las razones concretas:

| | Archivos PNG/SVG en una carpeta | Librería de iconos |
|---|---|---|
| Color | Un archivo por color. El ojo gris deshabilitado y el ojo guinda al pasar el mouse serían dos archivos | Heredan el color del texto con `currentColor`: se cambia en el CSS |
| Nitidez | Un PNG se ve borroso en pantallas retina salvo que subas 2x y 3x | Vectorial: nítido en cualquier tamaño y pantalla |
| Peticiones | Una petición HTTP por icono | Van dentro del bundle, cero peticiones |
| Consistencia | Cada quien descarga el suyo y acaban de estilos distintos | Todos del mismo set, mismo grosor de trazo |

El único costo real es la dependencia en `package.json` y el peso en el bundle — y eso se
controla con el registro selectivo que se explica abajo.

---

## Cómo está montado

### 1. El catálogo: `shared/iconos/iconos.ts`

Un solo archivo decide qué iconos existen y cómo se llaman. Los nombres están en español
porque el resto del código lo está:

```ts
export const ICONOS = {
  ojo: Eye,
  lapiz: Pencil,
  bote: Trash2,
  lupa: Search,
  // …
} as const;
```

### 2. El registro: `app.config.ts`

```ts
importProvidersFrom(LucideAngularModule.pick(ICONOS_REGISTRADOS)),
```

`pick()` es la pieza clave del peso. Lucide trae **más de 1500 iconos**; al registrar solo los
del catálogo, el compilador descarta el resto y nunca llegan al bundle. Si cada componente
importara los suyos por su cuenta sería fácil arrastrar de más sin darse cuenta — y el
presupuesto del bundle ya se rebasó una vez con este proyecto.

### 3. Usarlo en una pantalla

Tres pasos:

```ts
// 1. importar el módulo en el componente standalone
import { LucideAngularModule } from 'lucide-angular';
import { ICONOS } from '../../shared/iconos/iconos';

@Component({
  imports: [CommonModule, LucideAngularModule],
})
export class MiPantalla {
  // 2. exponer el catálogo a la plantilla
  readonly ICONOS = ICONOS;
}
```

```html
<!-- 3. usarlo -->
<lucide-icon [img]="ICONOS.ojo"></lucide-icon>
```

El tamaño y el color se controlan desde el CSS, como cualquier otro elemento:

```scss
.boton-ver {
  color: #6b5a60;
  lucide-icon { width: 17px; height: 17px; }

  &:hover { color: #6c1d45; }   // el icono cambia de color solo
}
```

---

## Reutilizarlo: el ojo del login

El caso que motivó todo esto. Para el "mostrar/ocultar contraseña" de `portal-login`:

1. En `iconos.ts`, importa `EyeOff` y agrégalo a ambos objetos:

```ts
import { Eye, EyeOff, ... } from 'lucide-angular';

export const ICONOS = {
  ojo: Eye,
  ojoTachado: EyeOff,
  // …
};

export const ICONOS_REGISTRADOS = { Eye, EyeOff, ... };
```

2. En el componente del login, importa `LucideAngularModule` y expón `ICONOS`.

3. En la plantilla:

```html
<button type="button" (click)="verPassword = !verPassword">
  <lucide-icon [img]="verPassword ? ICONOS.ojoTachado : ICONOS.ojo"></lucide-icon>
</button>
```

No hay que tocar `app.config.ts`: ya registra todo lo que declare `ICONOS_REGISTRADOS`.

---

## Agregar un icono nuevo

1. Búscalo en <https://lucide.dev> — el buscador está en inglés (`trash`, `eye`, `pencil`).
2. Anota el nombre en PascalCase (`Trash2`, `FileText`).
3. Agrégalo a los dos objetos de `iconos.ts`: a `ICONOS` con su nombre en español y a
   `ICONOS_REGISTRADOS` con el original.
4. Ya está disponible en cualquier pantalla que importe `LucideAngularModule`.

**Solo hay que tocar `iconos.ts`.** Si te encuentras editando `app.config.ts` para agregar un
icono, algo se salió del patrón.

---

## Los que ya están

| Nombre | Icono de Lucide | Dónde se usa |
|---|---|---|
| `ojo` | Eye | Ver queja |
| `lapiz` | Pencil | Editar queja |
| `bote` | Trash2 | Retirar queja, quitar evidencia |
| `lupa` | Search | Buscador de Mis Quejas |
| `mas` | Plus | Agregar evidencia |
| `equis` | X | Limpiar búsqueda, cerrar |
| `descargar` | Download | Descargar evidencia |
| `documento` | FileText | Archivos que no son imagen |
| `imagen` | Image | Evidencias de imagen |
| `palomita` | Check | Paso completado de la línea del tiempo |
| `advertencia` | AlertTriangle | Diálogos de confirmación |
| `reloj` | Clock | Paso actual de la línea del tiempo |
| `enviar` | Send | Acciones de envío |
| `prohibido` | Ban | Queja rechazada o retirada |
| `clip` | Paperclip | Adjuntos |
| `info` | Info | Avisos informativos |

---

## Si algún día hay que quitar la librería

El catálogo la aísla: `iconos.ts` es el único archivo que importa de `lucide-angular`, y las
plantillas solo conocen `ICONOS.ojo`. Cambiar de librería —o pasar a SVG propios— significa
reescribir ese archivo y nada más; las pantallas no se enteran.
