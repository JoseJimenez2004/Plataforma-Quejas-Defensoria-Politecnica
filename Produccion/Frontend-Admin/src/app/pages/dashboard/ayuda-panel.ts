/**
 * Textos de ayuda del Panel de la Defensora ("focos de ayuda") y pasos del recorrido guiado.
 * Cada sección explica qué muestra, cómo se usa y qué decisión ayuda a tomar.
 */
export interface AyudaSeccion {
  titulo: string;
  queMuestra: string;
  comoUsar: string;
  paraDecidir: string;
}

export type SeccionPanel = 'kpis' | 'hallazgos' | 'unidades' | 'tiempo' | 'perfil' | 'responsables' | 'detalle';

export const NOMBRE_SECCION: Record<SeccionPanel, string> = {
  kpis: 'Indicadores por etapa',
  hallazgos: 'Lo más relevante',
  unidades: 'Unidades y ficha del titular',
  tiempo: 'Evolución y antigüedad',
  perfil: 'Nivel, rama, horario y origen',
  responsables: 'Recepcionistas y abogados',
  detalle: 'Quejas antiguas, casos y motivos',
};

export const ORDEN_INICIAL: SeccionPanel[] = ['kpis', 'hallazgos', 'unidades', 'tiempo', 'perfil', 'responsables', 'detalle'];

export const AYUDA: Record<string, AyudaSeccion> = {
  filtros: {
    titulo: 'Filtros',
    queMuestra: 'Todo el panel se calcula sobre las quejas que cumplen los filtros activos (chips de arriba).',
    comoUsar: 'Haz clic en cualquier tarjeta, barra o celda para filtrar. Con Ctrl (o ⌘) + clic, o con "Selección múltiple" activa, sumas varios valores de la misma dimensión (p. ej. ESCOM y ESIME). Abre "Filtros" para elegir valores de una lista, poner un rango de fechas o buscar por folio. Guarda combinaciones frecuentes en "Mis vistas" y comparte el enlace: los filtros viajan en la dirección.',
    paraDecidir: 'Responder preguntas concretas: "¿qué pasa con las quejas de ESCOM que nadie ha tomado este mes?".',
  },
  kpis: {
    titulo: 'Indicadores por etapa',
    queMuestra: 'Cuántas quejas hay en cada etapa del proceso dentro de la selección, y la mediana de días que llevan las abiertas.',
    comoUsar: 'Clic en una etapa para ver solo esas quejas en todo el panel; otro clic la quita. "Sin tomar > 3 días" cuenta las de Recepción que nadie ha abierto.',
    paraDecidir: 'Detectar cuellos de botella: si Recepción concentra la mayoría, hace falta reforzar ese equipo o reasignar.',
  },
  hallazgos: {
    titulo: 'Lo más relevante',
    queMuestra: 'Frases calculadas automáticamente con lo que más destaca de la selección: concentración por unidad, rezago, alzas contra el periodo anterior y horarios pico.',
    comoUsar: 'Se actualizan con cada filtro. Úsalas como resumen para reportes o reuniones.',
    paraDecidir: 'Saber en segundos dónde poner atención sin leer cada gráfica.',
  },
  unidades: {
    titulo: 'Unidades con más quejas, por etapa',
    queMuestra: 'Mapa de calor: cada fila es una unidad y cada columna una etapa. El color más intenso indica más quejas. "vs ant." compara con el periodo anterior del mismo tamaño.',
    comoUsar: 'Clic en el nombre de la unidad para abrir su ficha con el titular (directorio del IPN); clic en una celda filtra por unidad y etapa a la vez; clic en el encabezado filtra por etapa.',
    paraDecidir: 'Identificar las unidades que requieren un oficio, una visita o una mesa de trabajo con su titular, y vigilar las que van al alza (↑ en rojo).',
  },
  ficha: {
    titulo: 'Ficha de la unidad',
    queMuestra: 'Titular, cargo y contacto según el Directorio de Servidores Públicos del IPN, más los indicadores de la unidad en la selección.',
    comoUsar: '"Escribir al titular" abre tu correo con el asunto listo; "Ver sus quejas" abre la lista filtrada.',
    paraDecidir: 'Contactar a la persona correcta y argumentar con datos (lugar en el ranking, quejas abiertas, días de espera).',
  },
  evolucion: {
    titulo: 'Evolución mensual por etapa',
    queMuestra: 'Quejas recibidas por mes en los últimos 12 meses, apiladas según la etapa en la que están hoy.',
    comoUsar: 'Clic en un mes para filtrar el panel por ese mes.',
    paraDecidir: 'Ver temporadas con más quejas (inicio de semestre, exámenes) y planear personal con anticipación.',
  },
  antiguedad: {
    titulo: 'Antigüedad de las quejas abiertas',
    queMuestra: 'Cuántos días llevan las quejas que siguen abiertas, por rango y por etapa.',
    comoUsar: 'Clic en un rango para ver solo esas quejas (p. ej. las de más de 30 días).',
    paraDecidir: 'Priorizar el rezago y vigilar el cumplimiento de plazos de atención.',
  },
  nivel: {
    titulo: 'Nivel educativo y rama',
    queMuestra: 'Distribución de las quejas por nivel (Medio Superior, Superior, Investigación…) y por rama del conocimiento, según la clasificación del directorio del IPN.',
    comoUsar: 'Clic para filtrar. Combínalo con etapa o antigüedad para comparar niveles.',
    paraDecidir: 'Orientar campañas de difusión de derechos al nivel o rama que más lo necesita.',
  },
  horario: {
    titulo: '¿Cuándo llegan las quejas?',
    queMuestra: 'Mapa de calor por día de la semana y bloque de 2 horas de llegada.',
    comoUsar: 'Clic en un día, una hora o una celda para filtrar.',
    paraDecidir: 'Ajustar turnos y horarios de atención de Recepción y ventanilla.',
  },
  origen: {
    titulo: 'Origen y prioridad',
    queMuestra: 'Si la queja se registró en línea o en ventanilla, y la prioridad que le asignó Primer Contacto.',
    comoUsar: 'Clic para filtrar.',
    paraDecidir: 'Saber qué canal se usa más y si las quejas de prioridad alta se están moviendo.',
  },
  responsables: {
    titulo: 'Recepcionistas y abogados',
    queMuestra: 'Quejas de la selección por persona responsable: total / abiertas.',
    comoUsar: 'Clic en una persona para ver su carga en todo el panel. "Nadie la ha tomado" y "Sin asignar" muestran lo que está sin dueño.',
    paraDecidir: 'Equilibrar la carga de trabajo y detectar quejas sin responsable.',
  },
  detalle: {
    titulo: 'Detalle',
    queMuestra: 'Las quejas abiertas más antiguas de la selección, los casos que el sistema marca como urgentes y los motivos de rechazo más frecuentes.',
    comoUsar: 'Clic en un folio para abrir el expediente.',
    paraDecidir: 'Actuar caso por caso sobre lo más atrasado.',
  },
};

/** Recorrido guiado: cada paso resalta una sección del panel. */
export const PASOS_RECORRIDO: { id: string; titulo: string; texto: string }[] = [
  { id: 'barra-filtros', titulo: '1. Filtros activos', texto: 'Aquí ves cuántas quejas cumplen la selección y los filtros aplicados. Quita uno con la × o todos con "Limpiar". "Filtros" abre el panel para elegir valores de una lista, fechas o folio; "Mis vistas" guarda combinaciones.' },
  { id: 'sec-kpis', titulo: '2. Todo es clicable', texto: 'Cada tarjeta, barra o celda filtra el panel completo. Ctrl + clic (⌘ en Mac) suma varios valores de la misma categoría.' },
  { id: 'sec-unidades', titulo: '3. Unidades y titulares', texto: 'Clic en una unidad para ver a su director o titular según el directorio del IPN, con correo y extensión, y cómo va contra el periodo anterior.' },
  { id: 'sec-tiempo', titulo: '4. Tiempo', texto: 'La evolución mensual muestra temporadas; la antigüedad muestra el rezago. Clic en un mes o rango para filtrar.' },
  { id: 'btn-personalizar', titulo: '5. Hazlo tuyo', texto: 'Con "Personalizar" arrastras las secciones para cambiar su orden u ocultas las que no uses. Se guarda en este navegador.' },
  { id: 'btn-ayuda', titulo: '6. Focos de ayuda', texto: 'Cada sección tiene un foco 💡 que explica qué muestra, cómo usarla y qué decisión apoya. "Ayuda" las muestra todas a la vez o repite este recorrido.' },
];
