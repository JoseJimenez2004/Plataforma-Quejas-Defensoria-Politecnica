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
import ipn.escom.defensoria.primercontacto.entity.DictamenPrimerContacto;
import ipn.escom.defensoria.primercontacto.entity.EvidenciaPrimerContacto;
import ipn.escom.defensoria.primercontacto.entity.ExpedientePrimerContacto;
import ipn.escom.defensoria.primercontacto.entity.RemisionExterna;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

/**
 * Genera el oficio de remisión externa (CU-PC-09) como PDF.
 *
 * No existe un formato oficial todavía; este sigue la estructura habitual de un oficio de
 * canalización entre instancias: membrete, número de oficio, lugar y fecha, destinatario,
 * fundamento, síntesis de los hechos, motivo de la no competencia, orientación dada al
 * quejoso, anexos, firma y copias. Cuando la Defensoría defina su formato, basta con
 * cambiar esta clase.
 */
@Service
public class OficioRemisionPdfService {

    private static final Color GUINDA = new Color(0x6C, 0x1D, 0x45);
    private static final Color GRIS = new Color(0x55, 0x55, 0x55);

    private static final Locale ES_MX = Locale.forLanguageTag("es-MX");
    private static final DateTimeFormatter FECHA_LARGA =
            DateTimeFormatter.ofPattern("d 'de' MMMM 'de' yyyy", ES_MX);

    private final Font titulo = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 13, GUINDA);
    private final Font subtitulo = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, GUINDA);
    private final Font normal = FontFactory.getFont(FontFactory.HELVETICA, 10.5f, Color.BLACK);
    private final Font negrita = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10.5f, Color.BLACK);
    private final Font pequena = FontFactory.getFont(FontFactory.HELVETICA, 8.5f, GRIS);

    public byte[] generar(
            RemisionExterna remision,
            ExpedientePrimerContacto expediente,
            DictamenPrimerContacto dictamen,
            List<EvidenciaPrimerContacto> evidencias
    ) {

        ByteArrayOutputStream salida = new ByteArrayOutputStream();
        Document documento = new Document(PageSize.LETTER, 64, 64, 44, 40);

        try {
            PdfWriter.getInstance(documento, salida);
            documento.addTitle("Oficio de remisión " + remision.getNumeroOficio());
            documento.addAuthor("Defensoría de los Derechos Politécnicos");
            documento.open();

            membrete(documento);
            encabezadoOficio(documento, remision, expediente);
            destinatario(documento, remision);
            cuerpo(documento, remision, expediente, dictamen, evidencias);
            firma(documento, remision, expediente);

            documento.close();

        } catch (DocumentException ex) {
            throw new IllegalStateException("No se pudo generar el PDF de la remisión.", ex);
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
            RemisionExterna remision,
            ExpedientePrimerContacto expediente
    ) throws DocumentException {

        PdfPTable tabla = new PdfPTable(2);
        tabla.setWidthPercentage(62);
        tabla.setHorizontalAlignment(Element.ALIGN_RIGHT);
        tabla.setWidths(new float[]{0.9f, 1.6f});

        filaDato(tabla, "Oficio núm.:", remision.getNumeroOficio());
        filaDato(tabla, "Folio de la queja:", expediente.getFolioOrigen());
        filaDato(tabla, "Expediente PC:", expediente.getFolio());
        filaDato(tabla, "Asunto:", "Remisión de queja por no competencia");

        documento.add(tabla);

        LocalDateTime fecha = remision.getFechaRemision() != null
                ? remision.getFechaRemision()
                : LocalDateTime.now();

        Paragraph lugarFecha = new Paragraph(
                "Ciudad de México, a " + fecha.format(FECHA_LARGA) + ".",
                normal
        );
        lugarFecha.setAlignment(Element.ALIGN_RIGHT);
        lugarFecha.setSpacingBefore(8);
        documento.add(lugarFecha);
        documento.add(espacio(14));
    }

    private void destinatario(Document documento, RemisionExterna remision) throws DocumentException {
        documento.add(new Paragraph(remision.getAutoridadRemision().toUpperCase(ES_MX), negrita));
        documento.add(new Paragraph("P R E S E N T E", negrita));
        documento.add(espacio(12));
    }

    private void cuerpo(
            Document documento,
            RemisionExterna remision,
            ExpedientePrimerContacto expediente,
            DictamenPrimerContacto dictamen,
            List<EvidenciaPrimerContacto> evidencias
    ) throws DocumentException {

        String quejoso = valor(expediente.getQuejosoNombre(), "la persona quejosa");
        String unidad = expediente.getUnidadAcademica() != null
                ? ", de " + expediente.getUnidadAcademica()
                : "";

        parrafo(documento,
                "Por medio del presente, y con fundamento en " + remision.getJustificacionLegal().trim()
                        + ", me permito remitir a esa instancia la queja registrada con el folio "
                        + expediente.getFolioOrigen() + ", presentada por " + quejoso + unidad
                        + ", relativa a \"" + valor(expediente.getTema(), "sin asunto especificado") + "\", "
                        + "para su atención en el ámbito de sus atribuciones.");

        seccion(documento, "Síntesis de los hechos");
        parrafo(documento, valor(expediente.getDescripcionHechos(), "Sin descripción registrada."));

        if (dictamen != null) {
            seccion(documento, "Motivo de la remisión");
            parrafo(documento,
                    "Del análisis realizado por el área de Primer Contacto se determinó que el asunto "
                            + "no es competencia de esta Defensoría, por lo siguiente: "
                            + dictamen.getJustificacion().trim());
        }

        if (remision.getSugerenciaQuejoso() != null && !remision.getSugerenciaQuejoso().isBlank()) {
            seccion(documento, "Orientación proporcionada a la persona quejosa");
            parrafo(documento, remision.getSugerenciaQuejoso().trim());
        }

        if (Boolean.TRUE.equals(remision.getAdjuntarExpediente())) {
            seccion(documento, "Anexos");

            if (evidencias.isEmpty()) {
                parrafo(documento, "Copia del expediente de Primer Contacto " + expediente.getFolio() + ".");
            } else {
                parrafo(documento, "Copia del expediente de Primer Contacto " + expediente.getFolio()
                        + ", con las siguientes evidencias aportadas por la persona quejosa:");
                com.lowagie.text.List lista = new com.lowagie.text.List(false, 12);
                lista.setListSymbol(new Chunk("•  ", normal));
                evidencias.forEach(e -> lista.add(new com.lowagie.text.ListItem(
                        valor(e.getNombreArchivo(), "Evidencia sin nombre"), normal)));
                documento.add(lista);
            }
        }

        documento.add(espacio(10));
        parrafo(documento, "Sin otro particular, aprovecho la ocasión para enviarle un cordial saludo.");
    }

    private void firma(
            Document documento,
            RemisionExterna remision,
            ExpedientePrimerContacto expediente
    ) throws DocumentException {

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

        Paragraph nombre = new Paragraph(valor(remision.getAnalistaNombre(), "Analista de Primer Contacto"), negrita);
        nombre.setAlignment(Element.ALIGN_CENTER);
        documento.add(nombre);

        Paragraph cargo = new Paragraph("Analista de Primer Contacto\nDefensoría de los Derechos Politécnicos", normal);
        cargo.setAlignment(Element.ALIGN_CENTER);
        documento.add(cargo);

        documento.add(espacio(10));

        documento.add(new Paragraph(
                "c.c.p. " + valor(expediente.getQuejosoNombre(), "Persona quejosa")
                        + " (" + valor(expediente.getQuejosoCorreo(), "sin correo") + ").- Para su conocimiento.\n"
                        + "c.c.p. Expediente " + expediente.getFolio() + ".",
                pequena
        ));
    }

    private void filaDato(PdfPTable tabla, String etiqueta, String valor) {
        tabla.addCell(celda(new Phrase(etiqueta, negrita)));
        tabla.addCell(celda(new Phrase(valor(valor, "—"), normal)));
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
        Paragraph p = new Paragraph(texto, normal);
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

    private String valor(String texto, String porDefecto) {
        return texto == null || texto.isBlank() ? porDefecto : texto;
    }
}
