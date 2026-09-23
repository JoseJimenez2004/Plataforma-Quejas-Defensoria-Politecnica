# Autocompletado del catálogo de dependencias

**Componente:** `Frontend/src/app/shared/autocompletar-dependencia/`
**Reemplaza a:** el `<select>` de 209 opciones del campo "Lugar donde sucedieron los hechos"
**Fecha:** 2026-09-09
**Toca backend o base de datos:** no

---

## El problema

El catálogo del IPN tiene **209 dependencias**: 45 unidades académicas, 54 divisiones, 49
centros, 25 direcciones y el resto secretarías, coordinaciones y órganos. Un `<select>` nativo
con 209 `<option>` obliga a deslizar una lista larguísima para encontrar la propia escuela, y en
celular es peor todavía: el selector nativo se vuelve una rueda interminable.

El usuario, además, casi nunca piensa en el nombre oficial. Piensa **"soy de ESCOM"**, no
"Escuela Superior de Cómputo".

## La solución en una frase

Una caja de texto que, conforme escribes, muestra las dependencias que coinciden — por siglas,
por nombre completo o por pedazos de ambos — y te deja elegir una con el mouse o el teclado.

Lo que el formulario guarda sigue siendo la **clave** (`ESCOM`), exactamente igual que antes.
Ni el backend ni la base de datos se enteran del cambio.

---

## Decisión de fondo: buscar en el cliente, no en el servidor

El filtrado ocurre **en el navegador**, sobre el catálogo que ya se descargó.

Podría haberse hecho al revés: un endpoint `GET /api/catalogos/dependencias?q=escom` que
buscara en Postgres y devolviera resultados por cada tecla. No se hizo, y la razón importa:

| | Buscar en el cliente | Buscar en el servidor |
|---|---|---|
| Tamaño del catálogo | 209 registros, ~40 KB | pensado para decenas de miles |
| Respuesta por tecla | inmediata, sin red | una petición HTTP cada vez |
| Si la red va lenta | sigue funcionando | la lista se congela |
| Carga en el backend | ninguna | una consulta por pulsación |
| Complejidad | filtrar un arreglo | endpoint + índice + *debounce* + cancelar peticiones viejas |

Con 209 registros no hay ninguna ganancia en mover esto al servidor. El catálogo completo ya
se pedía de todos modos al abrir la pantalla, porque el `<select>` lo necesitaba entero.

**Cuándo habría que cambiarlo:** si el catálogo creciera a varios miles de renglones, o si
tuviera que mostrar datos que cambian minuto a minuto. En ese caso lo que cambia es de dónde
salen las sugerencias — se agrega el endpoint de búsqueda y un *debounce* de unos 250 ms aquí —
y todo lo demás del componente (teclado, accesibilidad, resaltado, selección) se queda igual.

---

## Cómo funciona por dentro

### 1. Normalizar el texto

Antes de comparar nada, tanto lo que escribe el usuario como los datos del catálogo pasan por
la misma limpieza: minúsculas y sin acentos. Así `cómputo`, `computo` y `CÓMPUTO` son la misma
cosa, que es justo lo que espera alguien tecleando rápido en un celular.

```ts
private normalizar(texto: string): string {
  return [...texto]
    .map((caracter) => caracter.normalize('NFD')[0])
    .join('')
    .toLowerCase();
}
```

**Por qué carácter por carácter y no `texto.normalize('NFD').replace(...)`, que es lo habitual:**
descomponer la cadena completa la *alarga* — una `ó` se convierte en dos caracteres, `o` más
una tilde combinante. Si el texto normalizado tiene distinta longitud que el original, las
posiciones dejan de corresponderse, y entonces ya no se puede saber qué tramo del nombre real
hay que resaltar. Normalizando letra por letra y quedándose con la primera, cada carácter sigue
ocupando exactamente una posición.

### 2. Construir el índice una sola vez

Cuando llega el catálogo, cada dependencia se guarda ya normalizada, junto con sus palabras
sueltas:

```ts
{
  dep,                                  // la dependencia original, intacta
  nombreNorm: 'escuela superior de computo',
  claveNorm:  'escom',
  abrevNorm:  'escom',
  palabras:   ['escuela', 'superior', 'de', 'computo'],
}
```

Esto se hace **una vez**, no en cada tecla. Normalizar 209 nombres en cada pulsación sería
trabajo repetido para nada.

### 3. Puntuar cada coincidencia

Aquí está el corazón del asunto. No basta con preguntar "¿el nombre contiene lo que escribí?",
porque entonces escribir `ESCOM` pondría al mismo nivel a cualquier dependencia con "com" en el
nombre — Comercio, Comunicaciones, Cómputo — y la que el usuario quiere quedaría enterrada.

Por eso cada coincidencia recibe un puntaje según **qué tan buena** es:

| Puntaje | Caso | Ejemplo |
|---|---|---|
| 100 | Las siglas exactas | `escom` → **ESCOM** |
| 90 | Las siglas empiezan así | `esime` → **ESIME-ZAC**, ESIME-CUL, ESIME-TIC |
| 80 | El nombre empieza así | `escuela superior de ing` → **ESIT**, ESIA-TIC… |
| 70 | Cada palabra escrita empieza alguna palabra del nombre | `escuela superior co` → **ESCOM** |
| 60 | El nombre contiene el texto en medio | `computo` → **ESCOM**, División de Cómputo… |
| 50 | Las siglas contienen el texto en medio | |
| 0 | No coincide — se descarta | |

Los números no significan nada por sí solos; lo único que importa es el **orden**.

El nivel 70 es el que hace posible escribir a pedazos. `escuela superior co` se parte en tres
palabras y se exige que cada una sea el principio de alguna palabra del nombre: `escuela` ✓,
`superior` ✓, `co` es el principio de `computo` ✓.

### 4. Desempatar

Con el mismo puntaje, el orden es:

1. **Primero las unidades académicas.** De las 209 dependencias solo 45 son escuelas, pero son
   el lugar de la enorme mayoría de las quejas. Sin este desempate, escribir `computo` ponía
   "División de Cómputo" (un área administrativa) por encima de "Escuela Superior de Cómputo",
   nada más porque su nombre es más corto.
2. **Después el nombre más corto.** "Escuela Superior de Cómputo" antes que "Escuela Superior
   de Comercio y Administración, Unidad Santo Tomás".
3. **Y al final, alfabéticamente** en español, para que el orden sea estable y predecible.

### 5. Mostrar como máximo 8

Más de ocho renglones deja de ser una lista y vuelve a ser el problema que estamos resolviendo.
Si hay más coincidencias, abajo aparece "y N más — sigue escribiendo para afinar la búsqueda",
que le dice al usuario qué hacer en vez de dejarlo creyendo que eso es todo.

### 6. Resaltar lo que coincidió

El tramo del nombre que coincide se pinta en guinda y en negritas. Se hace partiendo el nombre
en tres pedazos —antes, coincidencia, después— y pintando cada uno en su propio `<span>`.

**No se usa `innerHTML`.** Insertar HTML armado a mano con texto que viene de la base de datos
es exactamente el patrón que abre un XSS. Aquí el texto pasa por la interpolación normal de
Angular, que lo escapa sola.

---

## Cómo se comporta

### Con el teclado

| Tecla | Qué hace |
|---|---|
| Escribir | Filtra y abre la lista |
| ↓ / ↑ | Se mueve entre las sugerencias (da la vuelta al llegar al final) |
| Enter | Elige la resaltada |
| Esc | Cierra la lista sin elegir |
| Tab | Cierra y pasa al siguiente campo |

**El `Enter` lleva `preventDefault()` cuando la lista está abierta**, y no es un detalle menor:
el campo vive dentro de un `<form>`, así que sin eso un Enter para elegir una escuela enviaría
la queja completa a medio llenar.

### Accesibilidad

Sigue el patrón *combobox* de ARIA: `role="combobox"` en la caja, `role="listbox"` en la lista,
`role="option"` en cada renglón, `aria-expanded` para decir si está abierta y
`aria-activedescendant` para que un lector de pantalla anuncie la opción resaltada sin mover el
foco real del campo. En un portal de gobierno esto no es opcional.

### No se puede escribir cualquier cosa

Es la parte que evita datos basura. El componente guarda por separado **lo que se ve escrito**
y **la clave elegida**. En cuanto el usuario teclea algo, la clave se borra:

```ts
if (this.valor) {
  this.valor = '';
  this.onChange('');   // el formulario se entera de que ya no hay nada válido
}
```

Así que escribir "mi escuela" y enviar no manda un lugar inventado: manda vacío, y la
validación del formulario lo detiene. Si se queda texto sin elegir opción, aparece un aviso
explícito debajo del campo.

Se eligió avisar en vez de borrarle el texto al usuario. Borrar lo que alguien acaba de teclear
se siente como una falla del sistema, aunque técnicamente sea "más limpio".

---

## Cómo se usa

Es un `ControlValueAccessor`, igual que el `app-datepicker`, así que entra en el formulario
como cualquier otro campo:

```html
<app-autocompletar-dependencia
  [(ngModel)]="unidadAcademica"
  name="unidadAcademica"
  [dependencias]="dependencias"
  [cargando]="cargandoDependencias"
  required
></app-autocompletar-dependencia>
```

| Entrada | Para qué |
|---|---|
| `[(ngModel)]` | La **clave** de la dependencia (`"ESCOM"`), no el nombre |
| `[dependencias]` | El catálogo completo, tal como lo devuelve `CatalogoService` |
| `[cargando]` | Deshabilita la caja y muestra "Cargando dependencias…" |
| `(cambio)` | Opcional, por si el padre necesita reaccionar a la selección |

Si el formulario ya trae una clave antes de que llegue el catálogo (por ejemplo al editar una
queja existente), el componente la guarda y muestra el nombre completo en cuanto las
dependencias cargan.

---

## Dónde está puesto y dónde falta

| Pantalla | Estado |
|---|---|
| `registro-queja-publico` — queja sin cuenta | ✅ Cambiado |
| `panel/nueva-queja` — queja desde el panel | ⏳ Sigue con el `<select>` |
| `panel/perfil` — unidad académica del usuario | ⏳ Sigue con el `<select>` |
| `panel/mis-quejas` — filtro | ⏳ Sigue con el `<select>` |
| `panel/queja-detalle` — edición | ⏳ Sigue con el `<select>` |

Las cuatro pendientes son un cambio de una sola etiqueta cada una, con el mismo `[(ngModel)]`.
Se dejaron fuera a propósito para que este cambio se pueda revisar y probar solo. En las dos de
filtro (`mis-quejas`, `queja-detalle`) hay que decidir antes qué pasa con la opción "todas",
que el autocompletado no contempla hoy.

---

## Verificación

Se probó el algoritmo contra el catálogo real (`dependencias_seed.sql`, 208 registros
parseados):

| Se escribe | Sale primero | Coincidencias |
|---|---|---|
| `ESCOM` | Escuela Superior de Cómputo | 1 |
| `escom` | Escuela Superior de Cómputo | 1 |
| `escuela superior co` | Escuela Superior de Cómputo | 3 |
| `computo` | Escuela Superior de Cómputo | 5 |
| `cómputo` | Escuela Superior de Cómputo | 5 |
| `esime` | ESIME-TIC / CUL / ZAC | 4 |
| `esime zac` | ESIME, Unidad Zacatenco | 1 |
| `cecyt 9` | CECyT No. 9 "Juan de Dios Bátiz" | 1 |
| `upiicsa` | UPIICSA | 1 |
| `medicina` | Escuela Superior de Medicina | 2 |
| `zacatenco` | ESIA-ZAC, ESIME-ZAC, CENDI Zacatenco | 4 |

`esime zac` acierta gracias a la **abreviatura** del catálogo (`ESIME Zacatenco`), no a la
clave (`ESIME-ZAC`, con guion). Por eso la búsqueda mira los tres campos —clave, abreviatura y
nombre— y no solo la clave.

---

## Si algo hay que ajustar

| Quiero… | Dónde |
|---|---|
| Mostrar más o menos sugerencias | `MAXIMO_SUGERENCIAS` |
| Cambiar qué tipo se prefiere al desempatar | `TIPO_PREFERIDO` |
| Cambiar el orden de los resultados | `puntuar()` y el `.sort()` de `buscar()` |
| Buscar también en la categoría | agregar el campo al índice y a `puntuar()` |
| Mover la búsqueda al servidor | reemplazar `buscar()` por una llamada con *debounce*; lo demás no cambia |
