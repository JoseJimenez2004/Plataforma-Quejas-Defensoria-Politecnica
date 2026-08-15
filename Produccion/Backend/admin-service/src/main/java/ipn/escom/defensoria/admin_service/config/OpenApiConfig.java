package ipn.escom.defensoria.admin_service.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI adminServiceOpenAPI() {
        final String esquemaJwt = "bearerAuth";
        return new OpenAPI()
                .info(new Info()
                        .title("Admin Service — Defensoría de los Derechos Politécnicos")
                        .description("Consola de administración: personal administrativo, roles, "
                                + "plantillas oficiales y respaldos.")
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
