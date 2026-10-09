/**
 * Catálogo de iconos de la aplicación.
 *
 * Un solo lugar donde se decide qué iconos existen y cómo se llaman. Los nombres están en
 * español a propósito: en las plantillas se lee `<lucide-icon [img]="ICONOS.ojo">`, no
 * `[img]="Eye"`, igual que el resto del código del proyecto.
 *
 * POR QUÉ ESTE ARCHIVO Y NO IMPORTAR DE lucide-angular EN CADA COMPONENTE:
 *
 *  1. **Peso del bundle.** lucide-angular trae más de 1500 iconos. Al importar solo los que
 *     se registran aquí, el compilador descarta el resto (tree-shaking). Si cada pantalla
 *     importara los suyos por su cuenta, sería fácil terminar arrastrando de más — y el
 *     presupuesto del bundle ya se rebasó una vez.
 *  2. **Consistencia.** Que "ver" sea siempre el mismo ojo en todas las pantallas, y que
 *     cambiarlo sea editar una línea aquí y no buscar por todo el proyecto.
 *
 * PARA AGREGAR UN ICONO: búscalo en https://lucide.dev, impórtalo arriba y agrégalo al
 * objeto ICONOS con su nombre en español. Nada más.
 */
import {
  Eye,
  Pencil,
  Trash2,
  Search,
  Plus,
  X,
  Download,
  FileText,
  Image,
  Check,
  AlertTriangle,
  Clock,
  Send,
  Ban,
  Paperclip,
  Info,
} from 'lucide-angular';

export const ICONOS = {
  ojo: Eye,
  lapiz: Pencil,
  bote: Trash2,
  lupa: Search,
  mas: Plus,
  equis: X,
  descargar: Download,
  documento: FileText,
  imagen: Image,
  palomita: Check,
  advertencia: AlertTriangle,
  reloj: Clock,
  enviar: Send,
  prohibido: Ban,
  clip: Paperclip,
  info: Info,
} as const;

/** Los iconos que se registran en el módulo de Lucide (ver app.config.ts). */
export const ICONOS_REGISTRADOS = {
  Eye,
  Pencil,
  Trash2,
  Search,
  Plus,
  X,
  Download,
  FileText,
  Image,
  Check,
  AlertTriangle,
  Clock,
  Send,
  Ban,
  Paperclip,
  Info,
};
