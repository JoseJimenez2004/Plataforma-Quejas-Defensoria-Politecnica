package ipn.escom.defensoria.primercontacto.service;

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
import ipn.escom.defensoria.primercontacto.entity.ExpedientePrimerContacto;
import ipn.escom.defensoria.primercontacto.entity.NotaAnalisis;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Genera el oficio de Primer Contacto (la nota de análisis del expediente) como PDF.
 *
 * Es una constancia interna que queda dentro de la queja: membrete, número de oficio,
 * lugar, fecha y hora en que se elaboró, datos de la queja, asunto, el texto que escribió el
 * analista y su firma. Mismo estilo que el oficio de remisión (OficioRemisionPdfService).
 */
@Service
public class OficioAnalisisPdfService {

    private static final Color GUINDA = new Color(0x6C, 0x1D, 0x45);
    private static final Color GRIS = new Color(0x55, 0x55, 0x55);
    private static final Color FONDO_DATOS = new Color(0xF7, 0xEE, 0xF1);

    private static final Locale ES_MX = Locale.forLanguageTag("es-MX");
    private static final DateTimeFormatter FECHA_LARGA =
            DateTimeFormatter.ofPattern("d 'de' MMMM 'de' yyyy", ES_MX);
    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm", ES_MX);
    private static final DateTimeFormatter FECHA_HORA =
            DateTimeFormatter.ofPattern("dd/MM/yyyy 'a las' HH:mm 'h'", ES_MX);

    private final Font titulo = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 13, GUINDA);
    private final Font subtitulo = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, GUINDA);
    private final Font normal = FontFactory.getFont(FontFactory.HELVETICA, 10.5f, Color.BLACK);
    private final Font negrita = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10.5f, Color.BLACK);
    private final Font datos = FontFactory.getFont(FontFactory.HELVETICA, 9.5f, Color.BLACK);
    private final Font datosNegrita = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9.5f, Color.BLACK);
    private final Font pequena = FontFactory.getFont(FontFactory.HELVETICA, 8.5f, GRIS);

    public byte[] generar(
            NotaAnalisis nota,
            String numeroOficio,
            ExpedientePrimerContacto expediente
    ) {

        ByteArrayOutputStream salida = new ByteArrayOutputStream();
        Document documento = new Document(PageSize.LETTER, 64, 64, 44, 40);

        try {
            PdfWriter.getInstance(documento, salida);
            documento.addTitle("Oficio " + numeroOficio);
            documento.addAuthor(valor(nota.getAnalistaNombre(), "Defensoría de los Derechos Politécnicos"));
            documento.open();

            membrete(documento);
            encabezadoOficio(documento, nota, numeroOficio, expediente);
            datosQueja(documento, expediente);
            cuerpo(documento, nota);
            firma(documento, nota, expediente);

            documento.close();

        } catch (DocumentException ex) {
            throw new IllegalStateException("No se pudo generar el PDF del oficio.", ex);
        }

        return salida.toByteArray();
    }

    private void membrete(Document documento) throws DocumentException {

        Paragraph institucion = new Paragraph("INSTITUTO POLITÉCNICO NACIONAL", titulo);
        institucion.setAlignment(Element.ALIGN_CENTER);
        documento.add(institucion);

        Paragraph area = new Paragraph("Defensoría de los Derechos Politécnicos · Área de Primer Contacto", subtitulo);
        area.setAlignment(Element.ALIGN_CENTER);
        documento.add(area);

        LineSeparator linea = new LineSeparator(1.2f, 100, GUINDA, Element.ALIGN_CENTER, -4);
        documento.add(new Chunk(linea));
        documento.add(espacio(10));
    }

    private void encabezadoOficio(
            Document documento,
            NotaAnalisis nota,
            String numeroOficio,
            ExpedientePrimerContacto expediente
    ) throws DocumentException {

        PdfPTable tabla = new PdfPTable(2);
        tabla.setWidthPercentage(62);
        tabla.setHorizontalAlignment(Element.ALIGN_RIGHT);
        tabla.setWidths(new float[]{0.9f, 1.6f});

        filaDato(tabla, "Oficio núm.:", numeroOficio);
        filaDato(tabla, "Folio de la queja:", expediente.getFolioOrigen());
        filaDato(tabla, "Expediente PC:", expediente.getFolio());
        filaDato(tabla, "Asunto:", valor(nota.getAsunto(), "Nota de análisis"));

        documento.add(tabla);

        LocalDateTime fecha = nota.getFechaCreacion() != null
                ? nota.getFechaCreacion()
                : LocalDateTime.now();

        Paragraph lugarFecha = new Paragraph(
                "Ciudad de México, a " + fecha.format(FECHA_LARGA) + ", " + fecha.format(HORA) + " h.",
                normal
        );
        lugarFecha.setAlignment(Element.ALIGN_RIGHT);
        lugarFecha.setSpacingBefore(8);
        documento.add(lugarFecha);
        documento.add(espacio(10));
    }

    /* Recuadro con los datos de la queja a la que pertenece el oficio. */
    private void datosQueja(Document documento, ExpedientePrimerContacto expediente) throws DocumentException {

        PdfPTable tabla = new PdfPTable(4);
        tabla.setWidthPercentage(100);
        tabla.setWidths(new float[]{1.1f, 2f, 1f, 1.6f});

        celdaDatos(tabla, "Persona quejosa:", valor(expediente.getQuejosoNombre(), "—"));
        celdaDatos(tabla, "Unidad:", valor(expediente.getUnidadAcademica(), "—"));
        celdaDatos(tabla, "Asunto de la queja:", valor(expediente.getTema(), "—"));
        celdaDatos(tabla, "Prioridad:", prioridad(expediente.getPrioridad()));

        documento.add(tabla);
        documento.add(espacio(12));
    }

    private void cuerpo(Document documento, NotaAnalisis nota) throws DocumentException {

        // Respeta los párrafos que escribió el analista (una línea en blanco o un salto).
        for (String bloque : valor(nota.getContenido(), "").strip().split("\\R+")) {
            if (!bloque.isBlank()) {
                parrafo(documento, bloque.strip());
            }
        }
    }

    private void firma(
            Document documento,
            NotaAnalisis nota,
            ExpedientePrimerContacto expediente
    ) throws DocumentException {

        documento.add(espacio(20));

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

        Paragraph nombre = new Paragraph(valor(nota.getAnalistaNombre(), "Analista de Primer Contacto"), negrita);
        nombre.setAlignment(Element.ALIGN_CENTER);
        documento.add(nombre);

        Paragraph cargo = new Paragraph("Analista de Primer Contacto\nDefensoría de los Derechos Politécnicos", normal);
        cargo.setAlignment(Element.ALIGN_CENTER);
        documento.add(cargo);

        documento.add(espacio(14));

        StringBuilder constancia = new StringBuilder("Elaborado por ")
                .append(valor(nota.getAnalistaNombre(), "el analista"));
        if (nota.getFechaCreacion() != null) {
            constancia.append(" el ").append(nota.getFechaCreacion().format(FECHA_HORA));
        }
        constancia.append(".");
        if (nota.getFechaActualizacion() != null) {
            constancia.append(" Última modificación: ")
                    .append(nota.getFechaActualizacion().format(FECHA_HORA)).append(".");
        }
        constancia.append("\nc.c.p. Expediente ").append(expediente.getFolio()).append(".");

        documento.add(new Paragraph(constancia.toString(), pequena));
    }

    private void filaDato(PdfPTable tabla, String etiqueta, String valor) {
        tabla.addCell(celda(new Phrase(etiqueta, negrita)));
        tabla.addCell(celda(new Phrase(valor(valor, "—"), normal)));
    }

    private void celdaDatos(PdfPTable tabla, String etiqueta, String valor) {
        for (PdfPCell celda : new PdfPCell[]{
                new PdfPCell(new Phrase(etiqueta, datosNegrita)),
                new PdfPCell(new Phrase(valor, datos))
        }) {
            celda.setBorder(PdfPCell.NO_BORDER);
            celda.setBackgroundColor(FONDO_DATOS);
            celda.setPadding(5);
            tabla.addCell(celda);
        }
    }

    private PdfPCell celda(Phrase contenido) {
        PdfPCell celda = new PdfPCell(contenido);
        celda.setBorder(PdfPCell.NO_BORDER);
        celda.setPaddingBottom(2);
        return celda;
    }

    private void parrafo(Document documento, String texto) throws DocumentException {
        Paragraph p = new Paragraph(texto, normal);
        p.setAlignment(Element.ALIGN_JUSTIFIED);
        p.setLeading(15);
        p.setSpacingAfter(6);
        documento.add(p);
    }

    private Paragraph espacio(float alto) {
        Paragraph p = new Paragraph(" ");
        p.setLeading(alto);
        return p;
    }

    /* ALTA -> Alta */
    private String prioridad(String codigo) {
        if (codigo == null || codigo.isBlank()) {
            return "Sin asignar";
        }
        String texto = codigo.strip().toLowerCase(ES_MX);
        return Character.toUpperCase(texto.charAt(0)) + texto.substring(1);
    }

    private String valor(String texto, String porDefecto) {
        return texto == null || texto.isBlank() ? porDefecto : texto;
    }
}
