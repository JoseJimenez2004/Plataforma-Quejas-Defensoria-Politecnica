package ipn.escom.defensoria.notificaciones_service.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI notificacionesServiceOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Notificaciones Service — Defensoría de los Derechos Politécnicos")
                        .description("Envío de correos (activación de cuenta, códigos de recuperación, avisos) "
                                + "para la Plataforma de Quejas.")
                        .version("v1"));
    }
}
