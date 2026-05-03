package ipn.escom.defensoria.quejoso.config;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;

/**
 * Deserializador flexible para campos de fecha (JSON a LocalDateTime).
 * 
 * ¿Por qué se necesita?
 * Los input type="date" de HTML en Angular envían solo la fecha ("2026-04-14"),
 * pero el backend usa el objeto completo LocalDateTime. 
 * Este deserializador convierte la fecha sola a LocalDateTime fijando la hora al inicio del día (00:00:00).
 */
public class JsonDateDeserializer extends JsonDeserializer<LocalDateTime> {
    @Override
    public LocalDateTime deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        String text = p.getText().trim();
        try {
            // Intentar parsear como LocalDateTime completo (ej. 2026-04-14T10:30:00)
            return LocalDateTime.parse(text);
        } catch (DateTimeParseException e) {
            // Si falla porque solo viene la fecha, parsear como LocalDate y agregar hora 00:00:00
            return LocalDate.parse(text).atStartOfDay();
        }
    }
}
