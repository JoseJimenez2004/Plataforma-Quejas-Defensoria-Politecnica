package ipn.escom.defensoria.chatbot_service.entity;

import java.time.LocalDateTime;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

/**
 * Pregunta preseleccionada del mini-chat/tutorial que aparece en el portal del quejoso
 * (icono tipo mascota/robot en la página principal y en "Presentar una queja"). No es un
 * chatbot con IA: es un árbol plano de preguntas y respuestas fijas, redactadas a partir de
 * la normatividad de la Defensoría (Acuerdo de creación de la DDP, Manual de Procedimientos
 * e información pública del sitio), para no correr el riesgo de que se invente información
 * institucional incorrecta.
 *
 * "orden" es un orden GLOBAL (no reinicia por categoría): al sembrar los datos se agrupan en
 * bloques consecutivos por categoría para que, ordenando únicamente por "orden", las
 * preguntas ya salgan agrupadas en el orden de categoría deseado (ver
 * resources/seed/chatbot_seed.sql). El servicio arma la lista de categorías preservando el
 * primer orden en que aparecen.
 */
@Data
@Entity
@Table(name = "preguntas_chatbot")
public class PreguntaChatbot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 80)
    private String categoria;

    @Column(nullable = false, length = 300)
    private String pregunta;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String respuesta;

    @Column(name = "orden", nullable = false)
    private Integer orden = 0;

    @Column(nullable = false)
    private Boolean activo = true;

    @Column(name = "creado_en")
    private LocalDateTime creadoEn = LocalDateTime.now();

    @Column(name = "actualizado_en")
    private LocalDateTime actualizadoEn = LocalDateTime.now();
}
