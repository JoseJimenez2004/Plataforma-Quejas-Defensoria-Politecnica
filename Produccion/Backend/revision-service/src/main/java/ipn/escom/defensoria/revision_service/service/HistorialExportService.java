package ipn.escom.defensoria.revision_service.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import ipn.escom.defensoria.revision_service.model.HistorialItemModel;

/** Genera el archivo Excel del botón "Exportar Reporte" del Historial de Trámites. */
@Service
public class HistorialExportService {

    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final String[] ENCABEZADOS = {
            "Folio", "Fecha Recibido", "Quejoso", "Tipo de Ingreso", "Estatus Final", "Motivo"
    };

    public byte[] exportar(List<HistorialItemModel> items) {
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Historial de Trámites");

            CellStyle estiloEncabezado = workbook.createCellStyle();
            Font fuenteEncabezado = workbook.createFont();
            fuenteEncabezado.setBold(true);
            estiloEncabezado.setFont(fuenteEncabezado);

            Row filaEncabezado = sheet.createRow(0);
            for (int i = 0; i < ENCABEZADOS.length; i++) {
                Cell celda = filaEncabezado.createCell(i);
                celda.setCellValue(ENCABEZADOS[i]);
                celda.setCellStyle(estiloEncabezado);
            }

            int filaIndice = 1;
            for (HistorialItemModel item : items) {
                Row fila = sheet.createRow(filaIndice++);
                fila.createCell(0).setCellValue(item.getNumeroFolio());
                fila.createCell(1).setCellValue(
                        item.getFechaCreacion() != null ? item.getFechaCreacion().format(FORMATO_FECHA) : "");
                fila.createCell(2).setCellValue(item.getNombreQuejoso());
                fila.createCell(3).setCellValue(item.getTipoIngreso());
                fila.createCell(4).setCellValue(item.getEstatusFinal());
                fila.createCell(5).setCellValue(item.getMotivoRechazo() != null ? item.getMotivoRechazo() : "");
            }

            for (int i = 0; i < ENCABEZADOS.length; i++) {
                sheet.autoSizeColumn(i);
            }

            ByteArrayOutputStream salida = new ByteArrayOutputStream();
            workbook.write(salida);
            return salida.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("No se pudo generar el archivo Excel: " + e.getMessage());
        }
    }
}
