package ipn.escom.defensoria.primercontacto.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;

/**
 * Documentación interactiva OpenAPI 3 (Swagger UI) de Primer Contacto, con el mismo formato
 * que el resto de los microservicios: /swagger-ui.html y /v3/api-docs.
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI primerContactoOpenAPI() {
        final String esquemaJwt = "bearerAuth";
        return new OpenAPI()
                .info(new Info()
                        .title("Primer Contacto — Defensoría de los Derechos Politécnicos")
                        .description("Bandeja de análisis, expedientes, citas, notas, dictámenes, "
                                + "conciliación y remisiones de Primer Contacto.")
                        .version("v1"))
                .addSecurityItem(new SecurityRequirement().addList(esquemaJwt))
                .components(new Components()
                        .addSecuritySchemes(esquemaJwt, new SecurityScheme()
                                .name(esquemaJwt)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")));
    }
}
