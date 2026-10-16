package ipn.escom.defensoria.subdefensoria.service;

import com.lowagie.text.Chunk;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.lowagie.text.pdf.draw.LineSeparator;
import ipn.escom.defensoria.subdefensoria.entity.AcuerdoConclusion;
import ipn.escom.defensoria.subdefensoria.entity.ExpedienteInvestigacion;
import ipn.escom.defensoria.subdefensoria.entity.OficioInformacion;
import ipn.escom.defensoria.subdefensoria.entity.RecordatorioUrgencia;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Documentos PDF de Subdefensoría: oficio de solicitud de información (CU-SD-06),
 * recordatorio de urgencia (CU-SD-07), acuerdo de conciliación y acuerdo de conclusión
 * (CU-SD-09).
 *
 * No existe un formato oficial todavía: son de EJEMPLO y siguen la misma estructura que el
 * oficio de remisión de Primer Contacto (membrete, datos del documento, lugar y fecha,
 * destinatario, cuerpo, firma y copias). Cuando la Defensoría defina sus formatos, basta con
 * cambiar esta clase.
 */
@Service
public class DocumentoPdfService {

    private static final Color GUINDA = new Color(0x6C, 0x1D, 0x45);
    private static final Color GRIS = new Color(0x55, 0x55, 0x55);

    private static final Locale ES_MX = Locale.forLanguageTag("es-MX");
    private static final DateTimeFormatter FECHA_LARGA = DateTimeFormatter.ofPattern("d 'de' MMMM 'de' yyyy", ES_MX);

    private static final String AREA = "Defensoría de los Derechos Politécnicos · Subdefensoría";

    private final Font titulo = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 13, GUINDA);
    private final Font subtitulo = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, GUINDA);
    private final Font normal = FontFactory.getFont(FontFactory.HELVETICA, 10.5f, Color.BLACK);
    private final Font negrita = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10.5f, Color.BLACK);
    private final Font pequena = FontFactory.getFont(FontFactory.HELVETICA, 8.5f, GRIS);

    // ---------------------------------------------------------------------------------
    // CU-SD-06: oficio de solicitud de información al director
    // ---------------------------------------------------------------------------------

    public byte[] oficio(OficioInformacion oficio, ExpedienteInvestigacion expediente) {
        return generar("Oficio " + oficio.getNumeroOficio(), documento -> {
            datos(documento, new String[]{
                    "Oficio núm.:", oficio.getNumeroOficio(),
                    "Expediente:", expediente.getFolio(),
                    "Folio de la queja:", folioQueja(expediente),
                    "Asunto:", "Solicitud de información"
            }, oficio.getFechaEnvio());
            destinatario(documento, oficio.getDestinatarioNombre(), oficio.getUnidadAcademica());

            parrafo(documento,
                    "Por medio del presente, y en seguimiento a la queja registrada con el folio "
                            + folioQueja(expediente) + ", relativa a \"" + valor(expediente.getAsunto(), "sin asunto")
                            + "\", me permito solicitarle la información que a continuación se describe, "
                            + "a fin de que esta Defensoría esté en posibilidad de integrar el expediente "
                            + "de investigación " + expediente.getFolio() + ".");
            seccion(documento, "Información solicitada");
            parrafo(documento, oficio.getContenidoRedactado());
            seccion(documento, "Plazo de respuesta");
            parrafo(documento, "Mucho agradeceré que la respuesta se haga llegar a más tardar el "
                    + fecha(oficio.getFechaLimite()) + ", por correo electrónico o por oficio dirigido a esta Defensoría.");
            cierre(documento);
            firma(documento, expediente);
            copias(documento, expediente, oficio.getDestinatarioCorreo());
        });
    }

    // ---------------------------------------------------------------------------------
    // CU-SD-07: recordatorio de urgencia de un oficio sin respuesta
    // ---------------------------------------------------------------------------------

    public byte[] recordatorio(
            RecordatorioUrgencia recordatorio,
            int numero,
            OficioInformacion oficio,
            ExpedienteInvestigacion expediente
    ) {
        String numeroRecordatorio = oficio.getNumeroOficio() + "/R" + numero;
        return generar("Recordatorio " + numeroRecordatorio, documento -> {
            datos(documento, new String[]{
                    "Recordatorio núm.:", numeroRecordatorio,
                    "Oficio de referencia:", oficio.getNumeroOficio(),
                    "Expediente:", expediente.getFolio(),
                    "Asunto:", "Recordatorio de urgencia"
            }, recordatorio.getFechaEnvio() != null ? recordatorio.getFechaEnvio().toLocalDate() : null);
            destinatario(documento, oficio.getDestinatarioNombre(), oficio.getUnidadAcademica());

            parrafo(documento,
                    "En relación con el oficio " + oficio.getNumeroOficio() + " de fecha " + fecha(oficio.getFechaEnvio())
                            + ", mediante el cual se solicitó información para la integración del expediente "
                            + expediente.getFolio() + ", me permito recordarle que a la fecha no se ha recibido "
                            + "la respuesta correspondiente"
                            + (recordatorio.getDiasRetraso() != null && recordatorio.getDiasRetraso() > 0
                            ? ", con un retraso de " + recordatorio.getDiasRetraso() + " días hábiles."
                            : "."));
            seccion(documento, "Mensaje");
            parrafo(documento, recordatorio.getMensaje());
            if (recordatorio.getMedidasOfrecidas() != null && !recordatorio.getMedidasOfrecidas().isBlank()) {
                seccion(documento, "Medidas ofrecidas");
                parrafo(documento, recordatorio.getMedidasOfrecidas());
            }
            seccion(documento, "Nuevo plazo");
            parrafo(documento, "Mucho agradeceré que la respuesta se haga llegar a más tardar el "
                    + fecha(oficio.getFechaLimite()) + ".");
            cierre(documento);
            firma(documento, expediente);
            copias(documento, expediente, oficio.getDestinatarioCorreo());
        });
    }

    // ---------------------------------------------------------------------------------
    // CU-SD-09: acuerdo de conciliación (lo responde el quejoso) y acuerdo de conclusión
    // ---------------------------------------------------------------------------------

    public byte[] acuerdoConciliacion(AcuerdoConclusion acuerdo, ExpedienteInvestigacion expediente) {
        return generar("Acuerdo de conciliación " + expediente.getFolio(), documento -> {
            datos(documento, new String[]{
                    "Expediente:", expediente.getFolio(),
                    "Folio de la queja:", folioQueja(expediente),
                    "Asunto:", "Acuerdo de conciliación"
            }, fechaDe(acuerdo.getFechaEnvioSecretarial()));
            destinatario(documento, valor(expediente.getQuejosoNombre(), "Persona quejosa"), null);

            parrafo(documento,
                    "Con motivo de la queja registrada con el folio " + folioQueja(expediente) + ", relativa a \""
                            + valor(expediente.getAsunto(), "sin asunto") + "\", y una vez recabada la información "
                            + "de la unidad académica, esta Defensoría le propone el siguiente acuerdo:");
            seccion(documento, "Acuerdo propuesto");
            parrafo(documento, acuerdo.getTextoAcuerdo());
            seccion(documento, "Respuesta de la persona quejosa");
            if ("ACEPTADO".equals(acuerdo.getRespuestaQuejoso())) {
                parrafo(documento, "Aceptado el " + fecha(fechaDe(acuerdo.getFechaRespuestaQuejoso())) + ".");
            } else if ("RECHAZADO".equals(acuerdo.getRespuestaQuejoso())) {
                parrafo(documento, "Rechazado el " + fecha(fechaDe(acuerdo.getFechaRespuestaQuejoso()))
                        + ". Motivo: " + valor(acuerdo.getComentarioQuejoso(), "sin motivo registrado") + ".");
            } else {
                parrafo(documento, "Pendiente. La persona quejosa puede aceptarlo o rechazarlo indicando el "
                        + "motivo desde el enlace que se le envió por correo; si lo rechaza, la investigación continúa.");
            }
            firma(documento, expediente);
            copias(documento, expediente, null);
        });
    }

    public byte[] acuerdoConclusion(AcuerdoConclusion acuerdo, ExpedienteInvestigacion expediente) {
        return generar("Acuerdo de conclusión " + expediente.getFolio(), documento -> {
            datos(documento, new String[]{
                    "Expediente:", expediente.getFolio(),
                    "Folio de la queja:", folioQueja(expediente),
                    "Asunto:", "Acuerdo de conclusión"
            }, fechaDe(acuerdo.getFechaConclusion()));

            Paragraph encabezado = new Paragraph("ACUERDO DE CONCLUSIÓN", titulo);
            encabezado.setAlignment(Element.ALIGN_CENTER);
            encabezado.setSpacingAfter(8);
            documento.add(encabezado);

            parrafo(documento,
                    "Visto el expediente de investigación " + expediente.getFolio() + ", derivado de la queja "
                            + folioQueja(expediente) + " presentada por " + valor(expediente.getQuejosoNombre(), "la persona quejosa")
                            + (expediente.getUnidadAcademica() != null ? ", de " + expediente.getUnidadAcademica() : "")
                            + ", y toda vez que la persona quejosa aceptó el acuerdo de conciliación propuesto, "
                            + "se emite el presente acuerdo de conclusión.");
            seccion(documento, "Acuerdo de conciliación aceptado");
            parrafo(documento, valor(acuerdo.getTextoAcuerdo(), "—"));
            seccion(documento, "Conclusión");
            parrafo(documento, valor(acuerdo.getTextoConclusion(), "—"));
            parrafo(documento, "En consecuencia, se da por concluido el expediente y se ordena su archivo como asunto "
                    + "total y definitivamente concluido.");
            firma(documento, expediente);
            copias(documento, expediente, null);
        });
    }

    // ---------------------------------------------------------------------------------

    private interface Contenido {
        void escribir(Document documento) throws DocumentException;
    }

    private byte[] generar(String tituloDocumento, Contenido contenido) {
        ByteArrayOutputStream salida = new ByteArrayOutputStream();
        Document documento = new Document(PageSize.LETTER, 64, 64, 44, 40);
        try {
            PdfWriter.getInstance(documento, salida);
            documento.addTitle(tituloDocumento);
            documento.addAuthor("Defensoría de los Derechos Politécnicos");
            documento.open();
            membrete(documento);
            contenido.escribir(documento);
            documento.close();
        } catch (DocumentException ex) {
            throw new IllegalStateException("No se pudo generar el PDF: " + tituloDocumento, ex);
        }
        return salida.toByteArray();
    }

    private void membrete(Document documento) throws DocumentException {
        Paragraph institucion = new Paragraph("INSTITUTO POLITÉCNICO NACIONAL", titulo);
        institucion.setAlignment(Element.ALIGN_CENTER);
        documento.add(institucion);

        Paragraph area = new Paragraph(AREA, subtitulo);
        area.setAlignment(Element.ALIGN_CENTER);
        documento.add(area);

        documento.add(new Chunk(new LineSeparator(1.2f, 100, GUINDA, Element.ALIGN_CENTER, -4)));
        documento.add(espacio(10));
    }

    /** Tabla de datos (pares etiqueta, valor) alineada a la derecha + lugar y fecha. */
    private void datos(Document documento, String[] etiquetaValor, LocalDate fecha) throws DocumentException {
        PdfPTable tabla = new PdfPTable(2);
        tabla.setWidthPercentage(62);
        tabla.setHorizontalAlignment(Element.ALIGN_RIGHT);
        tabla.setWidths(new float[]{0.9f, 1.6f});
        for (int i = 0; i + 1 < etiquetaValor.length; i += 2) {
            tabla.addCell(celda(new Phrase(etiquetaValor[i], negrita)));
            tabla.addCell(celda(new Phrase(valor(etiquetaValor[i + 1], "—"), normal)));
        }
        documento.add(tabla);

        Paragraph lugarFecha = new Paragraph("Ciudad de México, a " + fecha(fecha != null ? fecha : LocalDate.now()) + ".", normal);
        lugarFecha.setAlignment(Element.ALIGN_RIGHT);
        lugarFecha.setSpacingBefore(8);
        documento.add(lugarFecha);
        documento.add(espacio(14));
    }

    private void destinatario(Document documento, String nombre, String unidadAcademica) throws DocumentException {
        documento.add(new Paragraph(valor(nombre, "Destinatario").toUpperCase(ES_MX), negrita));
        if (unidadAcademica != null && !unidadAcademica.isBlank()) {
            documento.add(new Paragraph(unidadAcademica, normal));
        }
        documento.add(new Paragraph("P R E S E N T E", negrita));
        documento.add(espacio(12));
    }

    private void cierre(Document documento) throws DocumentException {
        documento.add(espacio(10));
        parrafo(documento, "Sin otro particular, aprovecho la ocasión para enviarle un cordial saludo.");
    }

    private void firma(Document documento, ExpedienteInvestigacion expediente) throws DocumentException {
        documento.add(espacio(14));

        Paragraph atentamente = new Paragraph("A T E N T A M E N T E", negrita);
        atentamente.setAlignment(Element.ALIGN_CENTER);
        documento.add(atentamente);

        Paragraph lema = new Paragraph("\"La Técnica al Servicio de la Patria\"", normal);
        lema.setAlignment(Element.ALIGN_CENTER);
        documento.add(lema);

        documento.add(espacio(26));

        Paragraph linea = new Paragraph("______________________________________", normal);
        linea.setAlignment(Element.ALIGN_CENTER);
        documento.add(linea);

        Paragraph nombre = new Paragraph(valor(expediente.getAbogadoAsesorNombre(), "Subdefensor(a)"), negrita);
        nombre.setAlignment(Element.ALIGN_CENTER);
        documento.add(nombre);

        Paragraph cargo = new Paragraph("Subdefensoría\nDefensoría de los Derechos Politécnicos", normal);
        cargo.setAlignment(Element.ALIGN_CENTER);
        documento.add(cargo);
    }

    private void copias(Document documento, ExpedienteInvestigacion expediente, String correoDestinatario)
            throws DocumentException {
        documento.add(espacio(10));
        StringBuilder texto = new StringBuilder();
        if (correoDestinatario != null && !correoDestinatario.isBlank()) {
            texto.append("Enviado por correo electrónico a ").append(correoDestinatario).append(".\n");
        }
        texto.append("c.c.p. ").append(valor(expediente.getQuejosoNombre(), "Persona quejosa")).append(".- Para su conocimiento.\n");
        texto.append("c.c.p. Expediente ").append(expediente.getFolio()).append(".");
        documento.add(new Paragraph(texto.toString(), pequena));
    }

    private PdfPCell celda(Phrase contenido) {
        PdfPCell celda = new PdfPCell(contenido);
        celda.setBorder(PdfPCell.NO_BORDER);
        celda.setPaddingBottom(2);
        return celda;
    }

    private void seccion(Document documento, String texto) throws DocumentException {
        Paragraph p = new Paragraph(texto, subtitulo);
        p.setSpacingBefore(10);
        p.setSpacingAfter(3);
        documento.add(p);
    }

    private void parrafo(Document documento, String texto) throws DocumentException {
        Paragraph p = new Paragraph(valor(texto, "—"), normal);
        p.setAlignment(Element.ALIGN_JUSTIFIED);
        p.setLeading(15);
        p.setSpacingAfter(4);
        documento.add(p);
    }

    private Paragraph espacio(float alto) {
        Paragraph p = new Paragraph(" ");
        p.setLeading(alto);
        return p;
    }

    private static String folioQueja(ExpedienteInvestigacion expediente) {
        return expediente.getFolioQueja() != null ? expediente.getFolioQueja() : expediente.getFolioOrigen();
    }

    private static LocalDate fechaDe(LocalDateTime fechaHora) {
        return fechaHora != null ? fechaHora.toLocalDate() : null;
    }

    private static String fecha(LocalDate fecha) {
        return fecha != null ? fecha.format(FECHA_LARGA) : "—";
    }

    private static String valor(String texto, String porDefecto) {
        return texto == null || texto.isBlank() ? porDefecto : texto.strip();
    }
}
