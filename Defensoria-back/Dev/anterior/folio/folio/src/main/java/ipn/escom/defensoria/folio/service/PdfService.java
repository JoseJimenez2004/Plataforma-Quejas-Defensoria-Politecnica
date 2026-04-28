
package ipn.escom.defensoria.folio.service;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfWriter;
import ipn.escom.defensoria.folio.entity.FolioEntity;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.stereotype.Service;

@Service
public class PdfService {

   public void generarAcuse(FolioEntity folio, HttpServletResponse response) 
    throws IOException, com.lowagie.text.DocumentException { // <--- Agrega el DocumentException
        Document document = new Document(PageSize.A4);
        PdfWriter.getInstance(document, response.getOutputStream());

        document.open();
        
        // Estilos
        Font fuenteTitulo = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18);
        Font fuenteTexto = FontFactory.getFont(FontFactory.HELVETICA, 12);

        // Contenido
        Paragraph titulo = new Paragraph("ACUSE DE RECIBO - DEFENSORÍA POLITÉCNICA", fuenteTitulo);
        titulo.setAlignment(Element.ALIGN_CENTER);
        document.add(titulo);

        document.add(new Paragraph("\n"));
        document.add(new Paragraph("Folio: " + folio.getCodigoFolio(), fuenteTitulo));
        document.add(new Paragraph("Fecha de Registro: " + folio.getFechaCreacion().toString(), fuenteTexto));
        document.add(new Paragraph("\nDATOS DEL QUEJOSO:", fuenteTitulo));
        document.add(new Paragraph("Nombre: " + folio.getNombre() + " " + folio.getPrimerApellido(), fuenteTexto));
        document.add(new Paragraph("Boleta/Empleado: " + folio.getBoleta(), fuenteTexto));
        
        document.add(new Paragraph("\nDETALLES DE LA QUEJA:", fuenteTitulo));
        document.add(new Paragraph("Asunto: " + folio.getAsunto(), fuenteTexto));
        document.add(new Paragraph("Descripción: " + folio.getDescripcion(), fuenteTexto));

        document.add(new Paragraph("\n\nEste documento sirve como comprobante oficial de su trámite.", fuenteTexto));
        
        document.close();
    }
}
